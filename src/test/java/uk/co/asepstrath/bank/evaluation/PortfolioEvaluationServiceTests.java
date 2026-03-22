package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PortfolioEvaluationServiceTests {

    private Account account;
    private InvestmentService investmentService;
    private final Map<String, AsepLatestPrice> quotes = new HashMap<>();
    private final AsepPriceSource fakeLatest = ticker -> Optional.ofNullable(quotes.get(ticker));
    private final Map<String, AsepLatestPrice> priorByTicker = new HashMap<>();
    private final AsepPriceSource fakePriorSource = new AsepPriceSource() {
        @Override
        public Optional<AsepLatestPrice> latestPrice(String normalizedTicker) {
            return Optional.empty();
        }

        @Override
        public Optional<AsepLatestPrice> priorTradingDayEod(String normalizedTicker) {
            return Optional.ofNullable(priorByTicker.get(normalizedTicker));
        }
    };

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(100_000);
        investmentService = new InvestmentService(account);
        quotes.clear();
        priorByTicker.clear();
    }

    @Test
    public void bookOverviewUsesPositionAverageBookPriceWithoutRecalculation() {
        investmentService.buy("AAPL", 10, 150.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildBookPriceOverview(investmentService);
        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertEquals("AAPL", line.ticker);
        assertEquals(10, line.shares);
        assertEquals(150, line.averageBookPricePerShare);
        assertEquals(1500, line.totalInvested);
        assertEquals(1500L, line.bookValue);
        assertTrue(line.bookValueGbp.contains("1,500") || line.bookValueGbp.contains("1500"));
        assertTrue(line.averageBookPricePerShareGbp.contains("150") || line.averageBookPricePerShareGbp.contains("1,50"));
        assertEquals(1500L, resp.summary.totalBookValue);
    }

    @Test
    public void bookOverviewReflectsAverageBookPriceAfterPartialSell() {
        investmentService.buy("MSFT", 10, 300.0);
        investmentService.sell("MSFT", 4, 320.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildBookPriceOverview(investmentService);
        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertEquals(6, line.shares);
        assertEquals(300, line.averageBookPricePerShare);
        assertEquals(1800, line.totalInvested);
    }

    @Test
    public void bookOverviewSortsBySectorThenTicker() {
        EquitiesService equities = new EquitiesService();
        EquityRecord a = new EquityRecord();
        a.ticker = "ZZZ";
        a.name = "Zed";
        a.sector = "Technology";
        equities.addEquity(a);
        EquityRecord b = new EquityRecord();
        b.ticker = "AAA";
        b.name = "Triple A";
        b.sector = "Technology";
        equities.addEquity(b);
        EquityRecord c = new EquityRecord();
        c.ticker = "JPM";
        c.name = "JPM";
        c.sector = "Financials";
        equities.addEquity(c);
        investmentService.buy("ZZZ", 1, 10.0);
        investmentService.buy("AAA", 1, 20.0);
        investmentService.buy("JPM", 1, 30.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(equities, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildBookPriceOverview(investmentService);
        assertEquals("JPM", resp.positions.get(0).ticker);
        assertEquals("AAA", resp.positions.get(1).ticker);
        assertEquals("ZZZ", resp.positions.get(2).ticker);
    }

    @Test
    public void bookOverviewIncludesAsepReferenceWhenAvailable() {
        investmentService.buy("GOOGL", 2, 2800.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 2900.0;
        px.date = "2025-03-01";
        quotes.put("GOOGL", px);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildBookPriceOverview(investmentService);
        EvaluationPositionLine line = resp.positions.get(0);
        assertTrue(line.referenceMarketPriceAvailable);
        assertEquals(2900.0, line.referenceMarketPricePerShare, 0.01);
        assertTrue(line.glanceLine.contains("ASEP ref"));
        assertTrue(resp.summary.notices.isEmpty());
    }

    @Test
    public void bookOverviewAddsNoticeWhenAsepMissing() {
        investmentService.buy("XOM", 1, 50.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildBookPriceOverview(investmentService);
        assertFalse(resp.positions.get(0).referenceMarketPriceAvailable);
        assertEquals(1, resp.summary.notices.size());
        assertTrue(resp.summary.notices.get(0).contains("XOM"));
    }

    @Test
    public void marketOverviewComputesMarketValueAsSharesTimesLatestAsepPrice() {
        investmentService.buy("AAPL", 10, 150.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 160.0;
        px.date = "2025-03-01";
        quotes.put("AAPL", px);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildMarketValueOverview(investmentService);
        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertTrue(line.marketPriceAvailable);
        assertEquals(1600.0, line.currentMarketValue, 0.01);
        assertEquals(160.0, line.latestClosePricePerShare, 0.01);
        assertEquals(1600.0, resp.summary.totalMarketValue, 0.01);
        assertEquals(1, resp.summary.positionsWithMarketPriceCount);
    }

    @Test
    public void marketOverviewExcludesMissingPricesFromTotalAndAddsNotice() {
        investmentService.buy("AAPL", 5, 100.0);
        investmentService.buy("MSFT", 2, 200.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 110.0;
        px.date = "2025-03-02";
        quotes.put("AAPL", px);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildMarketValueOverview(investmentService);
        assertEquals(550.0, resp.summary.totalMarketValue, 0.01);
        assertEquals(1, resp.summary.positionsWithoutMarketPriceCount);
        assertTrue(resp.summary.notices.stream().anyMatch(n -> n.contains("MSFT")));
    }

    @Test
    public void marketOverviewSortsPricedPositionsByMarketValueDescending() {
        investmentService.buy("LOW", 1, 1.0);
        investmentService.buy("HIGH", 1, 1.0);
        AsepLatestPrice low = new AsepLatestPrice();
        low.pricePerShare = 10.0;
        low.date = "2025-01-01";
        AsepLatestPrice high = new AsepLatestPrice();
        high.pricePerShare = 999.0;
        high.date = "2025-01-02";
        quotes.put("LOW", low);
        quotes.put("HIGH", high);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakeLatest);
        EvaluationOverviewResponse resp = svc.buildMarketValueOverview(investmentService);
        assertEquals("HIGH", resp.positions.get(0).ticker);
        assertEquals("LOW", resp.positions.get(1).ticker);
    }

    @Test
    public void priorDayOverviewShowsPriorEodFromAsepWhenTwoOrMoreDatesExistInFeed() {
        investmentService.buy("AAPL", 5, 150.0);
        AsepLatestPrice prior = new AsepLatestPrice();
        prior.pricePerShare = 148.25;
        prior.date = "2025-02-04";
        priorByTicker.put("AAPL", prior);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakePriorSource);
        EvaluationOverviewResponse resp = svc.buildPriorDayEodOverview(investmentService);
        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertTrue(line.priorEodAvailable);
        assertEquals(148.25, line.priorEodPricePerShare, 0.001);
        assertEquals("2025-02-04", line.priorSessionDate);
        assertEquals(1, resp.summary.positionsWithPriorEodCount);
        assertEquals(0, resp.summary.positionsWithoutPriorEodCount);
        assertTrue(line.glanceLine.contains("prior session EOD"));
    }

    @Test
    public void priorDayOverviewMarksUnavailableWhenAsepHasInsufficientHistory() {
        investmentService.buy("MSFT", 2, 300.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakePriorSource);
        EvaluationOverviewResponse resp = svc.buildPriorDayEodOverview(investmentService);
        assertFalse(resp.positions.get(0).priorEodAvailable);
        assertEquals(1, resp.summary.notices.size());
        assertTrue(resp.summary.notices.get(0).contains("MSFT"));
    }

    @Test
    public void priorDayOverviewSortsBySectorThenTicker() {
        EquitiesService equities = new EquitiesService();
        EquityRecord z = new EquityRecord();
        z.ticker = "ZZZ";
        z.name = "Z";
        z.sector = "Technology";
        equities.addEquity(z);
        EquityRecord a = new EquityRecord();
        a.ticker = "AAA";
        a.name = "A";
        a.sector = "Technology";
        equities.addEquity(a);
        EquityRecord j = new EquityRecord();
        j.ticker = "JPM";
        j.name = "J";
        j.sector = "Financials";
        equities.addEquity(j);
        investmentService.buy("ZZZ", 1, 10.0);
        investmentService.buy("AAA", 1, 20.0);
        investmentService.buy("JPM", 1, 30.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 1.0;
        px.date = "2025-01-01";
        priorByTicker.put("ZZZ", px);
        priorByTicker.put("AAA", px);
        priorByTicker.put("JPM", px);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(equities, fakePriorSource);
        EvaluationOverviewResponse resp = svc.buildPriorDayEodOverview(investmentService);
        assertEquals("JPM", resp.positions.get(0).ticker);
        assertEquals("AAA", resp.positions.get(1).ticker);
        assertEquals("ZZZ", resp.positions.get(2).ticker);
    }

    @Test
    public void priorEodValueOverviewValuesPositionAsSharesTimesPriorEodFromAsep() {
        investmentService.buy("AAPL", 4, 150.0);
        AsepLatestPrice prior = new AsepLatestPrice();
        prior.pricePerShare = 148.5;
        prior.date = "2025-02-04";
        priorByTicker.put("AAPL", prior);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakePriorSource);
        EvaluationOverviewResponse resp = svc.buildPriorEodValueOverview(investmentService);
        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertTrue(line.priorEodPriceAvailable);
        assertEquals(594.0, line.valueAtPriorClose, 0.01);
        assertEquals(594.0, resp.summary.totalValueAtPriorClose, 0.01);
        assertEquals(1, resp.summary.positionsWithPriceCount);
    }

    @Test
    public void priorEodValueOverviewExcludesMissingPriorEodFromTotal() {
        investmentService.buy("MSFT", 2, 100.0);
        PortfolioEvaluationService svc = new PortfolioEvaluationService(null, fakePriorSource);
        EvaluationOverviewResponse resp = svc.buildPriorEodValueOverview(investmentService);
        assertFalse(resp.positions.get(0).priorEodPriceAvailable);
        assertEquals(0.0, resp.summary.totalValueAtPriorClose, 0.01);
        assertEquals(1, resp.summary.notices.size());
    }

    @Test
    public void unrealizedOverviewMatchesSnapshotUnrealizedFigures() {
        investmentService.buy("AAPL", 10, 150.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 160.0;
        px.date = "2025-03-01";
        quotes.put("AAPL", px);
        PortfolioEvaluationService eval = new PortfolioEvaluationService(null, fakeLatest);
        PortfolioSnapshotResponse snap = eval.buildSnapshot(investmentService);
        EvaluationOverviewResponse pnl = eval.buildUnrealizedPnLOverview(investmentService);
        assertEquals(snap.positions.size(), pnl.positions.size());
        assertEquals(snap.positions.get(0).unrealizedPnL, pnl.positions.get(0).unrealizedPnL);
        assertEquals(snap.positions.get(0).unrealizedPnLPercent, pnl.positions.get(0).unrealizedPnLPercent);
        assertEquals(snap.summary.totalUnrealizedPnL, pnl.summary.totalUnrealizedPnL);
        assertEquals(snap.summary.totalUnrealizedPnLPercent, pnl.summary.totalUnrealizedPnLPercent);
        assertEquals(snap.summary.cashBalance, pnl.summary.cashBalance);
        assertEquals(snap.summary.totalPortfolioValue, pnl.summary.totalPortfolioValue, 0.01);
    }

    @Test
    public void eachPositionExposesAbsoluteAndPercentageGainsWhenAsepPricePresent() {
        investmentService.buy("GOOGL", 2, 2800.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 2900.0;
        px.date = "2025-04-01";
        quotes.put("GOOGL", px);
        EvaluationPositionLine line = new PortfolioEvaluationService(null, fakeLatest)
                .buildUnrealizedPnLOverview(investmentService).positions.get(0);
        assertTrue(line.marketPriceAvailable);
        assertNotNull(line.unrealizedPnL);
        assertNotNull(line.unrealizedPnLGbp);
        assertNotNull(line.unrealizedPnLPercent);
        assertNotNull(line.unrealizedPnLPercentDisplay);
        assertTrue(line.unrealizedPnLPercentDisplay.endsWith("%"));
    }

    @Test
    public void unrealizedMatchesSpecFormulasPerPosition() {
        investmentService.buy("MSFT", 8, 250.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 260.0;
        px.date = "2025-06-01";
        quotes.put("MSFT", px);
        EvaluationPositionLine line = new PortfolioEvaluationService(null, fakeLatest)
                .buildUnrealizedPnLOverview(investmentService).positions.get(0);
        long bookValue = (long) line.shares * line.bookPricePerShare;
        assertEquals(bookValue, line.bookValue);
        double current = line.shares * line.marketPricePerShare;
        assertEquals(current, line.currentMarketValue, 0.01);
        assertEquals(current - bookValue, line.unrealizedPnL, 0.01);
        assertNotNull(line.unrealizedPnLPercent);
        assertEquals(100.0 * (current - bookValue) / bookValue, line.unrealizedPnLPercent, 0.01);
        assertNotNull(line.unrealizedPnLGbp);
        assertNotNull(line.unrealizedPnLPercentDisplay);
    }

    @Test
    public void sectorGroupsAggregateUnrealizedPnL() {
        investmentService.buy("A", 1, 100.0);
        investmentService.buy("B", 1, 100.0);
        AsepLatestPrice up = new AsepLatestPrice();
        up.pricePerShare = 110.0;
        up.date = "2025-01-01";
        quotes.put("A", up);
        quotes.put("B", up);
        EquitiesService eq = new EquitiesService();
        EquityRecord a = new EquityRecord();
        a.ticker = "A";
        a.name = "A";
        a.sector = "Tech";
        eq.addEquity(a);
        EquityRecord b = new EquityRecord();
        b.ticker = "B";
        b.name = "B";
        b.sector = "Tech";
        eq.addEquity(b);
        EvaluationOverviewResponse pnl = new PortfolioEvaluationService(eq, fakeLatest)
                .buildUnrealizedPnLOverview(investmentService);
        EvaluationSectorGroup tech = pnl.bySector.stream()
                .filter(g -> "Tech".equals(g.sectorKey))
                .findFirst()
                .orElseThrow();
        assertEquals(20.0, tech.sectorUnrealizedPnL, 0.01);
    }

    @Test
    public void aggregatePerformanceSummaryMatchesSnapshotSummary() {
        investmentService.buy("AAPL", 10, 150.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 160.0;
        px.date = "2025-03-01";
        quotes.put("AAPL", px);
        PortfolioEvaluationService eval = new PortfolioEvaluationService(null, fakeLatest);
        PortfolioSnapshotResponse snap = eval.buildSnapshot(investmentService);
        PortfolioPerformanceOverviewResponse perf = eval.buildPortfolioPerformanceOverview(investmentService);
        assertEquals(snap.summary.positionsMarketValue, perf.summary.positionsMarketValue, 0.01);
        assertEquals(snap.summary.totalBookValueAllPositions, perf.summary.totalBookValueAllPositions);
        assertEquals(snap.summary.totalUnrealizedPnL, perf.summary.totalUnrealizedPnL, 0.01);
        assertEquals(snap.summary.totalPortfolioValue, perf.summary.totalPortfolioValue, 0.01);
        assertEquals(1600.0, perf.summary.positionsMarketValue, 0.01);
        assertEquals(1500L, perf.summary.totalBookValueAllPositions);
        assertNotNull(perf.summary.totalUnrealizedPnL);
        assertEquals(100.0, perf.summary.totalUnrealizedPnL, 0.01);
        assertNotNull(perf.summary.totalUnrealizedPnLPercent);
        assertEquals(100_000 - 1500 + 1600.0, perf.summary.totalPortfolioValue, 0.01);
        assertTrue(perf.summary.priceSourceNote.contains("ASEP"));
    }
}

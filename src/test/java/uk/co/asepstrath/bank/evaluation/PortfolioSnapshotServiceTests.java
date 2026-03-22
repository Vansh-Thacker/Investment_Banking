package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PortfolioSnapshotServiceTests {

    private Account account;
    private InvestmentService investmentService;
    private final Map<String, AsepLatestPrice> quotes = new HashMap<>();

    private final AsepPriceSource fakePrices = ticker -> Optional.ofNullable(quotes.get(ticker));

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(100_000);
        investmentService = new InvestmentService(account);
        quotes.clear();
    }

    @Test
    public void snapshotComputesCurrentValueAndUnrealizedPnLFromSpecFormulas() {
        investmentService.buy("AAPL", 10, 150.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 160.0;
        px.date = "2025-03-01";
        quotes.put("AAPL", px);

        PortfolioSnapshotService svc = new PortfolioSnapshotService(investmentService, null, fakePrices);
        PortfolioSnapshotResponse resp = svc.buildSnapshot();

        assertEquals(1, resp.positions.size());
        EvaluationPositionLine line = resp.positions.get(0);
        assertTrue(line.marketPriceAvailable);
        assertEquals(1600.0, line.currentMarketValue, 0.01);
        assertEquals(1500L, line.bookValue);
        assertEquals(100.0, line.unrealizedPnL, 0.01);
        assertNotNull(line.unrealizedPnLPercent);
        assertEquals(100.0 * 100 / 1500, line.unrealizedPnLPercent, 0.01);

        assertEquals(1600.0, resp.summary.positionsMarketValue, 0.01);
        assertEquals(100_000 - 1500 + 1600.0, resp.summary.totalPortfolioValue, 0.01);
        assertEquals(100.0, resp.summary.totalUnrealizedPnL, 0.01);
    }

    @Test
    public void snapshotExcludesMissingPricesFromMarketTotalAndAddsNotice() {
        investmentService.buy("AAPL", 5, 100.0);
        investmentService.buy("MSFT", 2, 200.0);
        AsepLatestPrice px = new AsepLatestPrice();
        px.pricePerShare = 110.0;
        px.date = "2025-03-02";
        quotes.put("AAPL", px);
        // MSFT: no quote

        PortfolioSnapshotService svc = new PortfolioSnapshotService(investmentService, null, fakePrices);
        PortfolioSnapshotResponse resp = svc.buildSnapshot();

        assertEquals(550.0, resp.summary.positionsMarketValue, 0.01);
        assertEquals(1, resp.summary.positionsWithoutMarketPriceCount);
        assertTrue(resp.summary.notices.stream().anyMatch(n -> n.contains("MSFT")));
        assertNotNull(resp.summary.totalUnrealizedPnL);
        assertEquals(50.0, resp.summary.totalUnrealizedPnL, 0.01);
        assertNotNull(resp.summary.totalUnrealizedPnLPercent);
        assertEquals(10.0, resp.summary.totalUnrealizedPnLPercent, 0.01);
    }

    @Test
    public void snapshotSortsPricedPositionsByMarketValueDescendingThenTicker() {
        investmentService.buy("ZZZ", 1, 10.0);
        investmentService.buy("AAA", 1, 10.0);
        AsepLatestPrice low = new AsepLatestPrice();
        low.pricePerShare = 20.0;
        low.date = "2025-01-01";
        AsepLatestPrice high = new AsepLatestPrice();
        high.pricePerShare = 100.0;
        high.date = "2025-01-02";
        quotes.put("AAA", high);
        quotes.put("ZZZ", low);

        PortfolioSnapshotService svc = new PortfolioSnapshotService(investmentService, null, fakePrices);
        PortfolioSnapshotResponse resp = svc.buildSnapshot();

        assertEquals("AAA", resp.positions.get(0).ticker);
        assertEquals("ZZZ", resp.positions.get(1).ticker);
    }

    @Test
    public void snapshotGroupsBySectorUsingEquitiesService() {
        EquitiesService equities = new EquitiesService();
        EquityRecord tech = new EquityRecord();
        tech.ticker = "AAPL";
        tech.name = "Apple";
        tech.sector = "Technology";
        equities.addEquity(tech);
        EquityRecord fin = new EquityRecord();
        fin.ticker = "JPM";
        fin.name = "JPMorgan";
        fin.sector = "Financials";
        equities.addEquity(fin);

        investmentService.buy("AAPL", 1, 100.0);
        investmentService.buy("JPM", 1, 50.0);
        AsepLatestPrice a = new AsepLatestPrice();
        a.pricePerShare = 110.0;
        a.date = "2025-01-01";
        AsepLatestPrice j = new AsepLatestPrice();
        j.pricePerShare = 55.0;
        j.date = "2025-01-02";
        quotes.put("AAPL", a);
        quotes.put("JPM", j);

        PortfolioSnapshotService svc = new PortfolioSnapshotService(investmentService, equities, fakePrices);
        PortfolioSnapshotResponse resp = svc.buildSnapshot();

        assertEquals(2, resp.bySector.size());
        EvaluationSectorGroup financials = resp.bySector.stream().filter(g -> "Financials".equals(g.sectorKey)).findFirst().orElseThrow();
        EvaluationSectorGroup technology = resp.bySector.stream().filter(g -> "Technology".equals(g.sectorKey)).findFirst().orElseThrow();
        assertEquals(55.0, financials.sectorMarketValue, 0.01);
        assertEquals(110.0, technology.sectorMarketValue, 0.01);
    }

    @Test
    public void snapshotWithNoPositionsShowsCashOnly() {
        PortfolioSnapshotService svc = new PortfolioSnapshotService(investmentService, null, fakePrices);
        PortfolioSnapshotResponse resp = svc.buildSnapshot();

        assertTrue(resp.positions.isEmpty());
        assertEquals(100_000, resp.summary.cashBalance);
        assertEquals(0.0, resp.summary.positionsMarketValue, 0.01);
        assertEquals(100_000.0, resp.summary.totalPortfolioValue, 0.01);
    }
}

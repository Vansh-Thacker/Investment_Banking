package uk.co.asepstrath.bank.evaluation;

import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.investments.Position;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

public class PortfolioEvaluationService {

    private static final String SECTOR_UNKNOWN = "Unknown";

    private final InvestmentService investmentService;
    private final EquitiesService equitiesService;
    private final AsepPriceSource priceSource;
    private final PortfolioSnapshotService snapshots;

    public PortfolioEvaluationService(InvestmentService investmentService,
                                      EquitiesService equitiesService,
                                      AsepPriceSource priceSource) {
        this.investmentService = investmentService;
        this.equitiesService = equitiesService;
        this.priceSource = priceSource;
        this.snapshots = new PortfolioSnapshotService(investmentService, equitiesService, priceSource);
    }

    public PortfolioSnapshotResponse buildSnapshot() {
        return snapshots.buildSnapshot();
    }

    public EvaluationOverviewResponse buildBookPriceOverview() {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        List<EvaluationPositionLine> lines = new ArrayList<>();
        EvaluationSummary summary = new EvaluationSummary();
        summary.sourceNote = "Book figures from Investment Management; optional ASEP latest close as reference (GET /api/prices/{symbol}).";

        for (Position p : investmentService.getPositions()) {
            EvaluationPositionLine line = new EvaluationPositionLine();
            line.ticker = p.ticker;
            line.shares = p.shares;
            line.averageBookPricePerShare = p.averageBookPrice;
            line.averageBookPricePerShareGbp = gbp.format(p.averageBookPrice);
            line.bookPricePerShare = p.averageBookPrice;
            line.bookPricePerShareGbp = line.averageBookPricePerShareGbp;
            line.totalInvested = p.totalInvested;
            line.totalInvestedGbp = gbp.format(p.totalInvested);
            line.bookValue = (long) p.shares * p.averageBookPrice;
            line.bookValueGbp = gbp.format(line.bookValue);
            resolveIdentity(p, line);

            Optional<AsepLatestPrice> ref = priceSource.latestPrice(p.ticker);
            if (ref.isPresent()) {
                AsepLatestPrice px = ref.get();
                line.referenceMarketPriceAvailable = true;
                line.referenceMarketPricePerShare = px.pricePerShare;
                line.referenceMarketPricePerShareGbp = gbp.format(px.pricePerShare);
                line.referencePriceAsOfDate = px.date;
                line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · book %s · ASEP ref %s (%s)",
                        line.ticker, line.displayName, line.shares, line.bookValueGbp,
                        line.referenceMarketPricePerShareGbp, line.referencePriceAsOfDate);
            } else {
                line.referenceMarketPriceAvailable = false;
                summary.notices.add("No ASEP reference price for " + p.ticker + ".");
                line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · book %s · no ASEP reference",
                        line.ticker, line.displayName, line.shares, line.bookValueGbp);
            }
            lines.add(line);
        }

        lines.sort(Comparator.comparing((EvaluationPositionLine l) -> l.sector).thenComparing(l -> l.ticker));

        long totalBook = 0;
        for (EvaluationPositionLine l : lines) {
            totalBook += l.bookValue;
        }
        summary.positionCount = lines.size();
        summary.totalBookValue = totalBook;
        summary.totalBookValueGbp = gbp.format(totalBook);

        EvaluationOverviewResponse resp = new EvaluationOverviewResponse();
        resp.summary = summary;
        resp.positions = lines;
        resp.bySector = buildBookSectorGroups(lines, gbp);
        return resp;
    }

    public EvaluationOverviewResponse buildMarketValueOverview() {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        List<EvaluationPositionLine> lines = new ArrayList<>();
        EvaluationSummary summary = new EvaluationSummary();
        summary.sourceNote = "Current value = shares × latest ASEP close (most recent date in feed per ticker).";

        for (Position p : investmentService.getPositions()) {
            EvaluationPositionLine line = new EvaluationPositionLine();
            line.ticker = p.ticker;
            line.shares = p.shares;
            resolveIdentity(p, line);

            Optional<AsepLatestPrice> quote = priceSource.latestPrice(p.ticker);
            if (quote.isPresent()) {
                AsepLatestPrice px = quote.get();
                line.marketPriceAvailable = true;
                line.marketPricePerShare = px.pricePerShare;
                line.marketPricePerShareGbp = gbp.format(px.pricePerShare);
                line.marketPriceAsOfDate = px.date;
                line.latestClosePricePerShare = px.pricePerShare;
                line.latestClosePricePerShareGbp = line.marketPricePerShareGbp;
                line.priceAsOfDate = px.date;
                line.currentMarketValue = p.shares * px.pricePerShare;
                line.currentMarketValueGbp = gbp.format(line.currentMarketValue);
                line.statusLabel = "OK";
                line.glanceLine = String.format(Locale.UK, "%s · %d sh @ %s = %s",
                        line.ticker, line.shares, line.latestClosePricePerShareGbp, line.currentMarketValueGbp);
            } else {
                line.marketPriceAvailable = false;
                line.statusLabel = "Price unavailable";
                summary.notices.add("Price unavailable for " + p.ticker + " — excluded from market total.");
                line.glanceLine = String.format(Locale.UK, "%s · %d sh · price unavailable", line.ticker, line.shares);
            }
            lines.add(line);
        }

        lines.sort(Comparator
                .comparing((EvaluationPositionLine l) -> !l.marketPriceAvailable)
                .thenComparing((EvaluationPositionLine l) -> -(l.currentMarketValue != null ? l.currentMarketValue : 0.0))
                .thenComparing(l -> l.ticker));

        double totalMkt = 0;
        int priced = 0;
        int unpriced = 0;
        for (EvaluationPositionLine l : lines) {
            if (l.marketPriceAvailable && l.currentMarketValue != null) {
                totalMkt += l.currentMarketValue;
                priced++;
            } else {
                unpriced++;
            }
        }
        summary.positionCount = lines.size();
        summary.totalMarketValue = totalMkt;
        summary.totalMarketValueGbp = gbp.format(totalMkt);
        summary.positionsWithMarketPriceCount = priced;
        summary.positionsWithoutMarketPriceCount = unpriced;

        EvaluationOverviewResponse resp = new EvaluationOverviewResponse();
        resp.summary = summary;
        resp.positions = lines;
        resp.bySector = buildMarketSectorGroups(lines, gbp);
        return resp;
    }

    public EvaluationOverviewResponse buildPriorDayEodOverview() {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        List<EvaluationPositionLine> lines = new ArrayList<>();
        EvaluationSummary summary = new EvaluationSummary();
        summary.sourceNote = "Prior session EOD = second-to-last dated close in ASEP price history per ticker.";

        for (Position p : investmentService.getPositions()) {
            EvaluationPositionLine line = new EvaluationPositionLine();
            line.ticker = p.ticker;
            line.shares = p.shares;
            resolveIdentity(p, line);

            Optional<AsepLatestPrice> prior = priceSource.priorTradingDayEod(p.ticker);
            if (prior.isPresent()) {
                AsepLatestPrice px = prior.get();
                line.priorEodAvailable = true;
                line.priorEodPricePerShare = px.pricePerShare;
                line.priorEodPricePerShareGbp = gbp.format(px.pricePerShare);
                line.priorSessionDate = px.date;
                line.statusLabel = "OK";
                line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · prior session EOD %s (%s)",
                        line.ticker, line.displayName, line.shares, line.priorEodPricePerShareGbp, line.priorSessionDate);
            } else {
                line.priorEodAvailable = false;
                line.statusLabel = "Prior EOD unavailable";
                summary.notices.add("Prior session EOD unavailable for " + p.ticker + " (insufficient ASEP history).");
                line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · prior EOD unavailable",
                        line.ticker, line.displayName, line.shares);
            }
            lines.add(line);
        }

        lines.sort(Comparator.comparing((EvaluationPositionLine l) -> l.sector).thenComparing(l -> l.ticker));

        int withPrior = 0;
        int without = 0;
        for (EvaluationPositionLine l : lines) {
            if (l.priorEodAvailable) {
                withPrior++;
            } else {
                without++;
            }
        }
        summary.positionCount = lines.size();
        summary.positionsWithPriorEodCount = withPrior;
        summary.positionsWithoutPriorEodCount = without;

        EvaluationOverviewResponse resp = new EvaluationOverviewResponse();
        resp.summary = summary;
        resp.positions = lines;
        resp.bySector = buildPriorDaySectorGroups(lines);
        return resp;
    }

    public EvaluationOverviewResponse buildPriorEodValueOverview() {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        List<EvaluationPositionLine> lines = new ArrayList<>();
        EvaluationSummary summary = new EvaluationSummary();
        summary.sourceNote = "Value at prior close = shares × prior session EOD from ASEP.";

        for (Position p : investmentService.getPositions()) {
            EvaluationPositionLine line = new EvaluationPositionLine();
            line.ticker = p.ticker;
            line.shares = p.shares;
            resolveIdentity(p, line);

            Optional<AsepLatestPrice> prior = priceSource.priorTradingDayEod(p.ticker);
            if (prior.isPresent()) {
                AsepLatestPrice px = prior.get();
                line.priorEodPriceAvailable = true;
                line.priorEodClosePerShare = px.pricePerShare;
                line.priorEodClosePerShareGbp = gbp.format(px.pricePerShare);
                line.priorEodAvailable = true;
                line.priorEodPricePerShare = px.pricePerShare;
                line.priorEodPricePerShareGbp = line.priorEodClosePerShareGbp;
                line.priorSessionDate = px.date;
                line.valueAtPriorClose = p.shares * px.pricePerShare;
                line.valueAtPriorCloseGbp = gbp.format(line.valueAtPriorClose);
                line.statusLabel = "OK";
                line.glanceLine = String.format(Locale.UK, "%s · %d sh × %s = %s",
                        line.ticker, line.shares, line.priorEodClosePerShareGbp, line.valueAtPriorCloseGbp);
            } else {
                line.priorEodPriceAvailable = false;
                line.priorEodAvailable = false;
                line.statusLabel = "Prior EOD unavailable";
                summary.notices.add("Prior EOD unavailable for " + p.ticker + " — excluded from total value at prior close.");
                line.glanceLine = String.format(Locale.UK, "%s · %d sh · prior EOD unavailable", line.ticker, line.shares);
            }
            lines.add(line);
        }

        lines.sort(Comparator.comparing((EvaluationPositionLine l) -> l.sector).thenComparing(l -> l.ticker));

        double totalVal = 0;
        int withP = 0;
        int without = 0;
        for (EvaluationPositionLine l : lines) {
            if (l.priorEodPriceAvailable && l.valueAtPriorClose != null) {
                totalVal += l.valueAtPriorClose;
                withP++;
            } else {
                without++;
            }
        }
        summary.positionCount = lines.size();
        summary.totalValueAtPriorClose = totalVal;
        summary.totalValueAtPriorCloseGbp = gbp.format(totalVal);
        summary.positionsWithPriceCount = withP;
        summary.positionsWithoutPriceCount = without;

        EvaluationOverviewResponse resp = new EvaluationOverviewResponse();
        resp.summary = summary;
        resp.positions = lines;
        resp.bySector = buildPriorEodValueSectorGroups(lines, gbp);
        return resp;
    }

    public EvaluationOverviewResponse buildUnrealizedPnLOverview() {
        PortfolioSnapshotResponse snap = snapshots.buildSnapshot();
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);

        EvaluationOverviewResponse out = new EvaluationOverviewResponse();
        out.summary = mapUnrealizedSummary(snap);
        out.positions = new ArrayList<>(snap.positions);
        out.bySector = buildUnrealizedSectorGroups(snap.positions, gbp);
        return out;
    }

    public PortfolioPerformanceOverviewResponse buildPortfolioPerformanceOverview() {
        PortfolioSnapshotResponse snap = snapshots.buildSnapshot();
        PortfolioPerformanceOverviewResponse out = new PortfolioPerformanceOverviewResponse();
        out.summary = snap.summary;
        return out;
    }

    private static EvaluationSummary mapUnrealizedSummary(PortfolioSnapshotResponse snap) {
        EvaluationSummary s = snap.summary;
        EvaluationSummary t = new EvaluationSummary();
        t.sourceNote = "Book value = shares × book price; current value = shares × ASEP latest close; "
                + "unrealized £ = current − book; % = 100 × unrealized ÷ book (per position when book > 0). "
                + "Portfolio totals match /evaluation/portfolio/snapshot. Cash included below for deposit/withdraw context.";
        t.positionCount = s.positionCount;
        t.positionsWithMarketPriceCount = s.positionsWithMarketPriceCount;
        t.positionsWithoutMarketPriceCount = s.positionsWithoutMarketPriceCount;
        t.positionsMarketValue = s.positionsMarketValue;
        t.positionsMarketValueGbp = s.positionsMarketValueGbp;
        t.totalBookValueAllPositions = s.totalBookValueAllPositions;
        t.totalBookValueAllPositionsGbp = s.totalBookValueAllPositionsGbp;
        t.totalUnrealizedPnL = s.totalUnrealizedPnL;
        t.totalUnrealizedPnLGbp = s.totalUnrealizedPnLGbp;
        t.totalUnrealizedPnLPercent = s.totalUnrealizedPnLPercent;
        t.totalUnrealizedPnLPercentDisplay = s.totalUnrealizedPnLPercentDisplay;
        t.cashBalance = s.cashBalance;
        t.cashBalanceGbp = s.cashBalanceGbp;
        t.totalPortfolioValue = s.totalPortfolioValue;
        t.totalPortfolioValueGbp = s.totalPortfolioValueGbp;
        t.priceSourceNote = s.priceSourceNote;
        t.notices.addAll(s.notices);
        return t;
    }

    private static List<EvaluationSectorGroup> buildUnrealizedSectorGroups(
            List<EvaluationPositionLine> lines, NumberFormat gbp) {
        TreeMap<String, EvaluationSectorGroup> map = new TreeMap<>();
        for (EvaluationPositionLine l : lines) {
            String key = l.sector != null ? l.sector : SECTOR_UNKNOWN;
            EvaluationSectorGroup g = map.computeIfAbsent(key, k -> {
                EvaluationSectorGroup ng = new EvaluationSectorGroup();
                ng.sectorKey = k;
                ng.sectorLabel = k;
                return ng;
            });
            if (!g.tickers.contains(l.ticker)) {
                g.tickers.add(l.ticker);
            }
            g.sectorBookValue += l.bookValue;
            if (l.marketPriceAvailable && l.currentMarketValue != null) {
                g.sectorMarketValue += l.currentMarketValue;
                g.sectorUnrealizedPnL += l.unrealizedPnL != null ? l.unrealizedPnL : 0;
            }
        }
        List<EvaluationSectorGroup> out = new ArrayList<>();
        for (EvaluationSectorGroup g : map.values()) {
            g.sectorMarketValueGbp = gbp.format(g.sectorMarketValue);
            g.sectorBookValueGbp = gbp.format(g.sectorBookValue);
            g.sectorUnrealizedPnLGbp = gbp.format(g.sectorUnrealizedPnL);
            g.tickers.sort(String::compareTo);
            out.add(g);
        }
        return out;
    }

    private void resolveIdentity(Position p, EvaluationPositionLine line) {
        EquityRecord eq = equitiesService != null ? equitiesService.getEquity(p.ticker) : null;
        line.displayName = eq != null && eq.name != null && !eq.name.isBlank() ? eq.name : p.ticker;
        line.sector = eq != null && eq.sector != null && !eq.sector.isBlank() ? eq.sector.trim() : SECTOR_UNKNOWN;
    }

    private static List<EvaluationSectorGroup> buildBookSectorGroups(List<EvaluationPositionLine> lines, NumberFormat gbp) {
        TreeMap<String, EvaluationSectorGroup> map = new TreeMap<>();
        for (EvaluationPositionLine l : lines) {
            String key = l.sector != null ? l.sector : SECTOR_UNKNOWN;
            EvaluationSectorGroup g = map.computeIfAbsent(key, k -> {
                EvaluationSectorGroup ng = new EvaluationSectorGroup();
                ng.sectorKey = k;
                ng.sectorLabel = k;
                return ng;
            });
            if (!g.tickers.contains(l.ticker)) {
                g.tickers.add(l.ticker);
            }
            g.sectorBookValue += l.bookValue;
        }
        List<EvaluationSectorGroup> out = new ArrayList<>();
        for (EvaluationSectorGroup g : map.values()) {
            g.sectorBookValueGbp = gbp.format(g.sectorBookValue);
            g.tickers.sort(String::compareTo);
            out.add(g);
        }
        return out;
    }

    private static List<EvaluationSectorGroup> buildMarketSectorGroups(List<EvaluationPositionLine> lines, NumberFormat gbp) {
        TreeMap<String, EvaluationSectorGroup> map = new TreeMap<>();
        for (EvaluationPositionLine l : lines) {
            String key = l.sector != null ? l.sector : SECTOR_UNKNOWN;
            EvaluationSectorGroup g = map.computeIfAbsent(key, k -> {
                EvaluationSectorGroup ng = new EvaluationSectorGroup();
                ng.sectorKey = k;
                ng.sectorLabel = k;
                return ng;
            });
            if (!g.tickers.contains(l.ticker)) {
                g.tickers.add(l.ticker);
            }
            if (l.marketPriceAvailable && l.currentMarketValue != null) {
                g.sectorMarketValue += l.currentMarketValue;
            }
        }
        List<EvaluationSectorGroup> out = new ArrayList<>();
        for (EvaluationSectorGroup g : map.values()) {
            g.sectorMarketValueGbp = gbp.format(g.sectorMarketValue);
            g.tickers.sort(String::compareTo);
            out.add(g);
        }
        return out;
    }

    private static List<EvaluationSectorGroup> buildPriorDaySectorGroups(List<EvaluationPositionLine> lines) {
        TreeMap<String, EvaluationSectorGroup> map = new TreeMap<>();
        for (EvaluationPositionLine l : lines) {
            String key = l.sector != null ? l.sector : SECTOR_UNKNOWN;
            EvaluationSectorGroup g = map.computeIfAbsent(key, k -> {
                EvaluationSectorGroup ng = new EvaluationSectorGroup();
                ng.sectorKey = k;
                ng.sectorLabel = k;
                return ng;
            });
            if (!g.tickers.contains(l.ticker)) {
                g.tickers.add(l.ticker);
            }
        }
        List<EvaluationSectorGroup> out = new ArrayList<>();
        for (EvaluationSectorGroup g : map.values()) {
            g.positionCount = g.tickers.size();
            g.tickers.sort(String::compareTo);
            out.add(g);
        }
        return out;
    }

    private static List<EvaluationSectorGroup> buildPriorEodValueSectorGroups(
            List<EvaluationPositionLine> lines, NumberFormat gbp) {
        TreeMap<String, EvaluationSectorGroup> map = new TreeMap<>();
        for (EvaluationPositionLine l : lines) {
            String key = l.sector != null ? l.sector : SECTOR_UNKNOWN;
            EvaluationSectorGroup g = map.computeIfAbsent(key, k -> {
                EvaluationSectorGroup ng = new EvaluationSectorGroup();
                ng.sectorKey = k;
                ng.sectorLabel = k;
                return ng;
            });
            if (!g.tickers.contains(l.ticker)) {
                g.tickers.add(l.ticker);
            }
            if (l.priorEodPriceAvailable && l.valueAtPriorClose != null) {
                g.sectorValueAtPriorClose += l.valueAtPriorClose;
            }
        }
        List<EvaluationSectorGroup> out = new ArrayList<>();
        for (EvaluationSectorGroup g : map.values()) {
            g.sectorValueAtPriorCloseGbp = gbp.format(g.sectorValueAtPriorClose);
            g.tickers.sort(String::compareTo);
            out.add(g);
        }
        return out;
    }
}

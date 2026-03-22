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

// positions and book from InvestmentService, latest prices from ASEP, cash from shared account
public class PortfolioSnapshotService {

    private static final String SECTOR_UNKNOWN = "Unknown";

    private final InvestmentService investmentService;
    private final EquitiesService equitiesService;
    private final AsepPriceSource priceSource;

    public PortfolioSnapshotService(InvestmentService investmentService,
                                    EquitiesService equitiesService,
                                    AsepPriceSource priceSource) {
        this.investmentService = investmentService;
        this.equitiesService = equitiesService;
        this.priceSource = priceSource;
    }

    public PortfolioSnapshotResponse buildSnapshot() {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);

        List<EvaluationPositionLine> lines = new ArrayList<>();
        for (Position p : investmentService.getPositions()) {
            lines.add(buildLine(p, gbp));
        }

        lines.sort(Comparator
                .comparing((EvaluationPositionLine l) -> !l.marketPriceAvailable)
                .thenComparing((EvaluationPositionLine l) -> -(l.currentMarketValue != null ? l.currentMarketValue : 0.0))
                .thenComparing(l -> l.ticker));

        EvaluationSummary summary = buildSummary(lines, gbp);
        List<EvaluationSectorGroup> sectors = buildSectorGroups(lines, gbp);

        PortfolioSnapshotResponse resp = new PortfolioSnapshotResponse();
        resp.summary = summary;
        resp.bySector = sectors;
        resp.positions = lines;
        return resp;
    }

    private EvaluationPositionLine buildLine(Position p, NumberFormat gbp) {
        EvaluationPositionLine line = new EvaluationPositionLine();
        line.ticker = p.ticker;
        line.shares = p.shares;
        line.bookPricePerShare = p.averageBookPrice;
        line.bookPricePerShareGbp = gbp.format(p.averageBookPrice);
        line.averageBookPricePerShare = p.averageBookPrice;
        line.averageBookPricePerShareGbp = line.bookPricePerShareGbp;
        long bookValue = (long) p.shares * p.averageBookPrice;
        line.bookValue = bookValue;
        line.bookValueGbp = gbp.format(bookValue);

        EquityRecord eq = equitiesService != null ? equitiesService.getEquity(p.ticker) : null;
        line.displayName = eq != null && eq.name != null && !eq.name.isBlank() ? eq.name : p.ticker;
        line.sector = eq != null && eq.sector != null && !eq.sector.isBlank() ? eq.sector.trim() : SECTOR_UNKNOWN;

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
            double current = p.shares * px.pricePerShare;
            line.currentMarketValue = current;
            line.currentMarketValueGbp = gbp.format(current);
            double u = current - bookValue;
            line.unrealizedPnL = u;
            line.unrealizedPnLGbp = gbp.format(u);
            if (bookValue > 0) {
                double pct = 100.0 * u / bookValue;
                line.unrealizedPnLPercent = pct;
                line.unrealizedPnLPercentDisplay = String.format(Locale.UK, "%.2f%%", pct);
            }
            line.statusLabel = "OK";
            line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · book %s · mkt %s (%s) · value %s · P&L %s (%s)",
                    line.ticker, line.displayName, line.shares,
                    line.bookPricePerShareGbp, line.marketPricePerShareGbp, line.marketPriceAsOfDate,
                    line.currentMarketValueGbp, line.unrealizedPnLGbp,
                    line.unrealizedPnLPercentDisplay != null ? line.unrealizedPnLPercentDisplay : "—");
        } else {
            line.marketPriceAvailable = false;
            line.statusLabel = "Price unavailable";
            line.glanceLine = String.format(Locale.UK, "%s (%s) · %d sh · book %s · price unavailable · book value %s",
                    line.ticker, line.displayName, line.shares, line.bookPricePerShareGbp, line.bookValueGbp);
        }
        return line;
    }

    private EvaluationSummary buildSummary(List<EvaluationPositionLine> lines, NumberFormat gbp) {
        EvaluationSummary s = new EvaluationSummary();
        s.priceSourceNote = "Latest prices from ASEP (GET /api/prices/{symbol}), most recent date in the response per holding.";
        s.cashBalance = investmentService.getCashBalance();
        s.cashBalanceGbp = gbp.format(s.cashBalance);

        double marketTotal = 0;
        long bookAll = 0;
        double unrealizedSum = 0;
        long bookForPriced = 0;
        int priced = 0;
        int unpriced = 0;

        for (EvaluationPositionLine l : lines) {
            bookAll += l.bookValue;
            if (l.marketPriceAvailable && l.currentMarketValue != null) {
                marketTotal += l.currentMarketValue;
                unrealizedSum += l.unrealizedPnL != null ? l.unrealizedPnL : 0;
                bookForPriced += l.bookValue;
                priced++;
            } else {
                unpriced++;
                s.notices.add("Price unavailable for " + l.ticker + " — book value " + l.bookValueGbp + " (excluded from market total).");
            }
        }

        s.positionCount = lines.size();
        s.positionsWithMarketPriceCount = priced;
        s.positionsWithoutMarketPriceCount = unpriced;
        s.positionsMarketValue = marketTotal;
        s.positionsMarketValueGbp = gbp.format(marketTotal);
        s.totalBookValueAllPositions = bookAll;
        s.totalBookValueAllPositionsGbp = gbp.format(bookAll);
        s.totalPortfolioValue = marketTotal + s.cashBalance;
        s.totalPortfolioValueGbp = gbp.format(s.totalPortfolioValue);

        if (priced > 0 && bookForPriced > 0) {
            s.totalUnrealizedPnL = unrealizedSum;
            s.totalUnrealizedPnLGbp = gbp.format(unrealizedSum);
            double aggPct = 100.0 * unrealizedSum / bookForPriced;
            s.totalUnrealizedPnLPercent = aggPct;
            s.totalUnrealizedPnLPercentDisplay = String.format(Locale.UK, "%.2f%%", aggPct);
        }
        return s;
    }

    private List<EvaluationSectorGroup> buildSectorGroups(List<EvaluationPositionLine> lines, NumberFormat gbp) {
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
}

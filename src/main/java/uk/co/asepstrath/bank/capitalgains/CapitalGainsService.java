package uk.co.asepstrath.bank.capitalgains;

import uk.co.asepstrath.bank.investments.InvestmentTransaction;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.*;

public class CapitalGainsService {

    private final InvestmentService investmentService;

    // Reconstructed transactions
    private final List<InvestmentTransaction> transactions = new ArrayList<>();

    // Price history: ticker → timestamp → price
    private final Map<String, TreeMap<LocalDateTime, Double>> priceHistory = new HashMap<>();

    // Fractional holdings + cost basis tracking (internal only)
    private final Map<String, Double> holdings = new HashMap<>();
    private final Map<String, Double> costBasis = new HashMap<>();

    private static final double ANNUAL_ALLOWANCE = 3000.0;
    private static final double TAX_RATE = 0.20;

    public CapitalGainsService(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    /* ============================================================
       PRICE CSV LOADING
       ============================================================ */
    public void loadPricesCsv(String path) {
        System.out.println("=== LOADING PRICES CSV: " + path + " ===");

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }

                System.out.println("PRICE ROW: " + line);

                String[] p = line.split(",");
                String ticker = p[0];
                String rawTs = p[1];
                double price = Double.parseDouble(p[2]);

                LocalDateTime ts;
                if (rawTs.length() == 10) {
                    ts = LocalDate.parse(rawTs).atStartOfDay();
                } else {
                    ts = LocalDateTime.parse(rawTs);
                }

                priceHistory
                        .computeIfAbsent(ticker, k -> new TreeMap<>())
                        .put(ts, price);

                System.out.println("→ PRICE LOADED: " + ticker + " @ " + ts + " = " + price);
            }

        } catch (Exception e) {
            System.out.println("ERROR LOADING PRICES CSV");
            e.printStackTrace();
        }

        System.out.println("=== FINISHED LOADING PRICES ===");
    }

    private double lookupPrice(String ticker, LocalDateTime ts) {
        System.out.println("LOOKUP PRICE FOR " + ticker + " @ " + ts);

        TreeMap<LocalDateTime, Double> map = priceHistory.get(ticker);
        if (map == null || map.isEmpty()) {
            System.out.println("→ NO PRICE HISTORY FOR " + ticker);
            return 0;
        }

        Map.Entry<LocalDateTime, Double> entry = map.floorEntry(ts);
        if (entry != null) {
            System.out.println("→ USING FLOOR PRICE: " + entry.getValue());
            return entry.getValue();
        }

        double fallback = map.firstEntry().getValue();
        System.out.println("→ USING EARLIEST PRICE: " + fallback);
        return fallback;
    }

    /* ============================================================
       HISTORY CSV LOADING (fractional shares)
       ============================================================ */
    public void loadHistoryCsv(String path) {
        System.out.println("=== LOADING HISTORY CSV: " + path + " ===");

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }

                System.out.println("HISTORY ROW: " + line);

                String[] p = line.split(",");
                LocalDateTime ts = LocalDateTime.parse(p[0]);
                String type = p[1];
                String ticker = p[2];
                long amountPence = Long.parseLong(p[3]);
                double amount = amountPence / 100.0;

                if (!type.equals("BUY") && !type.equals("SELL")) {
                    System.out.println("→ SKIPPED (not BUY/SELL)");
                    continue;
                }

                double price = lookupPrice(ticker, ts);
                if (price <= 0) {
                    System.out.println("→ SKIPPED (no price found)");
                    continue;
                }

                // FRACTIONAL SHARES
                double shares = amount / price;
                int roundedShares = (int) Math.round(shares);

                System.out.println("→ FRACTIONAL SHARES: " + shares + " (rounded=" + roundedShares + ")");

                InvestmentTransaction tx = new InvestmentTransaction();
                tx.timestamp = ts;
                tx.type = type;
                tx.ticker = ticker;
                tx.price = price;
                tx.shares = roundedShares; // keep Investment UI happy

                if (type.equals("BUY")) {
                    holdings.put(ticker, holdings.getOrDefault(ticker, 0.0) + shares);
                    costBasis.put(ticker, costBasis.getOrDefault(ticker, 0.0) + amount);

                    tx.sharesAfter = (int) Math.round(holdings.get(ticker));
                    System.out.println("→ BUY APPLIED: now holding " + holdings.get(ticker));
                }

                if (type.equals("SELL")) {
                    double prevShares = holdings.getOrDefault(ticker, 0.0);
                    double prevCost = costBasis.getOrDefault(ticker, 0.0);

                    if (prevShares <= 0) {
                        System.out.println("→ SKIPPED SELL (no shares)");
                        continue;
                    }

                    double proportion = shares / prevShares;
                    double costRemoved = prevCost * proportion;

                    double proceeds = amount;
                    double gain = proceeds - costRemoved;

                    holdings.put(ticker, prevShares - shares);
                    costBasis.put(ticker, prevCost - costRemoved);

                    tx.proceeds = proceeds;
                    tx.realizedGainLoss = gain;
                    tx.sharesAfter = (int) Math.round(holdings.get(ticker));

                    System.out.println("→ SELL APPLIED: gain=" + gain);
                }

                transactions.add(tx);
                System.out.println("→ TX ADDED");
            }

        } catch (Exception e) {
            System.out.println("ERROR LOADING HISTORY CSV");
            e.printStackTrace();
        }

        System.out.println("=== FINISHED LOADING HISTORY ===");
        System.out.println("TOTAL TX LOADED: " + transactions.size());
    }

    /* ============================================================
       TAX YEAR HELPERS
       ============================================================ */
    public List<TaxYearInfo> getAvailableTaxYears() {
        System.out.println("=== COMPUTING AVAILABLE TAX YEARS ===");

        Set<Integer> years = new HashSet<>();

        for (InvestmentTransaction tx : transactions) {
            if (!"SELL".equals(tx.type)) continue;
            int y = TaxYearInfo.getTaxYearStart(tx.timestamp);
            years.add(y);
            System.out.println("→ SELL FOUND IN YEAR " + y);
        }

        if (years.isEmpty()) {
            int fallback = TaxYearInfo.getCurrentTaxYearStart();
            years.add(fallback);
            System.out.println("→ NO SELL YEARS, USING CURRENT YEAR " + fallback);
        }

        List<Integer> sorted = new ArrayList<>(years);
        sorted.sort(Collections.reverseOrder());

        List<TaxYearInfo> list = new ArrayList<>();
        for (int y : sorted) list.add(new TaxYearInfo(y));

        System.out.println("YEARS: " + sorted);
        return list;
    }

    public TaxYearInfo resolveYear(Integer year) {
        List<TaxYearInfo> years = getAvailableTaxYears();

        if (year == null) {
            return years.get(0); // latest year with SELLs
        }

        return new TaxYearInfo(year);
    }

    /* ============================================================
       REALISED TRANSACTIONS
       ============================================================ */
    public List<InvestmentTransaction> getRealisedTransactionsForYear(int startYear) {
        List<InvestmentTransaction> list = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        for (InvestmentTransaction tx : transactions) {
            if (!"SELL".equals(tx.type)) continue;
            if (TaxYearInfo.getTaxYearStart(tx.timestamp) == startYear) {

                // Add formatted timestamp for the HBS template
                tx.formattedTimestamp = tx.timestamp.format(fmt);

                list.add(tx);
            }
        }

        return list;
    }

    /* ============================================================
       SUMMARY
       ============================================================ */
    public CapitalGainsSummary getSummaryForYear(int startYear) {
        System.out.println("=== COMPUTING SUMMARY FOR YEAR " + startYear + " ===");

        double totalGains = 0;
        double totalLosses = 0;

        for (InvestmentTransaction tx : transactions) {
            if (!"SELL".equals(tx.type)) continue;
            if (TaxYearInfo.getTaxYearStart(tx.timestamp) != startYear) continue;

            System.out.println("→ SELL TX: gain=" + tx.realizedGainLoss);

            if (tx.realizedGainLoss != null) {
                if (tx.realizedGainLoss >= 0) {
                    totalGains += tx.realizedGainLoss;
                } else {
                    totalLosses += Math.abs(tx.realizedGainLoss);
                }
            }
        }

        double netGain = totalGains - totalLosses;

        double allowanceUsed = Math.min(ANNUAL_ALLOWANCE, Math.max(0, netGain));
        double allowanceRemaining = ANNUAL_ALLOWANCE - allowanceUsed;

        double taxableGain = Math.max(0, netGain - ANNUAL_ALLOWANCE);
        double taxOwed = taxableGain * TAX_RATE;

        System.out.println("SUMMARY:");
        System.out.println("  totalGains=" + totalGains);
        System.out.println("  totalLosses=" + totalLosses);
        System.out.println("  netGain=" + netGain);
        System.out.println("  taxableGain=" + taxableGain);
        System.out.println("  taxOwed=" + taxOwed);

        return new CapitalGainsSummary(
                totalGains,
                totalLosses,
                netGain,
                allowanceUsed,
                allowanceRemaining,
                taxableGain,
                TAX_RATE,
                taxOwed
        );
    }
}


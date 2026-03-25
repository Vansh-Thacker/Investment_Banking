package uk.co.asepstrath.bank.investments;

import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;
import uk.co.asepstrath.bank.evaluation.PriceLoader;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InvestmentService {

    private static final String TYPE_BUY = "BUY";
    private static final String TYPE_SELL = "SELL";

    private final Account account;
    private final CurrentPriceService currentPriceService;
    private final String csv_path;

    private final Map<String, Position> positions = new HashMap<>();
    private final List<InvestmentTransaction> history = new ArrayList<>();

    public InvestmentService(Account account, CurrentPriceService currentPriceService, String csvPath) {
        this.account = account;
        this.currentPriceService = currentPriceService;
        this.csv_path = csvPath;

        ensureCsvExists();
        loadFromCsvOnStartup();
    }

    private void ensureCsvExists() {
        try {
            File f = new File(csv_path);
            File parent = f.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            if (!f.exists()) {
                try (PrintWriter w = new PrintWriter(f)) {
                    w.println("timestamp,type,ticker,amount");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init investments CSV: " + csv_path, e);
        }
    }

    private void loadFromCsvOnStartup() {
        try {
            File f = new File(csv_path);
            if (!f.exists()) return;

            try (InputStream in = new FileInputStream(f)) {
                PriceLoader loader = new PriceLoader("prices.csv");
                importCsv(in, loader);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load investments CSV on startup: " + csv_path, e);
        }
    }

    /* ============================================================
       CSV IMPORT (real prices, Option 1)
       ============================================================ */

    public void importCsv(InputStream csvStream, PriceLoader priceLoader) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {

            String header = reader.readLine();
            if (header == null) return;

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", -1);

                /* ============================
                   FORMAT A: 4 columns
                   timestamp,type,ticker,amountPence
                   ============================ */
                if (parts.length == 5) {
                    LocalDateTime ts = LocalDateTime.parse(parts[0].trim());
                    String type = parts[1].trim().toUpperCase();
                    String ticker = parts[2].trim().toUpperCase();
                    int amountPence = Integer.parseInt(parts[3].trim());

                    if (type.equals("DEPOSIT") || type.equals("WITHDRAW")) continue;
                    if (!type.equals("BUY") && !type.equals("SELL")) continue;

                    Double price = priceLoader.getPrice(ticker, ts);
                    if (price == null) continue;

                    int shares = (int) Math.floor((amountPence / 100.0) / price);
                    if (shares <= 0) continue;

                    if (type.equals("BUY")) {
                        applyBuyFromHistory(ts, ticker, shares, price);
                    } else {
                        applySellFromHistory(ts, ticker, shares, price);
                    }

                    continue;
                }

                /* ============================
                   FORMAT B: 5 columns (your real format)
                   date,type,ticker,amount,balanceAfter
                   ============================ */
                if (parts.length == 5) {

                    if (parts[0].equalsIgnoreCase("date")) continue;

                    LocalDateTime ts = LocalDateTime.parse(parts[0].trim());
                    String type = parts[1].trim().toUpperCase();
                    String ticker = parts[2].trim().toUpperCase();
                    int amountPence = Integer.parseInt(parts[3].trim());

                    if (type.equals("DEPOSIT") || type.equals("WITHDRAW")) continue;
                    if (!type.equals("BUY") && !type.equals("SELL")) continue;

                    Double price = priceLoader.getPrice(ticker, ts);

                    if (price == null) {
                        List<Double> allPrices = priceLoader.getAllPricesForTicker(ticker);

                        if (allPrices != null && !allPrices.isEmpty()) {
                            double sum = 0;
                            for (double p : allPrices) sum += p;
                            price = sum / allPrices.size();
                        }
                    }

                    if (price == null) {
                        System.out.println("NO PRICE FOUND FOR " + ticker + " @ " + ts + " AND NO AVERAGE AVAILABLE");
                        continue;
                    }

                    int shares = (int) Math.floor((amountPence / 100.0) / price);
                    if (shares <= 0) continue;

                    if (type.equals("BUY")) {
                        applyBuyFromHistory(ts, ticker, shares, price);
                    } else {
                        applySellFromHistory(ts, ticker, shares, price);
                    }

                    continue;
                }

                /* ============================
                   FORMAT C: legacy 5-column
                   timestamp,type,ticker,shares,price
                   ============================ */
                if (parts.length >= 5) {
                    LocalDateTime ts = LocalDateTime.parse(parts[0].trim());
                    String type = parts[1].trim().toUpperCase();
                    String ticker = parts[2].trim().toUpperCase();
                    int shares = Integer.parseInt(parts[3].trim());
                    double price = Double.parseDouble(parts[4].trim());

                    if (!type.equals("BUY") && !type.equals("SELL")) continue;

                    if (type.equals("BUY")) {
                        applyBuyFromHistory(ts, ticker, shares, price);
                    } else {
                        applySellFromHistory(ts, ticker, shares, price);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to import investments CSV", e);
        }
    }

    /* ============================================================
       APPLY HISTORY (used by CSV importer)
       ============================================================ */

    private void applyBuyFromHistory(LocalDateTime ts, String ticker, int shares, double price) {
        int cost = (int) Math.round(shares * price * 100);

        Position pos = positions.computeIfAbsent(ticker, k -> new Position());
        pos.ticker = ticker;
        pos.shares += shares;
        pos.totalInvested += cost;
        pos.averageBookPrice = pos.totalInvested / pos.shares;

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = ts;
        tx.type = "BUY";
        tx.ticker = ticker;
        tx.shares = shares;
        tx.price = price;
        tx.proceeds = 0;
        tx.realizedGainLoss = null;
        tx.sharesAfter = pos.shares;

        history.add(tx);
    }

    private void applySellFromHistory(LocalDateTime ts, String ticker, int shares, double price) {
        Position pos = positions.get(ticker);
        int held = pos == null ? 0 : pos.shares;

        int costBasis = (pos != null && pos.shares > 0)
                ? (shares * pos.averageBookPrice)
                : 0;

        double proceeds = shares * price;
        double realized = proceeds - costBasis;
        int sharesAfter = (pos != null && pos.shares >= shares) ? (pos.shares - shares) : 0;

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = ts;
        tx.type = "SELL";
        tx.ticker = ticker;
        tx.shares = shares;
        tx.price = price;
        tx.proceeds = proceeds;
        tx.realizedGainLoss = realized;
        tx.sharesAfter = sharesAfter;

        history.add(tx);

        if (pos != null) {
            pos.totalInvested -= costBasis;
            pos.shares -= shares;
            if (pos.shares <= 0) positions.remove(ticker);
            else pos.averageBookPrice = pos.totalInvested / pos.shares;
        }
    }

    /* ============================================================
       WRITE HISTORY TO CSV (unified 4-column format)
       ============================================================ */

    private void writeHistoryToCsv() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csv_path))) {

            writer.println("date,type,ticker,amount,balanceAfter");

            for (InvestmentTransaction tx : history) {
                int amountPence = (int) Math.round(tx.shares * tx.price * 100);

                writer.println(
                        tx.timestamp + "," +
                        tx.type + "," +
                        tx.ticker + "," +
                        amountPence + "," +
                        0
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + csv_path, e);
        }
    }

    /* ============================================================
       PUBLIC OPERATIONS
       ============================================================ */

    public PurchaseResult newTransaction(String ticker, int shares, double price) {
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares must be positive");
        }

        int costPence = (int) Math.round(shares * price * 100);
        if (costPence > account.getBalance()) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.withdraw(costPence);

        Position pos = positions.computeIfAbsent(ticker, k -> new Position());
        pos.ticker = ticker;
        pos.shares += shares;
        pos.totalInvested += costPence;
        pos.averageBookPrice = pos.totalInvested / pos.shares;

        record(TYPE_BUY, ticker, shares, price, pos.shares);

        PurchaseResult result = new PurchaseResult();
        result.success = true;
        result.ticker = ticker;
        result.shares = shares;
        result.price = price;
        result.balanceAfter = account.getBalance();
        return result;
    }

    public SellConfirmation sell(String ticker, int shares, double price) {
        Position pos = positions.get(ticker);
        if (pos == null || pos.shares < shares) {
            throw new IllegalArgumentException("Not enough shares to sell");
        }

        int costBasis = pos.averageBookPrice * shares;
        int proceedsPence = (int) Math.round(shares * price * 100);

        account.deposit(proceedsPence);

        pos.totalInvested -= costBasis;
        pos.shares -= shares;
        if (pos.shares <= 0) {
            positions.remove(ticker);
        } else {
            pos.averageBookPrice = pos.totalInvested / pos.shares;
        }

        record(TYPE_SELL, ticker, shares, price, pos.shares);

        SellConfirmation c = new SellConfirmation();
        c.ticker = ticker;
        c.shares = shares;
        c.finalSalePrice = price;
        return c;
    }

    public SalePreview getSalePreview(String ticker, int shares, double price) {
        Position pos = positions.get(ticker);
        if (pos == null || pos.shares < shares) {
            throw new IllegalArgumentException("Not enough shares to sell");
        }

        SalePreview preview = new SalePreview();
        preview.name = ticker;
        preview.shares = shares;
        preview.estimatedPricePerShare = pos.averageBookPrice * shares / 100.0;
        return preview;
    }

    /* ============================================================
       GETTERS
       ============================================================ */

    public Collection<Position> getPositions() {
        return positions.values();
    }

    public Position getPosition(String ticker) {
        return positions.get(ticker);
    }

    public int getCashBalance() {
        return account.getBalance();
    }

    public List<InvestmentTransaction> getHistory() {
        return history;
    }

    public List<InvestmentTransaction> getHistorySorted(String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        return history.stream()
                .sorted(asc
                        ? Comparator.comparing(t -> t.timestamp)
                        : Comparator.<InvestmentTransaction, LocalDateTime>comparing(t -> t.timestamp).reversed())
                .collect(Collectors.toList());
    }

    public List<InvestmentTransaction> getPurchaseHistory(String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        return history.stream()
                .filter(tx -> TYPE_BUY.equals(tx.type))
                .sorted(asc
                        ? Comparator.comparing(t -> t.timestamp)
                        : Comparator.<InvestmentTransaction, LocalDateTime>comparing(t -> t.timestamp).reversed())
                .collect(Collectors.toList());
    }

    /* ============================================================
       RECORD HELPERS
       ============================================================ */

    private void record(String type, String ticker, int shares, double price, int sharesAfter) {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = LocalDateTime.now();
        tx.type = type;
        tx.ticker = ticker;
        tx.shares = shares;
        tx.price = price;
        tx.sharesAfter = sharesAfter;

        history.add(tx);
        writeHistoryToCsv();
    }
}
package uk.co.asepstrath.bank.investments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.PriceLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InvestmentService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentService.class);
    private static final String TYPE_BUY = "BUY";
    private static final String TYPE_SELL = "SELL";

    private final Account account;
    final Map<String, Position> positions = new HashMap<>();
    final List<InvestmentTransaction> history = new ArrayList<>();

    public InvestmentService(Account account) {
        this.account = account;
    }

    /* ============================================================
       NEW: copy state from another InvestmentService
       ============================================================ */
    public void copyFrom(InvestmentService other) {
        this.positions.clear();
        this.positions.putAll(other.positions);

        this.history.clear();
        this.history.addAll(other.history);

        this.account.forceSetBalance(other.getCashBalance());
    }

    /* ============================================================
       TRADING METHODS (Controller depends on these)
       ============================================================ */

    public void buy(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }

        int cost = (int) Math.round(shares * price);
        int balance = account.getBalance();

        if (balance < cost) {
            log.warn("Insufficient funds for BUY {}. Processing anyway (historical).", ticker);
            account.withdrawAllowNegative(cost);
        } else {
            account.withdraw(cost);
        }

        Position pos = positions.computeIfAbsent(ticker, k -> new Position());
        pos.ticker = ticker;
        pos.shares += shares;
        pos.totalInvested += cost;
        pos.averageBookPrice = pos.totalInvested / pos.shares;

        record(TYPE_BUY, ticker, shares, price, pos.shares);
    }

    public PurchaseResult newTransaction(String ticker, int shares, double price) {
        PurchaseResult result = new PurchaseResult();

        if (ticker == null || ticker.isBlank()) {
            result.success = false;
            result.message = "Ticker is required";
            return result;
        }

        String normalized = ticker.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{1,5}")) {
            result.success = false;
            result.message = "Ticker must be 1–5 uppercase letters";
            return result;
        }

        if (shares <= 0) {
            result.success = false;
            result.message = "Number of shares must be positive";
            return result;
        }

        if (price <= 0) {
            result.success = false;
            result.message = "Price must be positive";
            return result;
        }

        buy(normalized, shares, price);

        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        int cost = (int) Math.round(shares * price);

        result.success = true;
        result.message = "Transaction successful.";
        result.ticker = normalized;
        result.shares = shares;
        result.price = price;
        result.totalCostGbp = gbp.format(cost);
        result.balanceAfter = account.getBalance();

        return result;
    }

    public SellConfirmation sell(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }

        Position pos = positions.get(ticker);
        int held = pos == null ? 0 : pos.shares;

        if (pos == null || pos.shares < shares) {
            log.error("Insufficient shares for SELL {}. Processing anyway (historical).", ticker);
        }

        int costBasis = (pos != null && pos.shares > 0)
                ? (shares * pos.averageBookPrice)
                : 0;

        double proceeds = shares * price;
        double realizedGainLoss = proceeds - costBasis;
        int sharesAfter = (pos != null && pos.shares >= shares) ? (pos.shares - shares) : 0;

        record(TYPE_SELL, ticker, shares, price, proceeds, realizedGainLoss, sharesAfter);

        account.deposit((int) Math.round(proceeds));

        if (pos == null) {
            pos = new Position();
            pos.ticker = ticker;
            pos.shares = 0;
            pos.averageBookPrice = 0;
            pos.totalInvested = 0;
            positions.put(ticker, pos);
        }

        pos.totalInvested -= costBasis;
        pos.shares -= shares;

        if (pos.shares <= 0) {
            positions.remove(ticker);
        } else {
            pos.averageBookPrice = pos.totalInvested / pos.shares;
        }

        return buildSellConfirmation(ticker, shares, price);
    }

    private static SellConfirmation buildSellConfirmation(String ticker, int shares, double price) {
        NumberFormat gbp = NumberFormat.getCurrencyInstance(Locale.UK);
        double totalEarnings = shares * price;

        SellConfirmation c = new SellConfirmation();
        c.ticker = ticker;
        c.shares = shares;
        c.finalSalePrice = price;
        c.finalSalePriceGbp = gbp.format(price);
        c.totalEarningsGbp = gbp.format(totalEarnings);
        c.successMessage = String.format(
                "Sale successful. Sold %d shares of %s at %s for total earnings %s.",
                shares, ticker, c.finalSalePriceGbp, c.totalEarningsGbp
        );
        c.portfolioUrl = "/investments/positions";
        c.transactionHistoryUrl = "/investments/transactions";

        return c;
    }

    public SalePreview getSalePreview(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }

        SalePreview p = new SalePreview();
        p.symbol = ticker;
        p.name = ticker;
        p.shares = shares;
        p.estimatedPricePerShare = price;
        p.estimatedTotalProceedsGbp =
                NumberFormat.getCurrencyInstance(Locale.UK).format(shares * price);

        return p;
    }

    /* ============================================================
       CSV IMPORT (real prices, Option 1)
       ============================================================ */

    public void importCsv(InputStream csvStream, PriceLoader priceLoader) {
        System.out.println("DEBUG: importCsv() STARTED");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {

            String header = reader.readLine();
            if (header == null) return;

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", -1);
                System.out.println("DEBUG: Read CSV line: " + line);

                /* ============================
                   FORMAT A: 4 columns
                   timestamp,type,ticker,amountPence
                   ============================ */
                if (parts.length == 4) {
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

                    System.out.println("DEBUG: Processing 5-column row: type=" + type + ", ticker=" + ticker);

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
        int cost = (int) Math.round(shares * price);

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

        System.out.println("DEBUG: APPLY " + tx.type + " " + shares + " shares of " + ticker + " at " + price);
        System.out.println("DEBUG: Added BUY transaction to history: " + ticker);

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

        System.out.println("DEBUG: APPLY " + tx.type + " " + shares + " shares of " + ticker + " at " + price);
        System.out.println("DEBUG: Added BUY transaction to history: " + ticker);

        history.add(tx);

        if (pos != null) {
            pos.totalInvested -= costBasis;
            pos.shares -= shares;
            if (pos.shares <= 0) positions.remove(ticker);
            else pos.averageBookPrice = pos.totalInvested / pos.shares;
        }
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

    private void record(String type, String ticker, int shares, double price) {
        record(type, ticker, shares, price, 0.0, null, null);
    }

    private void record(String type, String ticker, int shares, double price, int sharesAfter) {
        record(type, ticker, shares, price, 0.0, null, sharesAfter);
    }

    private void record(String type, String ticker, int shares, double price, double proceeds, Double realizedGainLoss) {
        record(type, ticker, shares, price, proceeds, realizedGainLoss, null);
    }

    private void record(String type, String ticker, int shares, double price,
                        double proceeds, Double realizedGainLoss, Integer sharesAfter) {

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = LocalDateTime.now();
        tx.type = type;
        tx.ticker = ticker;
        tx.shares = shares;
        tx.price = price;
        tx.proceeds = proceeds;
        tx.realizedGainLoss = realizedGainLoss;
        tx.sharesAfter = sharesAfter;

        history.add(tx);
    }
}

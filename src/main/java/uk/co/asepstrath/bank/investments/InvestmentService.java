package uk.co.asepstrath.bank.investments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.asepstrath.bank.Account;
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

    private final Account account;
    private final Map<String, Position> positions = new HashMap<>();
    private final List<InvestmentTransaction> history = new ArrayList<>();

    public InvestmentService(Account account) {
        this.account = account;
    }

    public void buy(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }
        // rounding to int to match Account withdraw(int)
        int cost = (int) Math.round(shares * price);
        account.withdraw(cost);

        // get or make position for this ticker 
        Position position = positions.computeIfAbsent(ticker, k -> {
            Position p = new Position();
            p.ticker = ticker;
            p.shares = 0;
            p.averageBookPrice = 0;
            p.totalInvested = 0;
            return p;
        });
        position.shares += shares;
        position.totalInvested += cost;
        // avg price per share -- int div as position uses it for averageBookPrice
        position.averageBookPrice = position.totalInvested / position.shares;

        record(TYPE_BUY, ticker, shares, price);
    }

    
    // execute sale, decrease holdings, credit account, close position if 0
    // record transaction with timestamp, price and proceeds. returns confirmation
     
    public SellConfirmation sell(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }
        Position position = positions.get(ticker);
        int held = position == null ? 0 : position.shares;
        if (position == null || position.shares < shares) {
            log.error("Validation: insufficient shares for SELL {} - held {}, requested {}. Processing transaction anyway (historical data).", ticker, held, shares);
        }
        double proceeds = shares * price;
        record("SELL", ticker, shares, price, proceeds);
        account.deposit((int) Math.round(proceeds));
        if (position == null) {
            position = new Position();
            position.ticker = ticker;
            position.shares = 0;
            position.averageBookPrice = 0;
            position.totalInvested = 0;
            positions.put(ticker, position);
        }
        position.shares -= shares;
        if (position.shares <= 0) {
            positions.remove(ticker);
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
        c.successMessage = String.format("Sale successful. Sold %d shares of %s at %s for total earnings %s.",
                shares, ticker, c.finalSalePriceGbp, c.totalEarningsGbp);
        c.portfolioUrl = "/investments/positions";
        c.transactionHistoryUrl = "/investments/transactions";
        return c;
    }

    
    // return sale details for confirmation screen. sale executes when user calls sell()
    
    public SalePreview getSalePreview(String ticker, int shares, double price) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }
        SalePreview p = new SalePreview();
        p.symbol = ticker;
        p.name = ticker; // placeholder until i integerate stock name source
        p.shares = shares;
        p.estimatedPricePerShare = price;
        p.estimatedTotalProceedsGbp = NumberFormat.getCurrencyInstance(Locale.UK).format(shares * price);
        return p;
    }

    public void importCsv(InputStream csvStream) {
    //imports the csv file and stores it into an investment transaction object
    }

    private void record(String type, String ticker, int shares, double price) {
        record(type, ticker, shares, price, 0.0);
    }

    private void record(String type, String ticker, int shares, double price, double proceeds) {
        if (shares <= 0 || price <= 0) {
            throw new IllegalArgumentException("Shares and price must be positive");
        }
        InvestmentTransaction invRecord = new InvestmentTransaction();
        invRecord.timestamp = LocalDateTime.now();
        invRecord.type = type;
        invRecord.ticker = ticker;
        invRecord.shares = shares;
        invRecord.price = price;
        invRecord.proceeds = proceeds;
        history.add(invRecord);
    }

    public Collection<Position> getPositions() {
        return positions.values(); //get current stock holdings
    }

    public Position getPosition(String ticker) {
        return positions.get(ticker); //return specific stock holding
    }

    public List<InvestmentTransaction> getHistory() {
        return history; //returns buy and sell history
    }

    // returns only BUY (purchase) transactions sorted by timestamp
    // ascending = sortOrder "asc" for oldest first, null or desc for newest
    
    public List<InvestmentTransaction> getPurchaseHistory(String sortOrder) {
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);
        return history.stream()
                .filter(tx -> TYPE_BUY.equals(tx.type))
                .sorted(ascending
                        ? Comparator.comparing(t -> t.timestamp)
                        : Comparator.<InvestmentTransaction, LocalDateTime>comparing(t -> t.timestamp).reversed())
                .collect(Collectors.toList());
    }
}


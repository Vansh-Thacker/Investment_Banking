package uk.co.asepstrath.bank.investments;

import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.Locale;

public class InvestmentTransaction {
    public LocalDateTime timestamp;
    public String type; // BUY or SELL
    public String ticker;
    public int shares;
    public double price;
    // proceeds from a sale (shares * price) set when type is sell
    public double proceeds;
    // realized gain/loss on SELL: sale proceeds
    public Double realizedGainLoss;
    // shares owned in this ticker after this transaction (for transaction list) 0 = position closed
    public Integer sharesAfter;
    // stock name from equities (set when transaction list is built, fallback to ticker if not resolved)
    public String stockName;

    // true when transaction closed the position
    public boolean isPositionClosed() {
        return "SELL".equals(type) && sharesAfter != null && sharesAfter == 0;
    }

    // total cost of trade (shares * price) formatted in gbp
    public String getTotalCostGbp() {
        if (shares <= 0 || price <= 0) {
            return NumberFormat.getCurrencyInstance(Locale.UK).format(0.0);
        }
        double total = shares * price;
        return NumberFormat.getCurrencyInstance(Locale.UK).format(total);
    }

    
    // signed value for t_earnings. display + for SELL, - for BUY
    public double getTotalEarningsSigned() {
        if ("SELL".equals(type) && proceeds > 0) {
            return proceeds;
        }
        if (shares <= 0 || price <= 0) {
            return 0.0;
        }
        double amount = shares * price;
        return "SELL".equals(type) ? amount : -amount;
    }

    
    // total earnings in gbp, formatted with - for BUY and + for SELL
    
    public String getTotalEarningsGbp() {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(getTotalEarningsSigned());
    }

    // no. of shares with sign: + for BUY, - for SELL (for transaction list display)
    public int getSharesSigned() {
        if ("SELL".equals(type)) {
            return shares <= 0 ? 0 : -shares;
        }
        return shares <= 0 ? 0 : shares;
    }

    // Price per share at execution, formatted in gbp
    public String getPricePerShareGbp() {
        if (price <= 0) {
            return NumberFormat.getCurrencyInstance(Locale.UK).format(0.0);
        }
        return NumberFormat.getCurrencyInstance(Locale.UK).format(price);
    }
}

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
    /** Proceeds from a sale (shares * price). Set when type is SELL. */
    public double proceeds;

    /** Total cost of this trade (shares * price) formatted in GBP for display. */
    public String getTotalCostGbp() {
        if (shares <= 0 || price <= 0) {
            return NumberFormat.getCurrencyInstance(Locale.UK).format(0.0);
        }
        double total = shares * price;
        return NumberFormat.getCurrencyInstance(Locale.UK).format(total);
    }
}

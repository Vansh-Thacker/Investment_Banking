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
    // realized gain/loss on SELL: sale proceeds - cost basis
    public Double realizedGainLoss;

    // total cost of trade (shares * price) formatted in gbp
    public String getTotalCostGbp() {
        if (shares <= 0 || price <= 0) {
            return NumberFormat.getCurrencyInstance(Locale.UK).format(0.0);
        }
        double total = shares * price;
        return NumberFormat.getCurrencyInstance(Locale.UK).format(total);
    }
}

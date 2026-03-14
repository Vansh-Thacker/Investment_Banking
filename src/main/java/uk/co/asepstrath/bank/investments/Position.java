package uk.co.asepstrath.bank.investments;

import java.text.NumberFormat;
import java.util.Locale;

public class Position {
    public String ticker;
    public int shares;
    public int averageBookPrice;
    public int totalInvested;
    // stock name from equities (set when positions list is built, fallback to ticker if not resolved
    public String stockName;
    // price per share at time of request (from /api/prices), null if not available
    public Double currentPrice;
    // current value = shares × currentPrice. null if currentPrice not available
    public Double currentValue;

    // avg price per share (from BUYs only), formatted in gbp
    public String getAveragePurchasePriceGbp() {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(averageBookPrice);
    }

    /** Total cost of units held (total invested amount), formatted in GBP for display. */
    public String getTotalInvestedGbp() {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(totalInvested);
    }

    // value (shares × latest price) formatted in GBP; "—" if not available
    public String getCurrentValueGbp() {
        if (currentValue == null) {
            return "—";
        }
        return NumberFormat.getCurrencyInstance(Locale.UK).format(currentValue);
    }

    // book value = shares × average book price ( Unrealized p&l)
    public int getBookValue() {
        return shares * averageBookPrice;
    }

    // unrealized P&L = current value − book value; null if currentValue not available
    public Double getUnrealizedPnL() {
        if (currentValue == null) {
            return null;
        }
        return currentValue - getBookValue();
    }

    // unrealized p&l % = (p&l / book value) × 100; null if book value <= 0 or no currentVal
    public Double getUnrealizedPnLPercent() {
        if (currentValue == null) {
            return null;
        }
        int book = getBookValue();
        if (book <= 0) {
            return null;
        }
        return 100.0 * (currentValue - book) / book;
    }
}

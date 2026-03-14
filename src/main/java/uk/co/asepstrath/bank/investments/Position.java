package uk.co.asepstrath.bank.investments;

import java.text.NumberFormat;
import java.util.Locale;

public class Position {
    public String ticker;
    public int shares;
    public int averageBookPrice;
    public int totalInvested;
    /** Stock name from equities (set when positions list is built; fallback to ticker if not resolved). */
    public String stockName;
    /** Latest price per share at time of request (from /api/prices); null if not available. */
    public Double currentPrice;
    /** Current value = shares × currentPrice; null if currentPrice not available. */
    public Double currentValue;

    /** Average purchase price per share (from BUYs only), formatted in GBP for display. */
    public String getAveragePurchasePriceGbp() {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(averageBookPrice);
    }

    /** Total cost of units held (total invested amount), formatted in GBP for display. */
    public String getTotalInvestedGbp() {
        return NumberFormat.getCurrencyInstance(Locale.UK).format(totalInvested);
    }

    /** Current value (shares × latest price) formatted in GBP; "—" if not available. */
    public String getCurrentValueGbp() {
        if (currentValue == null) {
            return "—";
        }
        return NumberFormat.getCurrencyInstance(Locale.UK).format(currentValue);
    }

    /** Book value = shares × average book price (§3 Unrealized P&L). */
    public int getBookValue() {
        return shares * averageBookPrice;
    }

    /** Unrealized P&L = current value − book value; null if currentValue not available (§3). */
    public Double getUnrealizedPnL() {
        if (currentValue == null) {
            return null;
        }
        return currentValue - getBookValue();
    }

    /** Unrealized P&L % = (P&L / book value) × 100; null if book value ≤ 0 or currentValue not available (§3). */
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

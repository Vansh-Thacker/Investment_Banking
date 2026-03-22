package uk.co.asepstrath.bank.investments;

import java.text.NumberFormat;
import java.util.Locale;

public class Position {

    public String ticker;
    public int shares;
    public int averageBookPrice;   // stored in pence
    public int totalInvested;      // stored in pence
    public String stockName;
    public Double currentPrice;    // latest price per share
    public Double currentValue;    // shares × currentPrice

    private static final NumberFormat GBP = NumberFormat.getCurrencyInstance(Locale.UK);

    /* ============================================================
       BASIC GETTERS FOR HANDLEBARS
       ============================================================ */

    public String getTicker() {
        return ticker;
    }

    public String getStockName() {
        return stockName != null ? stockName : ticker;
    }

    public int getShares() {
        return shares;
    }

    /* ============================================================
       FORMATTED VALUES FOR investments.hbs
       ============================================================ */

    public String getAverageBookPrice() {
        return GBP.format(averageBookPrice);
    }

    public String getTotalInvested() {
        return GBP.format(totalInvested);
    }

    public String getCurrentPrice() {
        return currentPrice != null ? GBP.format(currentPrice) : "£0.00";
    }

    public String getCurrentValue() {
        return currentValue != null ? GBP.format(currentValue) : "£0.00";
    }

    /* ============================================================
       EXISTING LOGIC (kept exactly as you had it)
       ============================================================ */

    public String getAveragePurchasePriceGbp() {
        return GBP.format(averageBookPrice);
    }

    public String getTotalInvestedGbp() {
        return GBP.format(totalInvested);
    }

    public String getCurrentValueGbp() {
        if (currentValue == null) {
            return "—";
        }
        return GBP.format(currentValue);
    }

    public int getBookValue() {
        return shares * averageBookPrice;
    }

    public Double getUnrealizedPnL() {
        if (currentValue == null) {
            return null;
        }
        return currentValue - getBookValue();
    }

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

    /* ============================================================
       FORMATTED PNL FOR investments.hbs
       ============================================================ */

    public String getUnrealizedPnLFormatted() {
        Double pnl = getUnrealizedPnL();
        return pnl != null ? GBP.format(pnl) : "£0.00";
    }

    public String getUnrealizedPnLPercentFormatted() {
        Double pct = getUnrealizedPnLPercent();
        return pct != null ? String.format("%.2f%%", pct) : "0%";
    }
}
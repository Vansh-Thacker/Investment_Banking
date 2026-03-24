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
        return GBP.format(averageBookPrice / 100.0); // displaying it as a double (decimal)
    }

    public String getTotalInvested() {
        return GBP.format(totalInvested / 100.0); // displaying it as a double (decimal)
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
        return totalInvested;
    }

    public Double getUnrealizedPnL() {
        if (currentValue == null) {
            return null;
        }

        double bookPounds = getBookValue() / 100.0; // convert pence → pounds
        return currentValue - bookPounds;
    }

    public Double getUnrealizedPnLPercent() {
        if (currentValue == null) {
            return null;
        }

        double bookPounds = getBookValue() / 100.0; // convert pence → pounds

        if (bookPounds <= 0) {
            return null;
        }

        return 100.0 * (currentValue - bookPounds) / bookPounds;
    }

    public String getUnrealizedPnLGbp() {
        Double pnl = getUnrealizedPnL();
        if (pnl == null) {
            return "—";
        }
        return GBP.format(pnl); // pnl is already in pounds
    }

    public String getUnrealizedPnLPercentFormatted() {
        Double pct = getUnrealizedPnLPercent();
        if (pct == null) {
            return "—";
        }
        return String.format("%.2f%%", pct);
    }
}
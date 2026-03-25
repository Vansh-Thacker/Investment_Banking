package uk.co.asepstrath.bank.investments;

import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.Locale;

public class InvestmentTransaction {

    public LocalDateTime timestamp;
    public String type;          // BUY or SELL
    public String ticker;
    public int shares;           // always positive internally
    public double price;         // price per share
    public double proceeds;      // SELL only
    public Double realizedGainLoss;
    public Integer sharesAfter;
    public String stockName;

    private static final NumberFormat GBP = NumberFormat.getCurrencyInstance(Locale.UK);

    /* ============================================================
       SIGNED SHARES (HTML expects {{sharesSigned}})
       BUY  -> +shares
       SELL -> -shares
       ============================================================ */
    public int getSharesSigned() {
        if ("SELL".equals(type)) {
            return shares <= 0 ? 0 : -shares;
        }
        return shares <= 0 ? 0 : shares;
    }

    /* ============================================================
       PRICE PER SHARE (HTML expects {{pricePerShareGbp}})
       ============================================================ */
    public String getPricePerShareGbp() {
        if (price <= 0) return GBP.format(0.0);
        return GBP.format(price);
    }

    /* ============================================================
       TOTAL EARNINGS SIGNED (BUY negative, SELL positive)
       HTML expects {{totalEarningsGbp}}
       ============================================================ */
    public double getTotalEarningsSigned() {
        if ("SELL".equals(type)) {
            return proceeds; // already shares * price
        }
        if (shares <= 0 || price <= 0) return 0.0;
        return -(shares * price); // BUY is negative
    }

    public String getTotalEarningsGbp() {
        return GBP.format(getTotalEarningsSigned());
    }

    /* ============================================================
       TOTAL COST (not used by your HTML but kept for completeness)
       ============================================================ */
    public String getTotalCostGbp() {
        if (shares <= 0 || price <= 0) return GBP.format(0.0);
        return GBP.format(shares * price);
    }

    /* ============================================================
       POSITION CLOSED CHECK
       ============================================================ */
    public boolean isPositionClosed() {
        return "SELL".equals(type) && sharesAfter != null && sharesAfter == 0;
    }

    public String getTimestampFormatted() {
        return timestamp != null ? timestamp.toString() : "";
    }

    public String getType() {
        return type;
    }

    public String getTicker() {
        return ticker;
    }
}
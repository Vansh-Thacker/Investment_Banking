package uk.co.asepstrath.bank.investments;

/**
 * Details for the sale confirmation screen. User reviews this before confirming;
 * sale executes only when they call POST /sell with the same parameters.
 */
public class SalePreview {
    /** Stock symbol (ticker). */
    public String symbol;
    /** Stock name; may be empty until a name source is integrated. */
    public String name;
    /** Number of shares to sell. */
    public int shares;
    /** Estimated price per share. */
    public double estimatedPricePerShare;
    /** Estimated total proceeds (shares * price) formatted in GBP. */
    public String estimatedTotalProceedsGbp;
}

package uk.co.asepstrath.bank.investments;

// details for the sale confirmation screen, user reviews before confirming
// sale executes only when they call post /sell with the same params

public class SalePreview {
    public String symbol;
    public String name;
    public int shares;
    public double estimatedPricePerShare;
    public String estimatedTotalProceedsGbp;
}

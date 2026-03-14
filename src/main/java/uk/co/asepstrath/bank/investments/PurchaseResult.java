package uk.co.asepstrath.bank.investments;

/**
 * Result of a buy (newTransaction): success/failure and message for immediate user notification.
 * When successful, includes transaction details and balance after (§1 BUY, §2 cash integration).
 */
public class PurchaseResult {
    public boolean success;
    public String message;
    public String ticker;
    public int shares;
    public double price;
    public String totalCostGbp;
    public int balanceAfter;
}

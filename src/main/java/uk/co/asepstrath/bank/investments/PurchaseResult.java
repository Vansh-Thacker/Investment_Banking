package uk.co.asepstrath.bank.investments;

// result of buy, success/failure and message for immediate user notification.
// when successfull, includes transaction details and bal after

public class PurchaseResult {
    public boolean success;
    public String message;
    public String ticker;
    public int shares;
    public double price;
    public String totalCostGbp;
    public int balanceAfter;
}

package uk.co.asepstrath.bank.investments;

/**
 * Response returned after a successful share sale. Includes success message,
 * sale details, and URLs to navigate back to portfolio or transaction history.
 */
public class SellConfirmation {
    public String successMessage;
    public String ticker;
    public int shares;
    public double finalSalePrice;
    public String finalSalePriceGbp;
    public String totalEarningsGbp;
    public String portfolioUrl;
    public String transactionHistoryUrl;
}

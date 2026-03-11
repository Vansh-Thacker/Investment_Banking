package uk.co.asepstrath.bank.investments;


// response returned after successful share sale, includes success msg, sale details and urls to navigate back to portfolio or transaction history

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

package uk.co.asepstrath.bank.investments;

// response for GET /investments/prices/{symbol} – current market price for confirmation screen

public class CurrentPriceResponse {
    public String symbol;
    public double price;
    public String date;
}

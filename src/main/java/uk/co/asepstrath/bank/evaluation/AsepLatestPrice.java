package uk.co.asepstrath.bank.evaluation;

// Last EOD price DTO returned from ASEP {@code GET /api/prices/{symbol}}
public class AsepLatestPrice {
    public double pricePerShare;
    public String date;
}

package uk.co.asepstrath.bank.investments;

// response for GET /investments/portfolio/summary
// total portfolio val = sum of all pos current values + cash balance

public class PortfolioSummaryResponse {
    // total portfolio value = positionsValue + cashBalance
    public double totalPortfolioValue;
    // sum of current vals of all pos (shares × latest price)
    public double positionsValue;
    public int cashBalance;
}

package uk.co.asepstrath.bank.investments;

/**
 * Response for GET /investments/portfolio/summary (§3 – portfolio-level evaluation).
 * Total portfolio value = sum of all position current values + cash balance.
 */
public class PortfolioSummaryResponse {
    /** Total portfolio value = positionsValue + cashBalance. */
    public double totalPortfolioValue;
    /** Sum of current values of all positions (shares × latest price). */
    public double positionsValue;
    /** Cash balance. */
    public int cashBalance;
}

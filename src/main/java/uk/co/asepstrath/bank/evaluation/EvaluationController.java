package uk.co.asepstrath.bank.evaluation;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/evaluation")
public class EvaluationController {

    private final PortfolioEvaluationService evaluation;

    public EvaluationController(PortfolioEvaluationService evaluation) {
        this.evaluation = evaluation;
    }

    @GET("/portfolio/snapshot")
    public Object portfolioSnapshot() {
        return evaluation.buildSnapshot();
    }

    // aggregate totals only — same summary as portfolio snapshot
    @GET("/portfolio/performance")
    public Object portfolioPerformanceOverview() {
        return evaluation.buildPortfolioPerformanceOverview();
    }

    @GET("/positions/average-book-price")
    public Object averageBookPriceOverview() {
        return evaluation.buildBookPriceOverview();
    }

    // same payload as average-book-price
    @GET("/positions/cost-basis")
    public Object costBasisOverview() {
        return evaluation.buildBookPriceOverview();
    }

    // prior session EOD per holding from ASEP
    @GET("/positions/prior-day-eod")
    public Object priorDayEodOverview() {
        return evaluation.buildPriorDayEodOverview();
    }

    // shares × latest ASEP close per position
    @GET("/positions/market-value")
    public Object marketValueOverview() {
        return evaluation.buildMarketValueOverview();
    }

    // shares × prior session EOD
    @GET("/positions/value-at-prior-close")
    public Object valueAtPriorCloseOverview() {
        return evaluation.buildPriorEodValueOverview();
    }

    // book vs ASEP last close and unrealized P&L
    @GET("/positions/unrealized-pnl")
    public Object unrealizedPnLOverview() {
        return evaluation.buildUnrealizedPnLOverview();
    }

    // same payload as unrealized-pnl
    @GET("/positions/unrealized-gains-losses")
    public Object unrealizedGainsLossesOverview() {
        return evaluation.buildUnrealizedPnLOverview();
    }

    // absolute £ and % unrealized per position
    @GET("/positions/gains-losses")
    public Object absoluteAndPercentageGainsLossesOverview() {
        return evaluation.buildUnrealizedPnLOverview();
    }
}

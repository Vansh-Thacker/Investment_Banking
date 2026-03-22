package uk.co.asepstrath.bank.evaluation;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.Collections;

@Path("/evaluation")
public class EvaluationController {

    private final PortfolioEvaluationService evaluation;
    private final EvaluationPortfolioLoader portfolioLoader;

    public EvaluationController(PortfolioEvaluationService evaluation, EvaluationPortfolioLoader portfolioLoader) {
        this.evaluation = evaluation;
        this.portfolioLoader = portfolioLoader;
    }

    @FunctionalInterface
    private interface PortfolioAction {
        Object run(InvestmentService inv);
    }

    private Object evalWithSessionPortfolio(Context ctx, PortfolioAction action) {
        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null || userId.isBlank()) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Authentication required");
        }
        try {
            InvestmentService inv = portfolioLoader.loadForUserId(userId);
            return action.run(inv);
        } catch (IllegalArgumentException e) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Authentication required");
        } catch (RuntimeException e) {
            ctx.setResponseCode(StatusCode.SERVER_ERROR);
            return Collections.singletonMap("error", "Failed to load portfolio");
        }
    }

    @GET("/portfolio/snapshot")
    public Object portfolioSnapshot(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildSnapshot);
    }

    @GET("/portfolio/performance")
    public Object portfolioPerformanceOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildPortfolioPerformanceOverview);
    }

    @GET("/positions/average-book-price")
    public Object averageBookPriceOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildBookPriceOverview);
    }

    @GET("/positions/cost-basis")
    public Object costBasisOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildBookPriceOverview);
    }

    @GET("/positions/prior-day-eod")
    public Object priorDayEodOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildPriorDayEodOverview);
    }

    @GET("/positions/market-value")
    public Object marketValueOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildMarketValueOverview);
    }

    @GET("/positions/value-at-prior-close")
    public Object valueAtPriorCloseOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildPriorEodValueOverview);
    }

    @GET("/positions/unrealized-pnl")
    public Object unrealizedPnLOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildUnrealizedPnLOverview);
    }

    @GET("/positions/unrealized-gains-losses")
    public Object unrealizedGainsLossesOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildUnrealizedPnLOverview);
    }

    @GET("/positions/gains-losses")
    public Object absoluteAndPercentageGainsLossesOverview(Context ctx) {
        return evalWithSessionPortfolio(ctx, evaluation::buildUnrealizedPnLOverview);
    }
}

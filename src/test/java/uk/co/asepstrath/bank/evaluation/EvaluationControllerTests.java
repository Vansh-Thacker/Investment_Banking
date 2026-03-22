package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluationControllerTests {

    private static EvaluationController controllerWithEmptyAsep(InvestmentService inv) {
        AsepPriceSource empty = ticker -> Optional.empty();
        return new EvaluationController(new PortfolioEvaluationService(inv, null, empty));
    }

    @Test
    public void portfolioSnapshotDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.portfolioSnapshot();
        assertTrue(body instanceof PortfolioSnapshotResponse);
        PortfolioSnapshotResponse resp = (PortfolioSnapshotResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void portfolioPerformanceOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        inv.buy("Z", 1, 50.0);
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.portfolioPerformanceOverview();
        assertTrue(body instanceof PortfolioPerformanceOverviewResponse);
        PortfolioPerformanceOverviewResponse perf = (PortfolioPerformanceOverviewResponse) body;
        assertNotNull(perf.summary);
        PortfolioSnapshotResponse snap = (PortfolioSnapshotResponse) controller.portfolioSnapshot();
        assertEquals(snap.summary.positionCount, perf.summary.positionCount);
        assertEquals(snap.summary.totalBookValueAllPositions, perf.summary.totalBookValueAllPositions);
        assertEquals(snap.summary.cashBalance, perf.summary.cashBalance);
        assertEquals(snap.summary.totalPortfolioValue, perf.summary.totalPortfolioValue, 0.01);
    }

    @Test
    public void averageBookPriceOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.averageBookPriceOverview();
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void costBasisOverviewReturnsSameShapeAsBookOverview() {
        InvestmentService inv = new InvestmentService(new Account());
        inv.buy("X", 1, 10.0);
        EvaluationController controller = controllerWithEmptyAsep(inv);
        EvaluationOverviewResponse avg = (EvaluationOverviewResponse) controller.averageBookPriceOverview();
        EvaluationOverviewResponse cost = (EvaluationOverviewResponse) controller.costBasisOverview();
        assertEquals(avg.positions.size(), cost.positions.size());
        assertEquals(avg.positions.get(0).bookValue, cost.positions.get(0).bookValue);
    }

    @Test
    public void marketValueOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.marketValueOverview();
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void valueAtPriorCloseOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.valueAtPriorCloseOverview();
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void unrealizedPnLOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.unrealizedPnLOverview();
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void unrealizedGainsLossesAliasMatchesUnrealizedPnl() {
        InvestmentService inv = new InvestmentService(new Account());
        inv.buy("Z", 1, 50.0);
        EvaluationController controller = controllerWithEmptyAsep(inv);
        EvaluationOverviewResponse a = (EvaluationOverviewResponse) controller.unrealizedPnLOverview();
        EvaluationOverviewResponse b = (EvaluationOverviewResponse) controller.unrealizedGainsLossesOverview();
        assertEquals(a.summary.positionCount, b.summary.positionCount);
        assertEquals(a.positions.size(), b.positions.size());
    }

    @Test
    public void gainsLossesAliasMatchesUnrealizedPnl() {
        InvestmentService inv = new InvestmentService(new Account());
        inv.buy("Z", 1, 50.0);
        EvaluationController controller = controllerWithEmptyAsep(inv);
        EvaluationOverviewResponse a = (EvaluationOverviewResponse) controller.unrealizedPnLOverview();
        EvaluationOverviewResponse b = (EvaluationOverviewResponse) controller.absoluteAndPercentageGainsLossesOverview();
        assertEquals(a.summary.positionCount, b.summary.positionCount);
        assertEquals(a.positions.size(), b.positions.size());
    }

    @Test
    public void priorDayEodOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Object body = controller.priorDayEodOverview();
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }
}

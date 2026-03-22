package uk.co.asepstrath.bank.evaluation;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.value.Value;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EvaluationControllerTests {

    private static final String TEST_USER_ID = "test-user";

    private static Context ctxWithUserId(String userId) {
        Context ctx = mock(Context.class);
        Session session = mock(Session.class);
        Value sessionValue = mock(Value.class);
        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(sessionValue);
        when(sessionValue.value((String) null)).thenReturn(userId);
        return ctx;
    }

    private static EvaluationController controllerWithEmptyAsep(InvestmentService inv) {
        AsepPriceSource empty = ticker -> Optional.empty();
        PortfolioEvaluationService eval = new PortfolioEvaluationService(null, empty);
        EvaluationPortfolioLoader loader = mock(EvaluationPortfolioLoader.class);
        when(loader.loadForUserId(eq(TEST_USER_ID))).thenReturn(inv);
        return new EvaluationController(eval, loader);
    }

    @Test
    public void portfolioSnapshotDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.portfolioSnapshot(ctx);
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
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.portfolioPerformanceOverview(ctx);
        assertTrue(body instanceof PortfolioPerformanceOverviewResponse);
        PortfolioPerformanceOverviewResponse perf = (PortfolioPerformanceOverviewResponse) body;
        assertNotNull(perf.summary);
        PortfolioSnapshotResponse snap = (PortfolioSnapshotResponse) controller.portfolioSnapshot(ctx);
        assertEquals(snap.summary.positionCount, perf.summary.positionCount);
        assertEquals(snap.summary.totalBookValueAllPositions, perf.summary.totalBookValueAllPositions);
        assertEquals(snap.summary.cashBalance, perf.summary.cashBalance);
        assertEquals(snap.summary.totalPortfolioValue, perf.summary.totalPortfolioValue, 0.01);
    }

    @Test
    public void averageBookPriceOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.averageBookPriceOverview(ctx);
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
        Context ctx = ctxWithUserId(TEST_USER_ID);
        EvaluationOverviewResponse avg = (EvaluationOverviewResponse) controller.averageBookPriceOverview(ctx);
        EvaluationOverviewResponse cost = (EvaluationOverviewResponse) controller.costBasisOverview(ctx);
        assertEquals(avg.positions.size(), cost.positions.size());
        assertEquals(avg.positions.get(0).bookValue, cost.positions.get(0).bookValue);
    }

    @Test
    public void marketValueOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.marketValueOverview(ctx);
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void valueAtPriorCloseOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.valueAtPriorCloseOverview(ctx);
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void unrealizedPnLOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.unrealizedPnLOverview(ctx);
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
        Context ctx = ctxWithUserId(TEST_USER_ID);
        EvaluationOverviewResponse a = (EvaluationOverviewResponse) controller.unrealizedPnLOverview(ctx);
        EvaluationOverviewResponse b = (EvaluationOverviewResponse) controller.unrealizedGainsLossesOverview(ctx);
        assertEquals(a.summary.positionCount, b.summary.positionCount);
        assertEquals(a.positions.size(), b.positions.size());
    }

    @Test
    public void gainsLossesAliasMatchesUnrealizedPnl() {
        InvestmentService inv = new InvestmentService(new Account());
        inv.buy("Z", 1, 50.0);
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        EvaluationOverviewResponse a = (EvaluationOverviewResponse) controller.unrealizedPnLOverview(ctx);
        EvaluationOverviewResponse b = (EvaluationOverviewResponse) controller.absoluteAndPercentageGainsLossesOverview(ctx);
        assertEquals(a.summary.positionCount, b.summary.positionCount);
        assertEquals(a.positions.size(), b.positions.size());
    }

    @Test
    public void priorDayEodOverviewDelegatesToService() {
        InvestmentService inv = new InvestmentService(new Account());
        EvaluationController controller = controllerWithEmptyAsep(inv);
        Context ctx = ctxWithUserId(TEST_USER_ID);
        Object body = controller.priorDayEodOverview(ctx);
        assertTrue(body instanceof EvaluationOverviewResponse);
        EvaluationOverviewResponse resp = (EvaluationOverviewResponse) body;
        assertNotNull(resp.summary);
        assertNotNull(resp.positions);
    }

    @Test
    public void returnsUnauthorizedWhenNotLoggedIn() {
        PortfolioEvaluationService eval = new PortfolioEvaluationService(null, ticker -> Optional.empty());
        EvaluationPortfolioLoader loader = mock(EvaluationPortfolioLoader.class);
        EvaluationController controller = new EvaluationController(eval, loader);
        Context ctx = ctxWithUserId(null);
        Object body = controller.portfolioSnapshot(ctx);
        assertTrue(body instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, ?> map = (java.util.Map<String, ?>) body;
        assertEquals("Authentication required", map.get("error"));
        verify(loader, never()).loadForUserId(anyString());
    }
}

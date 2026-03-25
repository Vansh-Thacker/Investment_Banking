package uk.co.asepstrath.bank.capitalgains;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.Session;
import io.jooby.value.Value;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;
import uk.co.asepstrath.bank.investments.InvestmentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CapitalGainsControllerTest {

    private void mockUserSession(Context ctx, String userId) {
        Session session = mock(Session.class);
        Value idValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(idValue);
        when(idValue.value()).thenReturn(userId);
        when(idValue.value((String) null)).thenReturn(userId);
    }

    private void mockQueryYear(Context ctx) {
        Value q = mock(Value.class);
        when(ctx.query("year")).thenReturn(q);
        when(q.valueOrNull()).thenReturn(null);
    }

    @Test
    void testSummaryReturnsModelAndView() {
        Logger log = mock(Logger.class);
        UserService userService = mock(UserService.class);
        User user = mock(User.class);
        InvestmentService inv = mock(InvestmentService.class);

        when(userService.findById("123")).thenReturn(user);
        when(user.getInvestmentService()).thenReturn(inv);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "123");
        mockQueryYear(ctx);

        CapitalGainsController controller = new CapitalGainsController(log, userService);

        Object result = controller.summary(ctx);

        assertTrue(result instanceof ModelAndView);
    }

    @Test
    void testTransactionsReturnsModelAndView() {
        Logger log = mock(Logger.class);
        UserService userService = mock(UserService.class);
        User user = mock(User.class);
        InvestmentService inv = mock(InvestmentService.class);

        when(userService.findById("123")).thenReturn(user);
        when(user.getInvestmentService()).thenReturn(inv);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "123");
        mockQueryYear(ctx);

        CapitalGainsController controller = new CapitalGainsController(log, userService);

        Object result = controller.transactions(ctx);

        assertTrue(result instanceof ModelAndView);
    }
}
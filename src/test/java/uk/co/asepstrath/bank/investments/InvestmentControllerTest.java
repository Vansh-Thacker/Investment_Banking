package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.Session;
import io.jooby.StatusCode;
import io.jooby.value.Value;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

import javax.sql.DataSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InvestmentControllerTest {

    private InvestmentController newController(
            UserService users,
            PriceService priceService,
            EquitiesService equitiesService,
            CurrentPriceService currentPriceService,
            Account account
    ) {
        Logger log = mock(Logger.class);
        DataSource ds = mock(DataSource.class);

        InvestmentController c = new InvestmentController(
                ds,
                log,
                account,
                priceService,
                equitiesService,
                currentPriceService
        );
        c.setUserService(users);
        return c;
    }

    private void mockUserSession(Context ctx, String userId) {
        Session session = mock(Session.class);
        Value idValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(idValue);
        when(idValue.value()).thenReturn(userId);
        when(idValue.value((String) null)).thenReturn(userId);
    }

    private void mockNoUserSession(Context ctx) {
        Session session = mock(Session.class);
        Value idValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(idValue);
        when(idValue.value((String) null)).thenReturn(null);
    }

    /* ============================================================
       /positions
       ============================================================ */

    @Test
    void testPositionsSuccess() {
        UserService users = mock(UserService.class);
        PriceService priceService = mock(PriceService.class);
        EquitiesService equitiesService = mock(EquitiesService.class);
        CurrentPriceService currentPriceService = mock(CurrentPriceService.class);

        Account account = new Account();
        InvestmentController controller = newController(users, priceService, equitiesService, currentPriceService, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        User user = mock(User.class);
        InvestmentService inv = mock(InvestmentService.class);

        Position p = new Position();
        p.ticker = "ABC";
        p.shares = 5;

        when(users.findById("u1")).thenReturn(user);
        when(user.getInvestmentService()).thenReturn(inv);
        when(inv.getPositions()).thenReturn(List.of(p));
        when(equitiesService.getName("ABC")).thenReturn("ABC Corp");
        when(currentPriceService.getPricePence("ABC")).thenReturn(1234);

        Object result = controller.positions(ctx);
        assertTrue(result instanceof Collection);

        Position out = ((Collection<Position>) result).iterator().next();
        assertEquals("ABC Corp", out.stockName);
        assertEquals(12.34, out.currentPrice);
    }

    @Test
    void testPositionsUnauthorized() {
        UserService users = mock(UserService.class);
        InvestmentController controller = newController(users, null, null, null, new Account());

        Context ctx = mock(Context.class);
        mockNoUserSession(ctx);

        Object result = controller.positions(ctx);
        assertTrue(result instanceof Map);
        verify(ctx).setResponseCode(StatusCode.UNAUTHORIZED);
    }

    /* ============================================================
       /prices/{symbol}
       ============================================================ */

    @Test
    void testCurrentPriceSuccess() {
        UserService users = mock(UserService.class);
        PriceService priceService = mock(PriceService.class);

        PriceRecord rec = new PriceRecord();
        rec.price = 5050;
        rec.date = "2024-01-01";

        when(priceService.getPrice("ABC")).thenReturn(rec);

        InvestmentController controller = newController(users, priceService, null, null, new Account());
        Context ctx = mock(Context.class);

        Object result = controller.currentPrice("ABC", ctx);
        assertTrue(result instanceof CurrentPriceResponse);

        CurrentPriceResponse r = (CurrentPriceResponse) result;
        assertEquals("ABC", r.symbol);
        assertEquals(5050, r.price);
    }

    @Test
    void testCurrentPriceInvalidSymbol() {
        InvestmentController controller = newController(mock(UserService.class), mock(PriceService.class), null, null, new Account());
        Context ctx = mock(Context.class);

        Object result = controller.currentPrice("", ctx);
        verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
        assertTrue(result instanceof Map);
    }

    @Test
    void testCurrentPriceNotFound() {
        PriceService priceService = mock(PriceService.class);
        when(priceService.getPrice("ABC")).thenReturn(null);

        InvestmentController controller = newController(mock(UserService.class), priceService, null, null, new Account());
        Context ctx = mock(Context.class);

        Object result = controller.currentPrice("ABC", ctx);
        verify(ctx).setResponseCode(StatusCode.NOT_FOUND);
        assertTrue(result instanceof Map);
    }

    /* ============================================================
       /transactions
       ============================================================ */

    @Test
    void testTransactionsSuccess() {
        UserService users = mock(UserService.class);
        InvestmentService inv = mock(InvestmentService.class);

        User user = mock(User.class);
        when(users.findById("u1")).thenReturn(user);
        when(user.getInvestmentService()).thenReturn(inv);

        InvestmentTransaction tx = new InvestmentTransaction();
        tx.ticker = "ABC";

        when(inv.getHistorySorted(null)).thenReturn(List.of(tx));

        InvestmentController controller = newController(users, null, null, null, new Account());
        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Object result = controller.transactions(null, null, null, null, ctx);
        assertTrue(result instanceof List);
    }

    @Test
    void testTransactionsUnauthorized() {
        InvestmentController controller = newController(mock(UserService.class), null, null, null, new Account());
        Context ctx = mock(Context.class);
        mockNoUserSession(ctx);

        Object result = controller.transactions(null, null, null, null, ctx);
        verify(ctx).setResponseCode(StatusCode.UNAUTHORIZED);
    }

    /* ============================================================
       BUY
       ============================================================ */

    @Test
    void testBuySuccess() {
        UserService users = mock(UserService.class);
        CurrentPriceService cps = mock(CurrentPriceService.class);

        when(cps.getPricePence("ABC")).thenReturn(1000);

        Account account = new Account();
        account.deposit(5000);

        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getAccount()).thenReturn(account);
        when(user.getInvestmentService()).thenReturn(inv);

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, cps, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value tickerVal = mock(Value.class);
        Value sharesVal = mock(Value.class);

        when(ctx.form("ticker")).thenReturn(tickerVal);
        when(tickerVal.value()).thenReturn("ABC");

        when(ctx.form("shares")).thenReturn(sharesVal);
        when(sharesVal.intValue()).thenReturn(3);

        controller.buy(ctx);

        verify(inv).newTransaction("ABC", 3, 10.0);
        verify(ctx).sendRedirect("/investments/investmentsfrontend");
    }

    @Test
    void testBuyInvalidTicker() {
        InvestmentController controller = newController(mock(UserService.class), null, null, mock(CurrentPriceService.class), new Account());
        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value tickerVal = mock(Value.class);
        Value sharesVal = mock(Value.class);

        when(ctx.form("ticker")).thenReturn(tickerVal);
        when(tickerVal.value()).thenReturn("###");

        when(ctx.form("shares")).thenReturn(sharesVal);
        when(sharesVal.intValue()).thenReturn(1);

        controller.buy(ctx);
        verify(ctx).sendRedirect(contains("error=Invalid"));
    }

    /* ============================================================
       SELL
       ============================================================ */

    @Test
    void testSellSuccess() {
        UserService users = mock(UserService.class);
        CurrentPriceService cps = mock(CurrentPriceService.class);

        when(cps.getPricePence("ABC")).thenReturn(2000);

        Account account = new Account();
        account.deposit(10000);

        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getAccount()).thenReturn(account);
        when(user.getInvestmentService()).thenReturn(inv);

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, cps, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value tickerVal = mock(Value.class);
        Value sharesVal = mock(Value.class);

        when(ctx.form("ticker")).thenReturn(tickerVal);
        when(tickerVal.value()).thenReturn("ABC");

        when(ctx.form("shares")).thenReturn(sharesVal);
        when(sharesVal.intValue()).thenReturn(2);

        controller.sell(ctx);

        verify(inv).sell("ABC", 2, 20.0);
        verify(ctx).sendRedirect("/investments/investmentsfrontend");
    }

    @Test
    void testSellInvalidTicker() {
        InvestmentController controller = newController(mock(UserService.class), null, null, mock(CurrentPriceService.class), new Account());
        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value tickerVal = mock(Value.class);
        Value sharesVal = mock(Value.class);

        when(ctx.form("ticker")).thenReturn(tickerVal);
        when(tickerVal.value()).thenReturn("###");

        when(ctx.form("shares")).thenReturn(sharesVal);
        when(sharesVal.intValue()).thenReturn(1);

        controller.sell(ctx);
        verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    }

    /* ============================================================
       FRONTEND PAGES
       ============================================================ */

    @Test
    void testInvestmentsFrontend() {
        UserService users = mock(UserService.class);
        CurrentPriceService cps = mock(CurrentPriceService.class);

        Account account = new Account();
        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getInvestmentService()).thenReturn(inv);
        when(user.getName()).thenReturn("Test User");
        when(inv.getPositions()).thenReturn(new ArrayList<>());

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, cps, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value errorVal = mock(Value.class);
        when(ctx.query("error")).thenReturn(errorVal);
        when(errorVal.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.showInvestments(ctx);

        assertEquals("investments.hbs", mv.getView());
        Map<String, Object> model = (Map<String, Object>) mv.getModel();
        assertTrue(model.containsKey("positions"));
        assertTrue(model.containsKey("cashBalance"));
        assertTrue(model.containsKey("name"));
    }

    @Test
    void testInvestmentsFrontendUnauthorized() {
        InvestmentController controller = newController(mock(UserService.class), null, null, null, new Account());
        Context ctx = mock(Context.class);
        mockNoUserSession(ctx);

        controller.showInvestments(ctx);
        verify(ctx).sendRedirect("/login");
    }

    @Test
    void testHistoryFrontend() {
        UserService users = mock(UserService.class);
        Account account = new Account();
        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getInvestmentService()).thenReturn(inv);
        when(user.getName()).thenReturn("Test User");
        when(inv.getHistorySorted("desc")).thenReturn(new ArrayList<>());

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, null, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value errorVal = mock(Value.class);
        when(ctx.query("error")).thenReturn(errorVal);
        when(errorVal.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.showInvestmentHistory(ctx);

        assertEquals("investment-history.hbs", mv.getView());
    }

    @Test
    void testHistoryFrontendUnauthorized() {
        InvestmentController controller = newController(mock(UserService.class), null, null, null, new Account());
        Context ctx = mock(Context.class);
        mockNoUserSession(ctx);

        controller.showInvestmentHistory(ctx);
        verify(ctx).sendRedirect("/login");
    }

    @Test
    void testBuyPage() {
        UserService users = mock(UserService.class);
        CurrentPriceService cps = mock(CurrentPriceService.class);

        when(cps.getPricePence(anyString())).thenReturn(1000);

        Account account = new Account();
        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getInvestmentService()).thenReturn(inv);
        when(user.getName()).thenReturn("Test User");
        when(inv.getPositions()).thenReturn(new ArrayList<>());
        when(inv.getCashBalance()).thenReturn(5000);

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, cps, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        Value errorVal = mock(Value.class);
        when(ctx.query("error")).thenReturn(errorVal);
        when(errorVal.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.buyPage(ctx);

        assertEquals("buy.hbs", mv.getView());
        Map<String, Object> model = (Map<String, Object>) mv.getModel();
        assertTrue(model.containsKey("tickerOptions"));
    }

    @Test
    void testSellPage() {
        UserService users = mock(UserService.class);
        Account account = new Account();
        InvestmentService inv = mock(InvestmentService.class);
        User user = mock(User.class);

        when(user.getInvestmentService()).thenReturn(inv);
        when(inv.getPositions()).thenReturn(new ArrayList<>());

        when(users.findById("u1")).thenReturn(user);

        InvestmentController controller = newController(users, null, null, null, account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "u1");

        ModelAndView mv = controller.sellPage(ctx);

        assertEquals("sell.hbs", mv.getView());
    }
}
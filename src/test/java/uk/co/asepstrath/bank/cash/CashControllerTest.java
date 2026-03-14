package uk.co.asepstrath.bank.cash;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.Session;
import io.jooby.value.Value;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.user.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class CashControllerTest {

    private CashController newController(Account account) throws Exception {
        Logger log = mock(Logger.class);

        // Create a temporary CSV file for the injected CashService
        Path temp = Files.createTempFile("cash", ".csv");
        CashService service = new CashService(account, temp.toString());

        // Controller uses this injected service in TEST MODE
        return new CashController(log, account, service);
    }

    private void mockUserSession(Context ctx, String username) {
        Session session = mock(Session.class);
        Value usernameValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("user")).thenReturn(usernameValue);
        when(usernameValue.value()).thenReturn(username);
        when(usernameValue.value((String) null)).thenReturn(username);
    }

    @Test
    void testIndexRedirect() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);

        controller.index(ctx);

        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testDepositPostSuccess() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);

        // REMOVE setUserService()

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        CashRequest req = new CashRequest();
        req.amount = 500;

        controller.deposit(req, ctx);

        assertEquals(500, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testDepositPostFailure() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        CashRequest req = new CashRequest();
        req.amount = -10;

        controller.deposit(req, ctx);

        verify(ctx).sendRedirect(contains("error=Deposit+must+be+positive"));
        assertEquals(0, account.getBalance());
    }

    @Test
    void testDepositGetSuccess() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);

        // REMOVE setUserService()

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        controller.depositGet(200, ctx);

        assertEquals(200, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testDepositGetFailure() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        controller.depositGet(-5, ctx);

        verify(ctx).sendRedirect(contains("error=Deposit+must+be+positive"));
        assertEquals(0, account.getBalance());
    }

    @Test
    void testWithdrawPostSuccess() throws Exception {
        Account account = new Account();
        account.deposit(1000);

        CashController controller = newController(account);

        // REMOVE setUserService()

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        CashRequest req = new CashRequest();
        req.amount = 300;

        controller.withdraw(req, ctx);

        assertEquals(700, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testWithdrawPostFailure() throws Exception {
        Account account = new Account();
        account.deposit(100);

        CashController controller = newController(account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        CashRequest req = new CashRequest();
        req.amount = 500;

        controller.withdraw(req, ctx);

        verify(ctx).sendRedirect(contains("error=Invalid+withdrawal+amount"));
        assertEquals(100, account.getBalance());
    }

    @Test
    void testWithdrawGetSuccess() throws Exception {
        Account account = new Account();
        account.deposit(500);

        CashController controller = newController(account);

        // REMOVE setUserService()

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        controller.withdrawGet(200, ctx);

        assertEquals(300, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testWithdrawGetFailure() throws Exception {
        Account account = new Account();
        account.deposit(100);

        CashController controller = newController(account);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        controller.withdrawGet(500, ctx);

        verify(ctx).sendRedirect(contains("error=Invalid+withdrawal+amount"));
        assertEquals(100, account.getBalance());
    }

    @Test
    void testDashboardModel() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);

        UserService users = mock(UserService.class);
        controller.setUserService(users);

        when(users.getBalance("testuser")).thenReturn(500);

        Context ctx = mock(Context.class);
        mockUserSession(ctx, "testuser");

        Value errorValue = mock(Value.class);
        when(ctx.query("error")).thenReturn(errorValue);
        when(errorValue.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.showDashboard(ctx);

        assertEquals("cash.hbs", mv.getView());

        Map<String, Object> model = (Map<String, Object>) mv.getModel();

        assertTrue(model.containsKey("balance"));
        assertTrue(model.containsKey("history"));
        assertTrue(model.containsKey("name"));
        assertTrue(model.containsKey("error"));
    }
}
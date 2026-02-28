package uk.co.asepstrath.bank.cash;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.value.Value;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class CashControllerTest {
    private CashController newController(Account account) throws Exception {
        Logger log = Mockito.mock(Logger.class);

        Path temp = Files.createTempFile("cash", ".csv");
        CashService service = new CashService(account, temp.toString());

        return new CashController(log, account, service);
    }

    @Test
    void testIndexRedirect() throws Exception { // does the /cash route work right
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.index(ctx);
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testDepositPostSuccess() throws Exception { // does the POST endpoint work correct
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        CashRequest req = new CashRequest();
        req.amount = 500;
        controller.deposit(req, ctx);
        assertEquals(500, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testDepositPostFailure() throws Exception { //does the deposit fail correctly
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        CashRequest req = new CashRequest();
        req.amount = -10;
        controller.deposit(req, ctx);
        verify(ctx).sendRedirect(contains("error=Deposit+must+be+positive"));
        assertEquals(0, account.getBalance());
    }

    @Test
    void testDepositGetSuccess() throws Exception { //does the GET deposit work correctly
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.depositGet(200, ctx);
        assertEquals(200, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }


    @Test
    void testDepositGetFailure() throws Exception { // does GET deposit validate input correctly
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.depositGet(-5, ctx);
        verify(ctx).sendRedirect(contains("error=Deposit+must+be+positive"));
        assertEquals(0, account.getBalance());
    }

    @Test
    void testWithdrawPostSuccess() throws Exception { // does POST withdrawal work correct
        Account account = new Account();
        account.deposit(1000);
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        CashRequest req = new CashRequest();
        req.amount = 300;
        controller.withdraw(req, ctx);
        assertEquals(700, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }


    @Test
    void testWithdrawPostFailure() throws Exception { // is overdraft blocked
        Account account = new Account();
        account.deposit(100);
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        CashRequest req = new CashRequest();
        req.amount = 500;
        controller.withdraw(req, ctx);
        verify(ctx).sendRedirect(contains("error=Invalid+withdrawal+amount"));
        assertEquals(100, account.getBalance());
    }

    @Test
    void testWithdrawGetSuccess() throws Exception { // does the GET withdrawal work correct
        Account account = new Account();
        account.deposit(500);
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.withdrawGet(200, ctx);
        assertEquals(300, account.getBalance());
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testWithdrawGetFailure() throws Exception { //does get withdraw block invalid amounts
        Account account = new Account();
        account.deposit(100);
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.withdrawGet(500, ctx);
        verify(ctx).sendRedirect(contains("error=Invalid+withdrawal+amount"));
        assertEquals(100, account.getBalance());
    }

    @Test
    void testBalanceEndpoint() throws Exception { // does the balance endpoint return the correct integer
        Account account = new Account();
        account.deposit(1234);
        CashController controller = newController(account);
        assertEquals(1234, controller.balance());
    }

    @Test
    void testTransactionsEndpoint() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);
        controller.getService().deposit(100);
        controller.getService().withdraw(50);
        Object result = controller.transactions();
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
    }

    @Test
    void testDashboardModel() throws Exception {
        Account account = new Account();
        CashController controller = newController(account);
        controller.getService().deposit(500);
        Context ctx = mock(Context.class);
        Value value = mock(Value.class);
        when(ctx.query("error")).thenReturn(value);
        when(value.value((String) null)).thenReturn(null);
        ModelAndView mv = controller.showDashboard(ctx);
        assertEquals("cash.hbs", mv.getView());
        Map<String, Object> model = (Map<String, Object>) mv.getModel();
        assertTrue(model.containsKey("balance"));
        assertTrue(model.containsKey("history"));
        assertTrue(model.containsKey("name"));
        assertTrue(model.containsKey("error"));
    }


}

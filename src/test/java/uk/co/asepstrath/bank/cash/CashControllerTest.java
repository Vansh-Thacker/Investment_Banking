package uk.co.asepstrath.bank.cash;

import io.jooby.Context;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;

import java.nio.file.Files;
import java.nio.file.Path;

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

}

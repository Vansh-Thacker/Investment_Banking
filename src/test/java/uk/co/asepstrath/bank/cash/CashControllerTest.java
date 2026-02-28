package uk.co.asepstrath.bank.cash;

import io.jooby.Context;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CashControllerTest {
    private CashController newController(Account account) {
        Logger log = Mockito.mock(Logger.class);
        return new CashController(log, account);
    }
    @Test
    void testIndexRedirect() { // does the /cash path behave correctly
        Account account = new Account();
        CashController controller = newController(account);
        Context ctx = mock(Context.class);
        controller.index(ctx);
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }
        @Test
        void testDepositPostSuccess() {
            Account account = new Account();
            CashController controller = newController(account);
            Context ctx = mock(Context.class);
            CashRequest req = new CashRequest();
            req.amount = 500;
            controller.deposit(req, ctx);
            assertEquals(500, account.getBalance());
            verify(ctx).sendRedirect("/cash/cashsidefrontend");
        }

}

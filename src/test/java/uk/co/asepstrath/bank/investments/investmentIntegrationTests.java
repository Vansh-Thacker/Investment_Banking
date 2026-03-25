package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class investmentIntegrationTests {
    @Test
    void testInvestmentFlow() {
        // 1. Prepare dependencies
        Account acc = new Account();
        acc.deposit(1000); // Start with £10
        Logger mockLogger = mock(Logger.class);
        DataSource mockDs = mock(DataSource.class);

        // FIX 1: Provide 6 arguments to the constructor
        // Based on typical Jooby/Investment setups, these are likely:
        // (DataSource, Logger, Account, Service, ..., ...)
        // If your 6 args are different, update these placeholders:
        InvestmentController controller = new InvestmentController(
                mockDs,       // 1
                mockLogger,   // 2
                acc,          // 3
                null,         // 4 (Placeholder)
                null,         // 5 (Placeholder)
                null          // 6 (Placeholder)
        );

        // 2. Prepare the Request Data
        TradeRequest req = new TradeRequest();
        req.ticker = "AAPL";
        req.shares = 2;
        req.price = 100.0;

        // 3. Mock the Context
        // FIX 2: controller.sell requires Context, not TradeRequest
        Context mockCtx = mock(Context.class);
        // Tell Jooby to return our TradeRequest when the controller calls ctx.body(TradeRequest.class)
        when(mockCtx.body(TradeRequest.class)).thenReturn(req);

        // 4. Execute
        controller.sell(mockCtx);

        // 5. Assert (2 shares * £1.00 = 200 pence proceeds)
        // 1000 (start) + 200 (sale) = 1200
        assertEquals(1200, acc.getBalance());
    }
}
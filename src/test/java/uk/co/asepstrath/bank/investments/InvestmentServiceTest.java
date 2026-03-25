package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.CurrentPriceLoader;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;
import uk.co.asepstrath.bank.evaluation.PriceLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class InvestmentServiceTest {

    private InvestmentService newService(Account account, String csvPath) {
        CurrentPriceLoader loader = mock(CurrentPriceLoader.class);
        CurrentPriceService cps = new CurrentPriceService(loader);
        return new InvestmentService(account, cps, csvPath);
    }

    /* ============================================================
       BUY
       ============================================================ */

    @Test
    void testBuySuccess() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(10000);

        InvestmentService service = newService(account, temp.toString());

        PurchaseResult result = service.newTransaction("ABC", 5, 10.0);

        assertTrue(result.success);
        assertEquals(5000, account.getBalance());
        assertEquals(5, service.getPosition("ABC").shares);
        assertEquals(1000, service.getPosition("ABC").averageBookPrice);
    }

    @Test
    void testBuyInsufficientFunds() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(1000);

        InvestmentService service = newService(account, temp.toString());

        assertThrows(IllegalArgumentException.class,
                () -> service.newTransaction("ABC", 5, 10.0));
    }

    /* ============================================================
       SELL
       ============================================================ */

    @Test
    void testSellSuccess() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(10000);

        InvestmentService service = newService(account, temp.toString());

        service.newTransaction("ABC", 10, 10.0); // cost 10000
        SellConfirmation conf = service.sell("ABC", 4, 20.0);

        assertEquals(6, service.getPosition("ABC").shares);
        assertEquals(20.0, conf.finalSalePrice);
        assertEquals(8000, account.getBalance()); // 10000 - 10000 + 8000
    }

    @Test
    void testSellAllSharesRemovesPosition() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(5000);

        InvestmentService service = newService(account, temp.toString());

        service.newTransaction("XYZ", 5, 10.0);
        service.sell("XYZ", 5, 10.0);

        assertNull(service.getPosition("XYZ"));
    }

    @Test
    void testSellNotEnoughShares() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(5000);

        InvestmentService service = newService(account, temp.toString());

        service.newTransaction("XYZ", 3, 10.0);

        assertThrows(IllegalArgumentException.class,
                () -> service.sell("XYZ", 5, 10.0));
    }

    /* ============================================================
       SALE PREVIEW
       ============================================================ */

    @Test
    void testSalePreviewSuccess() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(20000); // FIX: enough to buy 10 shares at £10

        InvestmentService service = newService(account, temp.toString());

        service.newTransaction("ABC", 10, 10.0);

        SalePreview preview = service.getSalePreview("ABC", 5, 12.0);

        assertEquals("ABC", preview.name);
        assertEquals(5, preview.shares);
        assertEquals(50.0, preview.estimatedPricePerShare);
    }

    @Test
    void testSalePreviewNotEnoughShares() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(5000);

        InvestmentService service = newService(account, temp.toString());

        service.newTransaction("ABC", 2, 10.0);

        assertThrows(IllegalArgumentException.class,
                () -> service.getSalePreview("ABC", 5, 12.0));
    }

    /* ============================================================
       CSV IMPORT
       ============================================================ */

    @Test
    void testCsvImportBasic() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");

        String csv = """
                date,type,ticker,amount,balanceAfter
                2024-01-01T10:00,BUY,ABC,1000,0
                2024-01-02T10:00,BUY,ABC,1000,0
                """;

        Files.writeString(temp, csv);

        Account account = new Account();
        CurrentPriceLoader loader = mock(CurrentPriceLoader.class);
        CurrentPriceService cps = new CurrentPriceService(loader);

        PriceLoader priceLoader = mock(PriceLoader.class);
        when(priceLoader.getPrice(eq("ABC"), any())).thenReturn(10.0);

        InvestmentService service = new InvestmentService(account, cps, temp.toString());
        service.importCsv(Files.newInputStream(temp), priceLoader);

        Position p = service.getPosition("ABC");
        assertNotNull(p);
        assertEquals(2, p.shares);
        assertEquals(2000, p.totalInvested);
    }

    /* ============================================================
       HISTORY SORTING
       ============================================================ */

    @Test
    void testHistorySorting() throws Exception {
        Path temp = Files.createTempFile("inv", ".csv");
        Account account = new Account();
        account.deposit(10000);

        InvestmentService service = newService(account, temp.toString());

        // Create transactions with different timestamps
        InvestmentTransaction t1 = new InvestmentTransaction();
        t1.timestamp = LocalDateTime.now().minusDays(1);
        t1.type = "BUY";
        t1.ticker = "A";
        t1.shares = 1;
        t1.price = 1.0;

        InvestmentTransaction t2 = new InvestmentTransaction();
        t2.timestamp = LocalDateTime.now();
        t2.type = "BUY";
        t2.ticker = "B";
        t2.shares = 1;
        t2.price = 1.0;

        service.getHistory().add(t1);
        service.getHistory().add(t2);

        List<InvestmentTransaction> desc = service.getHistorySorted("desc");
        List<InvestmentTransaction> asc = service.getHistorySorted("asc");

        assertEquals("B", desc.get(0).ticker);
        assertEquals("A", asc.get(0).ticker);
    }
}
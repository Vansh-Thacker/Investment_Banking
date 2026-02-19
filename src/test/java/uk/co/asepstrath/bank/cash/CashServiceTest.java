package uk.co.asepstrath.bank.cash;
import uk.co.asepstrath.bank.Account;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CashServiceTest {
   @Test // Does deposit update balance and the history?
    void testDeposit() throws Exception {
        Path temp = Files.createTempFile("cash", ".csv");
        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        service.deposit(500); // 5 pounds

        assertEquals(500, service.getBalance());
        assertEquals(1, service.getHistory().size());
        assertEquals("DEPOSIT", service.getHistory().get(0).getType());
    }

    @Test //does withdraw update balance and history?
    void testWithdraw() throws Exception {
        Path temp = Files.createTempFile("cash", ".csv");
        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        service.deposit(1000);
        service.withdraw(400);

        assertEquals(600, service.getBalance());
        assertEquals(2, service.getHistory().size());
        assertEquals("WITHDRAWAL", service.getHistory().get(1).getType());
    }
    @Test //does withdraw fail when you give insufficient funds
    void testWithdrawFails() throws Exception {
        Path temp = Files.createTempFile("cash", ".csv");
        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        service.deposit(300);

        assertThrows(IllegalArgumentException.class, () -> {
            service.withdraw(500);
        });

        assertEquals(300, service.getBalance());
        assertEquals(1, service.getHistory().size());
    }

    @Test //does CSV file writing work
    void testCsvWriting() throws Exception {
        Path temp = Files.createTempFile("cash", ".csv");
        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        service.deposit(1000);

        List<String> lines = Files.readAllLines(temp);

        assertEquals(2, lines.size()); // header + 1 row
        assertTrue(lines.get(1).contains("DEPOSIT"));
        assertTrue(lines.get(1).contains("10.00")); // £10.00
    }

    @Test //does csv loading rebuild the balance
    void testCsvLoading() throws Exception {
        String csv = """
            timestamp,type,amount
            2024-01-01T10:00:00,DEPOSIT,10.00
            2024-01-02T10:00:00,WITHDRAWAL,5.00
            """;

        Path temp = Files.createTempFile("cash", ".csv");
        Files.writeString(temp, csv);

        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        assertEquals(500, service.getBalance()); // 10.00 - 5.00 = 5.00 → 500 pence
        assertEquals(2, service.getHistory().size());
    }

    @Test // does the csv loading sort correctly
    void testCsvSorting() throws Exception {
        String csv = """
            timestamp,type,amount
            2024-01-03T10:00:00,DEPOSIT,5.00
            2024-01-01T10:00:00,DEPOSIT,10.00
            2024-01-02T10:00:00,WITHDRAWAL,3.00
            """;

        Path temp = Files.createTempFile("cash", ".csv");
        Files.writeString(temp, csv);

        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        List<CashTransaction> history = service.getHistory();

        assertEquals("2024-01-01T10:00", history.get(0).getDateTime().toString().substring(0, 16));
        assertEquals("2024-01-02T10:00", history.get(1).getDateTime().toString().substring(0, 16));
        assertEquals("2024-01-03T10:00", history.get(2).getDateTime().toString().substring(0, 16));
    }

    @Test //is the integer of pence correclty stored in the csv file as a double
    void testPenceConversion() throws Exception {
        String csv = """
            timestamp,type,amount
            2024-01-01T10:00:00,DEPOSIT,12.34
            """;

        Path temp = Files.createTempFile("cash", ".csv");
        Files.writeString(temp, csv);

        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        assertEquals(1234, service.getBalance());
    }
    @Test // is the exact balance getting withdrawn?
    void testWithdrawExactBalance() throws Exception {
        Path temp = Files.createTempFile("cash", ".csv");
        Account account = new Account();
        CashService service = new CashService(account, temp.toString());

        service.deposit(1000);
        service.withdraw(1000);

        assertEquals(0, service.getBalance());
    }


}

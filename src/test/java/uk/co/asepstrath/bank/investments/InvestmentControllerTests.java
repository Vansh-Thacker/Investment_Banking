package uk.co.asepstrath.bank.investments;

import io.jooby.test.MockRouter;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.App;
import io.jooby.StatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InvestmentControllerTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(100000);
        service = new InvestmentService(account);
    }

    // ===== Tests for transactions endpoint with no filter =====

    @Test
    public void transactionsReturnsAllTransactionsWhenNoFilter() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> result = service.getHistory();

        assertEquals(3, result.size());
    }

    @Test
    public void transactionsReturnsEmptyListWhenNoTransactions() {
        List<InvestmentTransaction> result = service.getHistory();

        assertEquals(0, result.size());
    }

    // ===== Tests for transactions endpoint with purchases filter =====

    @Test
    public void transactionsWithPurchasesFilterReturnsBuyTransactionsOnly() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(tx -> "BUY".equals(tx.type)));
    }

    @Test
    public void transactionsWithPurchasesFilterExcludesSellTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 10, 155.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(2, result.size());
        for (InvestmentTransaction tx : result) {
            assertNotEquals("SELL", tx.type);
        }
    }

    @Test
    public void transactionsWithPurchasesFilterEmptyWhenNoPurchases() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 10, 155.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(0, result.size());
    }

    // ===== Tests for transactions endpoint with sort parameter =====

    @Test
    public void transactionsWithSortAscReturnsOldestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals("AAPL", result.get(0).ticker);
        assertEquals("GOOGL", result.get(1).ticker);
        assertEquals("MSFT", result.get(2).ticker);
    }

    @Test
    public void transactionsWithSortDescReturnsNewestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory("desc");

        assertEquals("MSFT", result.get(0).ticker);
        assertEquals("GOOGL", result.get(1).ticker);
        assertEquals("AAPL", result.get(2).ticker);
    }

    @Test
    public void transactionsDefaultSortIsNewestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> resultNoSort = service.getPurchaseHistory(null);
        List<InvestmentTransaction> resultDesc = service.getPurchaseHistory("desc");

        assertEquals(resultDesc.get(0).ticker, resultNoSort.get(0).ticker);
        assertEquals(resultDesc.get(1).ticker, resultNoSort.get(1).ticker);
    }

    // ===== Tests for combined filter and sort =====

    @Test
    public void transactionsWithPurchasesFilterAndSortAscReturnsBuyTransactionsOldestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.sell("GOOGL", 3, 2850.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(3, result.size());
        assertEquals("AAPL", result.get(0).ticker);
        assertEquals("GOOGL", result.get(1).ticker);
        assertEquals("MSFT", result.get(2).ticker);
        assertTrue(result.stream().allMatch(tx -> "BUY".equals(tx.type)));
    }

    @Test
    public void transactionsWithPurchasesFilterAndSortDescReturnsBuyTransactionsNewestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.sell("AAPL", 5, 155.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory("desc");

        assertEquals(2, result.size());
        assertEquals("GOOGL", result.get(0).ticker);
        assertEquals("AAPL", result.get(1).ticker);
        assertTrue(result.stream().allMatch(tx -> "BUY".equals(tx.type)));
    }

    // ===== Tests for purchase history scrollability (data integrity) =====

    @Test
    public void multipleTransactionsAreScrollable() {
        for (int i = 0; i < 50; i++) {
            service.buy("TICK" + i, 10, 150.0 + i);
        }

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(50, result.size());
        assertEquals("TICK0", result.get(0).ticker);
        assertEquals("TICK49", result.get(49).ticker);
    }

}

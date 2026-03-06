package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InvestmentServiceTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(100000); // Start with sufficient funds
        service = new InvestmentService(account);
    }

    // ===== Tests for buy() method =====

    @Test
    public void buyRecordsTransactionInHistory() {
        service.buy("AAPL", 10, 150.0);
        List<InvestmentTransaction> history = service.getHistory();

        assertEquals(1, history.size());
        InvestmentTransaction tx = history.get(0);
        assertEquals("BUY", tx.type);
        assertEquals("AAPL", tx.ticker);
        assertEquals(10, tx.shares);
        assertEquals(150.0, tx.price);
    }

    @Test
    public void buyWithZeroSharesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 0, 150.0));
    }

    @Test
    public void buyWithNegativeSharesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", -5, 150.0));
    }

    @Test
    public void buyWithZeroPriceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 10, 0.0));
    }

    @Test
    public void buyWithNegativePriceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 10, -150.0));
    }

    @Test
    public void multipleTransactionsRecordedInOrder() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> history = service.getHistory();
        assertEquals(3, history.size());
        assertEquals("AAPL", history.get(0).ticker);
        assertEquals("GOOGL", history.get(1).ticker);
        assertEquals("MSFT", history.get(2).ticker);
    }

    // ===== Tests for getPurchaseHistory() filtering =====

    @Test
    public void getPurchaseHistoryFiltersOnlyBuyTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);

        assertEquals(3, purchases.size());
        for (InvestmentTransaction tx : purchases) {
            assertEquals("BUY", tx.type);
        }
    }

    @Test
    public void getPurchaseHistoryExcludesSellTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);

        assertEquals(2, purchases.size());
        assertTrue(purchases.stream().allMatch(tx -> "BUY".equals(tx.type)));
        assertTrue(purchases.stream().noneMatch(tx -> "SELL".equals(tx.type)));
    }

    @Test
    public void getPurchaseHistoryEmptyWhenNoTransactions() {
        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);
        assertEquals(0, purchases.size());
    }

    @Test
    public void getPurchaseHistoryEmptyWhenOnlySellingTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 10, 155.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);
        assertEquals(0, purchases.size());
    }

    // ===== Tests for getPurchaseHistory() sorting =====

    @Test
    public void getPurchaseHistorySortsNewestFirstByDefault() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10); // Ensure different timestamps
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);

        assertEquals(3, purchases.size());
        assertEquals("MSFT", purchases.get(0).ticker);
        assertEquals("GOOGL", purchases.get(1).ticker);
        assertEquals("AAPL", purchases.get(2).ticker);
    }

    @Test
    public void getPurchaseHistorySortsNewestFirstWithDescOrder() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("desc");

        assertEquals(3, purchases.size());
        assertEquals("MSFT", purchases.get(0).ticker);
        assertEquals("GOOGL", purchases.get(1).ticker);
        assertEquals("AAPL", purchases.get(2).ticker);
    }

    @Test
    public void getPurchaseHistorySortsOldestFirstWithAscOrder() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(3, purchases.size());
        assertEquals("AAPL", purchases.get(0).ticker);
        assertEquals("GOOGL", purchases.get(1).ticker);
        assertEquals("MSFT", purchases.get(2).ticker);
    }

    @Test
    public void getPurchaseHistorySortsOldestFirstWithAscOrderCaseInsensitive() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("ASC");

        assertEquals(2, purchases.size());
        assertEquals("AAPL", purchases.get(0).ticker);
        assertEquals("GOOGL", purchases.get(1).ticker);
    }

    @Test
    public void getPurchaseHistoryMaintainsTimestampOnEachTransaction() {
        service.buy("AAPL", 10, 150.0);
        LocalDateTime beforeSecondBuy = LocalDateTime.now();
        service.buy("GOOGL", 5, 2800.0);
        LocalDateTime afterSecondBuy = LocalDateTime.now();

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(2, purchases.size());
        assertNotNull(purchases.get(0).timestamp);
        assertNotNull(purchases.get(1).timestamp);
        assertTrue(purchases.get(0).timestamp.isBefore(purchases.get(1).timestamp));
    }

    // ===== Tests for combined filtering and sorting =====

    @Test
    public void getPurchaseHistoryFiltersSortsBuyTransactionsCorrectly() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(10);
        service.sell("AAPL", 5, 155.0);
        Thread.sleep(10);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(3, purchases.size());
        assertEquals("BUY", purchases.get(0).type);
        assertEquals("BUY", purchases.get(1).type);
        assertEquals("BUY", purchases.get(2).type);
        assertEquals("AAPL", purchases.get(0).ticker);
        assertEquals("GOOGL", purchases.get(1).ticker);
        assertEquals("MSFT", purchases.get(2).ticker);
    }

    // ===== Tests for getHistory() =====

    @Test
    public void getHistoryIncludesAllTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 5, 155.0);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> history = service.getHistory();

        assertEquals(3, history.size());
    }

    @Test
    public void getHistoryIncludesBothBuyAndSellTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> history = service.getHistory();

        long buyCount = history.stream().filter(tx -> "BUY".equals(tx.type)).count();
        long sellCount = history.stream().filter(tx -> "SELL".equals(tx.type)).count();

        assertEquals(1, buyCount);
        assertEquals(1, sellCount);
    }

    // ===== Story 4: Sale execution and holdings update =====

    @Test
    public void buyDecreasesAccountBalanceAndUpdatesPosition() {
        int startingBalance = account.getBalance();
        service.buy("AAPL", 10, 100.0); // cost 1000
        assertEquals(startingBalance - 1000, account.getBalance());
        Position pos = service.getPosition("AAPL");
        assertNotNull(pos);
        assertEquals(10, pos.shares);
    }

    @Test
    public void sellIncreasesAccountBalanceAndDecreasesPosition() {
        account.deposit(5000);
        service.buy("AAPL", 20, 50.0); // cost 1000
        int balanceAfterBuy = account.getBalance();

        SellConfirmation conf = service.sell("AAPL", 5, 60.0); // proceeds 300
        assertEquals(balanceAfterBuy + 300, account.getBalance());

        Position pos = service.getPosition("AAPL");
        assertNotNull(pos);
        assertEquals(15, pos.shares);
    }

    @Test
    public void sellingAllSharesClosesPosition() {
        service.buy("GOOGL", 5, 200.0);
        assertNotNull(service.getPosition("GOOGL"));

        service.sell("GOOGL", 5, 210.0);
        assertNull(service.getPosition("GOOGL"));
    }

    @Test
    public void saleTransactionRecordedWithTimestampPriceAndProceeds() {
        service.buy("MSFT", 10, 100.0);
        SellConfirmation conf = service.sell("MSFT", 4, 110.0);

        // last history item should be the sale
        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction last = history.get(history.size() - 1);
        assertEquals("SELL", last.type);
        assertNotNull(last.timestamp);
        assertEquals(4, last.shares);
        assertEquals(110.0, last.price);
        assertEquals(4 * 110.0, last.proceeds);
    }

    @Test
    public void sellWithInsufficientHoldingsProcessesAnyway() {
        account.deposit(5000);
        service.buy("AAPL", 2, 100.0);
        int balanceBefore = account.getBalance();

        SellConfirmation confirmation = service.sell("AAPL", 5, 105.0);

        assertNotNull(confirmation);
        assertEquals(5, confirmation.shares);
        assertEquals(105.0, confirmation.finalSalePrice);
        assertEquals(balanceBefore + (int) Math.round(5 * 105.0), account.getBalance());
        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction last = history.get(history.size() - 1);
        assertEquals("SELL", last.type);
        assertEquals(5, last.shares);
        assertNull(service.getPosition("AAPL"));
    }

}

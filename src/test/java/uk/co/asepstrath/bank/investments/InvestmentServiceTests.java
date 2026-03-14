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
        assertEquals(1, purchases.size()); // should purchase history be actuall purchase hist (all purchases) or only current purchases -a owned stocks list- (eg only things you have shares in)
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
    public void getPurchaseHistoryMaintainsTimestampOnEachTransaction() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        // throwing pipeline errors bc timestamp = same when testing so adding sleep to give distinct
        Thread.sleep(10);
        service.buy("GOOGL", 5, 2800.0);

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

    // ===== getHistorySorted: all transactions sorted by timestamp =====

    @Test
    public void getHistorySortedIncludesBothBuyAndSell() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 5, 155.0);
        service.buy("MSFT", 5, 300.0);

        List<InvestmentTransaction> sorted = service.getHistorySorted(null);

        assertEquals(3, sorted.size());
        assertTrue(sorted.stream().anyMatch(t -> "BUY".equals(t.type)));
        assertTrue(sorted.stream().anyMatch(t -> "SELL".equals(t.type)));
    }

    @Test
    public void getHistorySortedNewestFirstByDefault() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(5);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(5);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> desc = service.getHistorySorted(null);

        assertEquals(3, desc.size());
        // newest last in time = first in list
        assertEquals("SELL", desc.get(0).type);
        assertEquals("GOOGL", desc.get(1).ticker);
        assertEquals("AAPL", desc.get(2).ticker);
    }

    @Test
    public void getHistorySortedAscReturnsOldestFirst() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(5);
        service.buy("GOOGL", 5, 2800.0);
        Thread.sleep(5);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> asc = service.getHistorySorted("asc");

        assertEquals(3, asc.size());
        assertEquals("AAPL", asc.get(0).ticker);
        assertEquals("GOOGL", asc.get(1).ticker);
        assertEquals("SELL", asc.get(2).type);
    }

    @Test
    public void getHistorySortedEmptyWhenNoTransactions() {
        List<InvestmentTransaction> sorted = service.getHistorySorted("asc");

        assertTrue(sorted.isEmpty());
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

    // ===== Sell order: partial sell updates totalInvested and average book price =====

    @Test
    public void partialSellUpdatesTotalInvestedAndKeepsAverageBookPriceForRemainingShares() {
        service.buy("AAPL", 100, 150.0); // totalInvested 15000, avg 150
        Position before = service.getPosition("AAPL");
        assertEquals(100, before.shares);
        assertEquals(150, before.averageBookPrice);
        assertEquals(15000, before.totalInvested);

        service.sell("AAPL", 40, 160.0); // sell 40 at 160

        Position after = service.getPosition("AAPL");
        assertNotNull(after);
        assertEquals(60, after.shares);
        // cost basis sold = 40 * 150 = 6000; totalInvested = 15000 - 6000 = 9000
        assertEquals(9000, after.totalInvested);
        assertEquals(150, after.averageBookPrice); // 9000/60 = 150
    }

    @Test
    public void sellTransactionStoresRealizedGainLoss() {
        service.buy("AAPL", 10, 100.0); // cost basis 100 per share
        service.sell("AAPL", 4, 110.0); // proceeds 440, cost basis 400, gain 40

        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction sellTx = history.stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertEquals(40.0, sellTx.realizedGainLoss, 0.01);
    }

    @Test
    public void sellTransactionStoresRealizedLossWhenSellingBelowCost() {
        service.buy("MSFT", 20, 50.0); // cost basis 50 per share
        service.sell("MSFT", 10, 45.0); // proceeds 450, cost basis 500, loss -50

        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction sellTx = history.stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertEquals(-50.0, sellTx.realizedGainLoss, 0.01);
    }

    @Test
    public void sellingAllSharesStoresRealizedGainAndClosesPosition() {
        service.buy("GOOGL", 5, 200.0);
        service.sell("GOOGL", 5, 210.0); // proceeds 1050, cost 1000, gain 50

        assertNull(service.getPosition("GOOGL"));
        InvestmentTransaction sellTx = service.getHistory().stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertEquals(50.0, sellTx.realizedGainLoss, 0.01);
    }

    // ===== Sell order: positions contain only stocks user owns =====

    @Test
    public void getPositionsReturnsOnlyStocksUserOwns() {
        assertTrue(service.getPositions().isEmpty());

        service.buy("AAPL", 10, 150.0);
        service.buy("MSFT", 5, 300.0);
        assertEquals(2, service.getPositions().size());
        assertTrue(service.getPositions().stream().anyMatch(p -> "AAPL".equals(p.ticker)));
        assertTrue(service.getPositions().stream().anyMatch(p -> "MSFT".equals(p.ticker)));

        service.sell("AAPL", 10, 155.0);
        assertEquals(1, service.getPositions().size());
        assertEquals("MSFT", service.getPositions().iterator().next().ticker);
    }

    // ===== Average purchase price: GBP formatted for display (investment manager story) =====

    @Test
    public void positionAveragePurchasePriceFormattedInGbp() {
        service.buy("AAPL", 10, 150.0);

        Position pos = service.getPosition("AAPL");
        assertNotNull(pos);
        String gbp = pos.getAveragePurchasePriceGbp();
        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("150"));
    }

    @Test
    public void positionAveragePurchasePriceUpdatedAfterMultipleBuys() {
        service.buy("AAPL", 10, 100.0);
        service.buy("AAPL", 10, 200.0); // avg = 150

        Position pos = service.getPosition("AAPL");
        assertNotNull(pos);
        assertEquals(150, pos.averageBookPrice);
        String gbp = pos.getAveragePurchasePriceGbp();
        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("150"));
    }

    @Test
    public void positionsReturnFormattedAveragePurchasePriceGbp() {
        service.buy("MSFT", 5, 300.0);
        assertEquals(1, service.getPositions().size());
        Position p = service.getPositions().iterator().next();
        assertEquals("MSFT", p.ticker);
        assertTrue(p.getAveragePurchasePriceGbp().startsWith("£"));
    }

    @Test
    public void averagePurchasePriceNotDisplayedForStocksUserDoesNotOwn() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 10, 155.0);

        assertTrue(service.getPositions().isEmpty());
        assertNull(service.getPosition("AAPL"));
    }

    @Test
    public void noPositionsWhenNoStockPurchases() {
        assertTrue(service.getPositions().isEmpty());
    }

    // ===== Position after each transaction: updated share count on transaction list =====

    @Test
    public void buyTransactionRecordsSharesAfter() {
        service.buy("AAPL", 10, 150.0);

        List<InvestmentTransaction> history = service.getHistory();
        assertEquals(1, history.size());
        InvestmentTransaction tx = history.get(0);
        assertEquals("BUY", tx.type);
        assertEquals(10, tx.shares);
        assertNotNull(tx.sharesAfter);
        assertEquals(10, tx.sharesAfter.intValue());
    }

    @Test
    public void multipleBuysRecordCumulativeSharesAfter() {
        service.buy("AAPL", 10, 150.0);
        service.buy("AAPL", 5, 155.0);

        List<InvestmentTransaction> history = service.getHistory();
        assertEquals(2, history.size());
        assertEquals(10, history.get(0).sharesAfter.intValue());
        assertEquals(15, history.get(1).sharesAfter.intValue());
    }

    @Test
    public void partialSellTransactionRecordsSharesAfter() {
        service.buy("AAPL", 20, 100.0);
        service.sell("AAPL", 7, 110.0);

        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction sellTx = history.stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertNotNull(sellTx.sharesAfter);
        assertEquals(13, sellTx.sharesAfter.intValue());
        assertFalse(sellTx.isPositionClosed());
    }

    @Test
    public void sellAllSharesRecordsSharesAfterZeroAndPositionClosed() {
        service.buy("GOOGL", 5, 200.0);
        service.sell("GOOGL", 5, 210.0);

        List<InvestmentTransaction> history = service.getHistory();
        InvestmentTransaction sellTx = history.stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertNotNull(sellTx.sharesAfter);
        assertEquals(0, sellTx.sharesAfter.intValue());
        assertTrue(sellTx.isPositionClosed());
        assertNull(service.getPosition("GOOGL"));
    }

    @Test
    public void transactionListShowsUpdatedShareCountAfterEachTransaction() {
        service.buy("MSFT", 10, 300.0);
        service.sell("MSFT", 3, 310.0);
        service.sell("MSFT", 4, 305.0);

        List<InvestmentTransaction> history = service.getHistory();
        assertEquals(3, history.size());
        assertEquals(10, history.get(0).sharesAfter.intValue());
        assertEquals(7, history.get(1).sharesAfter.intValue());
        assertEquals(3, history.get(2).sharesAfter.intValue());
        assertFalse(history.get(1).isPositionClosed());
        assertFalse(history.get(2).isPositionClosed());
    }

    @Test
    public void sellThatClosesPositionMarksPositionClosed() {
        service.buy("AAPL", 4, 100.0);
        service.sell("AAPL", 4, 105.0);

        InvestmentTransaction sellTx = service.getHistory().stream()
                .filter(t -> "SELL".equals(t.type))
                .findFirst()
                .orElseThrow();
        assertTrue(sellTx.isPositionClosed());
        assertEquals(0, sellTx.sharesAfter.intValue());
    }

    // ===== newTransaction(): check funds, take money, update shares, return success/unsuccess (§1; still process when insufficient) =====

    @Test
    public void newTransactionWithValidArgsReturnsSuccessAndUpdatesPosition() {
        PurchaseResult result = service.newTransaction("AAPL", 10, 150.0);

        assertTrue(result.success);
        assertEquals("Transaction successful.", result.message);
        assertEquals("AAPL", result.ticker);
        assertEquals(10, result.shares);
        assertEquals(150.0, result.price, 0.01);
        assertTrue(result.totalCostGbp.contains("1,500") || result.totalCostGbp.contains("1500"));
        assertEquals(100000 - 1500, result.balanceAfter);
        assertEquals(1, service.getHistory().size());
        Position p = service.getPosition("AAPL");
        assertNotNull(p);
        assertEquals(10, p.shares);
    }

    @Test
    public void newTransactionWithInsufficientFundsStillProcessesPerRequirements() {
        account.withdraw(100000 - 500); // balance 500 (setUp deposited 100000)
        PurchaseResult result = service.newTransaction("MSFT", 10, 100.0); // cost 1000

        assertTrue(result.success);
        assertEquals("Transaction successful.", result.message);
        assertEquals(500 - 1000, result.balanceAfter);
        Position p = service.getPosition("MSFT");
        assertNotNull(p);
        assertEquals(10, p.shares);
        assertEquals(1, service.getHistory().size());
    }

    @Test
    public void newTransactionWithInvalidTickerReturnsUnsuccessful() {
        PurchaseResult result = service.newTransaction("INVALID12", 10, 150.0);

        assertFalse(result.success);
        assertTrue(result.message.contains("Ticker") || result.message.contains("1–5"));
        assertEquals(0, service.getHistory().size());
        assertEquals(100000, account.getBalance());
    }

    @Test
    public void newTransactionWithZeroSharesReturnsUnsuccessful() {
        PurchaseResult result = service.newTransaction("AAPL", 0, 150.0);

        assertFalse(result.success);
        assertTrue(result.message.contains("positive"));
        assertEquals(0, service.getHistory().size());
    }

    @Test
    public void buyWithInsufficientFundsLogsWarningAndStillProcesses() {
        account.withdraw(100000 - 200); // balance 200 (setUp deposited 100000)
        service.buy("AAPL", 5, 100.0); // cost 500, balance 200 -> -300

        assertEquals(1, service.getHistory().size());
        Position p = service.getPosition("AAPL");
        assertNotNull(p);
        assertEquals(5, p.shares);
        assertEquals(-300, account.getBalance());
    }

}

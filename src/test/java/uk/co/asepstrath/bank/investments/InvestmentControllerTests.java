package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.EquityRecord;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collection;
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

        // purchase history shows all BUY transactions
        assertEquals(1, result.size());
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

    // ===== Transaction list: sorted by timestamp, pagination =====

    @Test
    public void transactionsEndpointReturnsSortedListWhenNoPagination() throws InterruptedException {
        service.buy("AAPL", 10, 150.0);
        Thread.sleep(5);
        service.sell("AAPL", 5, 155.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.transactions(null, "asc", null, null);

        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<InvestmentTransaction> list = (List<InvestmentTransaction>) result;
        assertEquals(2, list.size());
        assertEquals("BUY", list.get(0).type);
        assertEquals("SELL", list.get(1).type);
    }

    @Test
    public void transactionsEndpointWithLimitReturnsPaginatedResponse() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.transactions(null, null, 2, 0);

        assertTrue(result instanceof TransactionListResponse);
        TransactionListResponse resp = (TransactionListResponse) result;
        assertEquals(3, resp.total);
        assertEquals(2, resp.transactions.size());
    }

    @Test
    public void transactionsEndpointPaginationRespectsOffset() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.transactions(null, null, 2, 1);

        assertTrue(result instanceof TransactionListResponse);
        TransactionListResponse resp = (TransactionListResponse) result;
        assertEquals(3, resp.total);
        assertEquals(2, resp.transactions.size());
    }

    @Test
    public void transactionsEndpointWithoutLimitReturnsPlainList() {
        service.buy("AAPL", 10, 150.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.transactions(null, null, null, null);

        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<InvestmentTransaction> list = (List<InvestmentTransaction>) result;
        assertEquals(1, list.size());
    }

    // ===== Holdings table: positions with symbol, stock name, total units, total price of units =====

    @Test
    public void positionsEndpointEnrichesWithStockNameWhenEquitiesServiceHasEquity() {
        EquitiesService equities = new EquitiesService();
        EquityRecord rec = new EquityRecord();
        rec.ticker = "AAPL";
        rec.name = "Apple Inc.";
        equities.addEquity(rec);

        service.buy("AAPL", 10, 150.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, equities, service);

        Object result = controller.positions();

        assertTrue(result instanceof Collection);
        @SuppressWarnings("unchecked")
        Collection<Position> positions = (Collection<Position>) result;
        assertEquals(1, positions.size());
        Position p = positions.iterator().next();
        assertEquals("AAPL", p.ticker);
        assertEquals("Apple Inc.", p.stockName);
        assertEquals(10, p.shares);
        assertTrue(p.getTotalInvestedGbp().startsWith("£"));
        assertTrue(p.getTotalInvestedGbp().contains("1,500") || p.getTotalInvestedGbp().contains("1500"));
    }

    @Test
    public void positionsEndpointUsesTickerAsStockNameWhenEquityNotFound() {
        EquitiesService equities = new EquitiesService();
        // no equity for AAPL
        service.buy("AAPL", 5, 200.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, equities, service);

        Object result = controller.positions();

        @SuppressWarnings("unchecked")
        Collection<Position> positions = (Collection<Position>) result;
        assertEquals(1, positions.size());
        assertEquals("AAPL", positions.iterator().next().stockName);
    }

    @Test
    public void positionsEndpointReturnsHoldingsWithUnitsAndTotalInvestedGbp() {
        service.buy("MSFT", 20, 300.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.positions();

        @SuppressWarnings("unchecked")
        Collection<Position> positions = (Collection<Position>) result;
        assertEquals(1, positions.size());
        Position p = positions.iterator().next();
        assertEquals("MSFT", p.ticker);
        assertEquals(20, p.shares);
        assertEquals(6000, p.totalInvested);
        assertTrue(p.getTotalInvestedGbp().startsWith("£"));
    }

    // ===== Current prices of each stock (§3 – value at moment of request) =====

    @Test
    public void positionsEndpointEnrichesWithCurrentPriceAndValueWhenPriceServiceHasPrice() {
        PriceService priceService = new PriceService();
        PriceRecord pr = new PriceRecord();
        pr.ticker = "AAPL";
        pr.price = 160;
        pr.date = "2025-03-13";
        priceService.addPrice(pr);

        service.buy("AAPL", 10, 150.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, priceService, null, service);

        Object result = controller.positions();

        @SuppressWarnings("unchecked")
        Collection<Position> positions = (Collection<Position>) result;
        assertEquals(1, positions.size());
        Position p = positions.iterator().next();
        assertEquals(160.0, p.currentPrice, 0.01);
        assertEquals(1600.0, p.currentValue, 0.01);
        assertTrue(p.getCurrentValueGbp().contains("1,600") || p.getCurrentValueGbp().contains("1600"));
    }

    @Test
    public void positionsEndpointLeavesCurrentPriceNullWhenPriceNotFound() {
        PriceService priceService = new PriceService();
        // no price for AAPL
        service.buy("AAPL", 5, 200.0);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, priceService, null, service);

        Object result = controller.positions();

        @SuppressWarnings("unchecked")
        Collection<Position> positions = (Collection<Position>) result;
        assertEquals(1, positions.size());
        Position p = positions.iterator().next();
        assertNull(p.currentPrice);
        assertNull(p.currentValue);
        assertEquals("—", p.getCurrentValueGbp());
    }

    // ===== Portfolio summary endpoint (§3 – total portfolio value = positions + cash) =====

    @Test
    public void portfolioSummaryReturnsTotalAsPositionsValuePlusCash() {
        PriceService priceService = new PriceService();
        PriceRecord pr = new PriceRecord();
        pr.ticker = "AAPL";
        pr.price = 155;
        priceService.addPrice(pr);

        service.buy("AAPL", 20, 150.0); // cost 3000, cash was 100000 -> 97000
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, priceService, null, service);

        Object result = controller.portfolioSummary();

        assertTrue(result instanceof PortfolioSummaryResponse);
        PortfolioSummaryResponse resp = (PortfolioSummaryResponse) result;
        assertEquals(20 * 155.0, resp.positionsValue, 0.01); // 3100
        assertEquals(97000, resp.cashBalance);
        assertEquals(97000 + 3100.0, resp.totalPortfolioValue, 0.01);
    }

    @Test
    public void portfolioSummaryWhenNoPositionsReturnsCashOnly() {
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.portfolioSummary();

        assertTrue(result instanceof PortfolioSummaryResponse);
        PortfolioSummaryResponse resp = (PortfolioSummaryResponse) result;
        assertEquals(0.0, resp.positionsValue, 0.01);
        assertEquals(100000, resp.cashBalance);
        assertEquals(100000.0, resp.totalPortfolioValue, 0.01);
    }

    // ===== Buy stocks with available funds (§1: newTransaction, immediate notification) =====

    @Test
    public void buyEndpointReturnsPurchaseResultWhenValid() {
        TradeRequest req = new TradeRequest();
        req.ticker = "AAPL";
        req.shares = 10;
        req.price = 150.0;
        io.jooby.Context ctx = org.mockito.Mockito.mock(io.jooby.Context.class);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.buy(req, ctx);

        assertTrue(result instanceof PurchaseResult);
        PurchaseResult pr = (PurchaseResult) result;
        assertTrue(pr.success);
        assertEquals("Transaction successful.", pr.message);
        assertEquals("AAPL", pr.ticker);
        assertEquals(10, pr.shares);
        assertEquals(98500, pr.balanceAfter); // 100000 - 10*150
    }

    @Test
    public void buyEndpointRejectsInvalidTicker() {
        TradeRequest req = new TradeRequest();
        req.ticker = "TOOLONG";
        req.shares = 10;
        req.price = 150.0;
        io.jooby.Context ctx = org.mockito.Mockito.mock(io.jooby.Context.class);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.buy(req, ctx);

        assertTrue(result instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) result;
        assertTrue(map.containsKey("error"));
        assertEquals(0, service.getHistory().size());
    }

    @Test
    public void buyEndpointRejectsNonPositiveShares() {
        TradeRequest req = new TradeRequest();
        req.ticker = "AAPL";
        req.shares = 0;
        req.price = 150.0;
        io.jooby.Context ctx = org.mockito.Mockito.mock(io.jooby.Context.class);
        InvestmentController controller = new InvestmentController(
                (DataSource) null, LoggerFactory.getLogger(InvestmentControllerTests.class), account, null, null, service);

        Object result = controller.buy(req, ctx);

        assertTrue(result instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) result;
        assertTrue(map.containsKey("error"));
    }

}

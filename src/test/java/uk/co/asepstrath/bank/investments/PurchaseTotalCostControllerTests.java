package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseTotalCostControllerTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(10_000_000);
        service = new InvestmentService(account);
    }

    // ===== Tests for total cost in /investments/transactions endpoint =====

    @Test
    public void transactionsEndpointIncludesTotalCostForEachPurchase() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> result = service.getHistory();

        assertEquals(2, result.size());
        for (InvestmentTransaction tx : result) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    @Test
    public void transactionsEndpointWithPurchasesFilterIncludesTotalCost() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(2, result.size());
        for (InvestmentTransaction tx : result) {
            assertEquals("BUY", tx.type);
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    @Test
    public void purchaseHistoryWithNoFilterShowsTotalCostForAllPurchases() {
        service.buy("AAPL", 10, 150.0);
        service.sell("AAPL", 5, 155.0);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(2, result.size());
        for (InvestmentTransaction tx : result) {
            assertNotNull(tx.getTotalCostGbp());
        }
    }

    // ===== Tests for total cost accuracy in endpoint responses =====

    @Test
    public void eachPurchaseInEndpointResponseHasAccurateTotalCost() {
        service.buy("AAPL", 25, 145.50);
        service.buy("MSFT", 100, 320.75);
        service.buy("GOOGL", 8, 2890.00);

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(3, result.size());
        
        InvestmentTransaction aapl = result.get(0);
        assertEquals("AAPL", aapl.ticker);
        assertEquals(25, aapl.shares);
        assertEquals(145.50, aapl.price);
        // 25 * 145.50 = 3,637.50
        assertTrue(aapl.getTotalCostGbp().contains("3,637.50"));

        InvestmentTransaction msft = result.get(1);
        assertEquals("MSFT", msft.ticker);
        assertEquals(100, msft.shares);
        assertEquals(320.75, msft.price);
        // 100 * 320.75 = 32,075.00
        assertTrue(msft.getTotalCostGbp().contains("32,075"));

        InvestmentTransaction googl = result.get(2);
        assertEquals("GOOGL", googl.ticker);
        assertEquals(8, googl.shares);
        assertEquals(2890.0, googl.price);
        // 8 * 2890 = 23,120.00
        assertTrue(googl.getTotalCostGbp().contains("23,120"));
    }

    // ===== Tests for total cost display consistency across different views =====

    @Test
    public void totalCostConsistentBetweenAllHistoryAndFilteredHistory() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> allTransactions = service.getHistory();
        List<InvestmentTransaction> purchaseOnly = service.getPurchaseHistory(null);

        // Find the AAPL purchase in both lists
        InvestmentTransaction aaplInAll = allTransactions.stream()
                .filter(tx -> "AAPL".equals(tx.ticker) && "BUY".equals(tx.type))
                .findFirst()
                .orElse(null);

        InvestmentTransaction aaplInPurchases = purchaseOnly.stream()
                .filter(tx -> "AAPL".equals(tx.ticker))
                .findFirst()
                .orElse(null);

        assertNotNull(aaplInAll);
        assertNotNull(aaplInPurchases);
        assertEquals(aaplInAll.getTotalCostGbp(), aaplInPurchases.getTotalCostGbp());
    }

    @Test
    public void totalCostUnaffectedBySortOrderInResponse() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> ascResult = service.getPurchaseHistory("asc");
        List<InvestmentTransaction> descResult = service.getPurchaseHistory("desc");

        // Find AAPL in both sorted results
        InvestmentTransaction aaplAsc = ascResult.stream()
                .filter(tx -> "AAPL".equals(tx.ticker))
                .findFirst()
                .orElse(null);

        InvestmentTransaction aaplDesc = descResult.stream()
                .filter(tx -> "AAPL".equals(tx.ticker))
                .findFirst()
                .orElse(null);

        assertNotNull(aaplAsc);
        assertNotNull(aaplDesc);
        assertEquals(aaplAsc.getTotalCostGbp(), aaplDesc.getTotalCostGbp());
    }

    // ===== Tests for correct GBP formatting in endpoint response =====

    @Test
    public void allTotalCostsInResponseFormatCorrectlyAsGBP() {
        service.buy("AAPL", 1, 10.00);      // £10.00
        service.buy("GOOGL", 10, 100.00);   // £1,000.00
        service.buy("MSFT", 100, 50.00);    // £5,000.00

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(3, result.size());
        for (InvestmentTransaction tx : result) {
            String totalCost = tx.getTotalCostGbp();
            assertTrue(totalCost.startsWith("£"), "Every total cost should start with £ symbol");
            assertFalse(totalCost.contains("$"), "Should not contain $ symbol");
            assertFalse(totalCost.contains("€"), "Should not contain € symbol");
        }
    }

    @Test
    public void totalCostResponseWithVariousAmounts() {
        service.buy("SYMB1", 1, 5.50);      // Very small amount
        service.buy("SYMB2", 50, 100.00);   // Thousands
        service.buy("SYMB3", 5000, 250.00); // Hundred thousands

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(3, result.size());
        
        // Small amount
        assertTrue(result.get(0).getTotalCostGbp().startsWith("£"));
        assertTrue(result.get(0).getTotalCostGbp().contains("5.50"));

        // Thousands
        assertTrue(result.get(1).getTotalCostGbp().startsWith("£"));
        assertTrue(result.get(1).getTotalCostGbp().contains("5,000"));

        // Hundred thousands
        assertTrue(result.get(2).getTotalCostGbp().startsWith("£"));
        assertTrue(result.get(2).getTotalCostGbp().contains("1,250,000"));
    }

    // ===== Tests for total cost with edge cases in endpoint =====

    @Test
    public void emptyPurchaseHistoryReturnsNoTotalCosts() {
        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(0, result.size());
    }

    @Test
    public void onlyOnePurchaseStillDisplaysTotalCost() {
        service.buy("AAPL", 10, 150.0);

        List<InvestmentTransaction> result = service.getPurchaseHistory(null);

        assertEquals(1, result.size());
        InvestmentTransaction tx = result.get(0);
        assertNotNull(tx.getTotalCostGbp());
        assertTrue(tx.getTotalCostGbp().contains("1,500"));
    }

    @Test
    public void largeNumberOfPurchasesAllHaveTotalCost() {
        // Create 50 purchase transactions
        for (int i = 0; i < 50; i++) {
            service.buy("TICK" + i, 10 + i, 100.0 + i);
        }

        List<InvestmentTransaction> result = service.getPurchaseHistory("asc");

        assertEquals(50, result.size());
        for (InvestmentTransaction tx : result) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    // ===== Tests for total cost data integrity =====

    @Test
    public void totalCostDoesNotChangeAfterPurchaseCreation() {
        service.buy("AAPL", 10, 150.0);
        
        List<InvestmentTransaction> firstFetch = service.getPurchaseHistory(null);
        String firstTotalCost = firstFetch.get(0).getTotalCostGbp();

        // Make another purchase
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> secondFetch = service.getPurchaseHistory(null);
        String secondTotalCostAAPL = secondFetch.stream()
                .filter(tx -> "AAPL".equals(tx.ticker))
                .findFirst()
                .map(InvestmentTransaction::getTotalCostGbp)
                .orElse(null);

        assertEquals(firstTotalCost, secondTotalCostAAPL);
    }

    @Test
    public void purchaseTotalCostUnaffectedBySubsequentSales() {
        service.buy("AAPL", 100, 150.0);
        
        String totalCostBeforeSale = service.getPurchaseHistory(null)
                .get(0)
                .getTotalCostGbp();

        // Sell part of the holdings
        service.sell("AAPL", 50, 155.0);

        String totalCostAfterSale = service.getPurchaseHistory(null)
                .get(0)
                .getTotalCostGbp();

        // Total cost of purchase should remain unchanged
        assertEquals(totalCostBeforeSale, totalCostAfterSale);
    }

    @Test
    public void sellTransactionDoesNotShowInPurchaseHistoryTotalCost() {
        service.buy("AAPL", 100, 150.0);
        service.sell("AAPL", 50, 155.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);
        List<InvestmentTransaction> allTransactions = service.getHistory();

        // Should only have 1 purchase
        assertEquals(1, purchases.size());
        // But full history should have 2 (1 buy + 1 sell)
        assertEquals(2, allTransactions.size());

        // Verify only BUY transaction is in purchases with total cost
        InvestmentTransaction purchaseTx = purchases.get(0);
        assertEquals("BUY", purchaseTx.type);
        assertTrue(purchaseTx.getTotalCostGbp().contains("15,000")); // 100 * 150
    }

}

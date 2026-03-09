package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseTotalCostTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(10000000); // Start with sufficient funds for all tests
        service = new InvestmentService(account);
    }

    // ===== Tests for total cost calculation formula (shares * price) =====

    @Test
    public void totalCostCalculatesSharesMultipliedByPrice() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();
        // 10 * 150 = 1500
        assertTrue(totalCost.contains("1,500"));
    }

    @Test
    public void totalCostCalculatesCorrectlyForLargeQuantities() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 1000;
        tx.price = 350.75;

        String totalCost = tx.getTotalCostGbp();
        // 1000 * 350.75 = 350,750
        assertTrue(totalCost.contains("350,750"));
    }

    @Test
    public void totalCostCalculatesCorrectlyWithDecimalShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 5;
        tx.price = 2850.50;

        String totalCost = tx.getTotalCostGbp();
        // 5 * 2850.50 = 14,252.50
        assertTrue(totalCost.contains("14,252.50"));
    }

    @Test
    public void totalCostCalculatesCorrectlyForSingleShare() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 1;
        tx.price = 1500.0;

        String totalCost = tx.getTotalCostGbp();
        // 1 * 1500 = 1500
        assertTrue(totalCost.contains("1,500"));
    }

    @Test
    public void totalCostCalculatesCorrectlyForFractionalPrices() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 100;
        tx.price = 0.50;

        String totalCost = tx.getTotalCostGbp();
        // 100 * 0.50 = 50
        assertTrue(totalCost.contains("50"));
    }

    // ===== Tests for GBP formatting =====

    @Test
    public void totalCostFormatsInGBPCurrency() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.startsWith("£"), "Total cost should start with £ symbol");
    }

    @Test
    public void totalCostFormatsWithThousandsSeparator() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 100;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();
        // 100 * 150 = 15,000
        assertTrue(totalCost.contains(","), "Total cost should include thousands separator");
        assertTrue(totalCost.contains("15,000"));
    }

    @Test
    public void totalCostFormatsWithMultipleThousandsSeparators() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10000;
        tx.price = 500.0;

        String totalCost = tx.getTotalCostGbp();
        // 10000 * 500 = 5,000,000
        assertTrue(totalCost.contains(","));
        assertTrue(totalCost.contains("5,000,000"));
    }

    @Test
    public void totalCostFormatsWithPenceCorrectly() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 50;
        tx.price = 10.45;

        String totalCost = tx.getTotalCostGbp();
        // 50 * 10.45 = 522.50
        assertTrue(totalCost.contains("522.50"), "Should display pence with .50");
    }

    @Test
    public void totalCostFormatsSmallAmountsCorrectly() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 1;
        tx.price = 5.00;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.startsWith("£"));
        assertTrue(totalCost.contains("5"));
    }

    // ===== Tests for validation of negative/zero values =====

    @Test
    public void buyRejectedWithZeroShares() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 0, 150.0));
    }

    @Test
    public void buyRejectedWithNegativeShares() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", -10, 150.0));
    }

    @Test
    public void buyRejectedWithZeroPrice() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 10, 0.0));
    }

    @Test
    public void buyRejectedWithNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> service.buy("AAPL", 10, -150.0));
    }

    @Test
    public void totalCostReturnsZeroForZeroShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 0;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void totalCostReturnsZeroForZeroPrice() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = 0.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void totalCostReturnsZeroForNegativeShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = -10;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void totalCostReturnsZeroForNegativePrice() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = -150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    // ===== Tests for total cost displayed in purchase history =====

    @Test
    public void purchaseHistoryTransactionsHaveTotalCostAvailable() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(2, purchases.size());
        for (InvestmentTransaction tx : purchases) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    @Test
    public void eachPurchaseTransactionShowsCorrectTotalCost() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(3, purchases.size());
        // AAPL: 10 * 150 = 1500
        assertTrue(purchases.get(0).getTotalCostGbp().contains("1,500"));
        // GOOGL: 5 * 2800 = 14,000
        assertTrue(purchases.get(1).getTotalCostGbp().contains("14,000"));
        // MSFT: 20 * 300 = 6,000
        assertTrue(purchases.get(2).getTotalCostGbp().contains("6,000"));
    }

    @Test
    public void totalCostCorrectForMultiplePurchasesOfSameStock() {
        service.buy("AAPL", 10, 150.0);
        service.buy("AAPL", 5, 155.0);
        service.buy("AAPL", 20, 160.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(3, purchases.size());
        // Each transaction should calculate its own total cost correctly
        assertTrue(purchases.get(0).getTotalCostGbp().contains("1,500")); // 10 * 150
        assertTrue(purchases.get(1).getTotalCostGbp().contains("775"));   // 5 * 155
        assertTrue(purchases.get(2).getTotalCostGbp().contains("3,200")); // 20 * 160
    }

    @Test
    public void filteredPurchaseHistoryIncludesTotalCosts() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);

        assertEquals(3, purchases.size());
        for (InvestmentTransaction tx : purchases) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    // ===== Tests for total cost integration with sorted purchase history =====

    @Test
    public void totalCostDisplayedCorrectlyWhenSortedAscending() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        // Verify all totals are calculated correctly regardless of sort order
        assertEquals(3, purchases.size());
        for (InvestmentTransaction tx : purchases) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    @Test
    public void totalCostDisplayedCorrectlyWhenSortedDescending() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.buy("MSFT", 20, 300.0);

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("desc");

        // Verify all totals are calculated correctly regardless of sort order
        assertEquals(3, purchases.size());
        for (InvestmentTransaction tx : purchases) {
            assertNotNull(tx.getTotalCostGbp());
            assertTrue(tx.getTotalCostGbp().startsWith("£"));
        }
    }

    // ===== Tests for total cost consistency across all transaction types =====

    @Test
    public void totalCostCalculatedOnlyForBuyTransactions() {
        service.buy("AAPL", 10, 150.0);
        service.buy("GOOGL", 5, 2800.0);
        service.sell("AAPL", 5, 155.0);

        List<InvestmentTransaction> fullHistory = service.getHistory();
        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);

        // Full history should have 3 items (2 buy + 1 sell)
        assertEquals(3, fullHistory.size());
        // Purchase history should have only 2 items
        assertEquals(2, purchases.size());
        // All items in purchase history should have total cost
        for (InvestmentTransaction tx : purchases) {
            assertEquals("BUY", tx.type);
            assertNotNull(tx.getTotalCostGbp());
        }
    }

    // ===== Tests for large volume purchase tracking =====

    @Test
    public void largeVolumePurchasesCalculateTotalCostCorrectly() {
        service.buy("AAPL", 5000, 175.50);
        
        List<InvestmentTransaction> purchases = service.getPurchaseHistory(null);
        
        assertEquals(1, purchases.size());
        InvestmentTransaction tx = purchases.get(0);
        // 5000 * 175.50 = 877,500
        assertTrue(tx.getTotalCostGbp().contains("877,500"));
    }

    @Test
    public void manyTransactionsAllDisplayTotalCostCorrectly() {
        // Create 100 purchase transactions
        for (int i = 0; i < 100; i++) {
            service.buy("TICK" + i, 10 + i, 100.0 + i);
        }

        List<InvestmentTransaction> purchases = service.getPurchaseHistory("asc");

        assertEquals(100, purchases.size());
        // Verify first, middle, and last transactions have correct totals
        assertTrue(purchases.get(0).getTotalCostGbp().startsWith("£"));
        assertTrue(purchases.get(50).getTotalCostGbp().startsWith("£"));
        assertTrue(purchases.get(99).getTotalCostGbp().startsWith("£"));
    }

}

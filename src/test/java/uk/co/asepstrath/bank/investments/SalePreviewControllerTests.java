package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import static org.junit.jupiter.api.Assertions.*;

public class SalePreviewControllerTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(500000);
        service = new InvestmentService(account);
    }

    // ===== Tests for GET /sell/preview endpoint =====

    @Test
    public void salePreviewEndpointReturnsSalePreviewObject() {
        service.buy("AAPL", 10, 150.0);
        
        Object result = service.getSalePreview("AAPL", 5, 155.0);

        assertNotNull(result);
        assertTrue(result instanceof SalePreview);
    }

    @Test
    public void salePreviewIncludesStockSymbol() {
        service.buy("AAPL", 10, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 5, 155.0);

        assertEquals("AAPL", preview.symbol);
    }

    @Test
    public void salePreviewIncludesStockName() {
        service.buy("GOOGL", 10, 2800.0);
        
        SalePreview preview = service.getSalePreview("GOOGL", 5, 2850.0);

        assertNotNull(preview.name);
        assertEquals("GOOGL", preview.name); // Currently placeholder, will be enhanced
    }

    @Test
    public void previewDoesNotRecordTransaction() {
        service.buy("AAPL", 10, 150.0);
        int beforeSize = service.getHistory().size();

        service.getSalePreview("AAPL", 5, 155.0);
        int afterSize = service.getHistory().size();

        assertEquals(beforeSize, afterSize, "Preview should not create any history entries");
    }

    @Test
    public void previewDoesNotModifyHoldingsOrBalance() {
        service.buy("AAPL", 10, 150.0);
        int beforeBalance = account.getBalance();
        int beforeShares = service.getPosition("AAPL").shares;

        service.getSalePreview("AAPL", 2, 155.0);

        assertEquals(beforeBalance, account.getBalance());
        assertEquals(beforeShares, service.getPosition("AAPL").shares);
    }

    @Test
    public void salePreviewIncludesNumberOfShares() {
        service.buy("MSFT", 50, 300.0);
        
        SalePreview preview = service.getSalePreview("MSFT", 20, 310.0);

        assertEquals(20, preview.shares);
    }

    @Test
    public void salePreviewIncludesEstimatedPricePerShare() {
        service.buy("AAPL", 10, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 5, 165.50);

        assertEquals(165.50, preview.estimatedPricePerShare);
    }

    @Test
    public void salePreviewIncludesEstimatedTotalProceeds() {
        service.buy("AAPL", 100, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 50, 155.0);

        assertNotNull(preview.estimatedTotalProceedsGbp);
        // 50 * 155 = 7750
        assertTrue(preview.estimatedTotalProceedsGbp.contains("7,750"));
    }

    // ===== Tests for preview data accuracy =====

    @Test
    public void previewAccuratelyCalculatesTotalProceeds() {
        service.buy("AAPL", 10, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 10, 160.0);

        // 10 * 160 = 1600
        assertTrue(preview.estimatedTotalProceedsGbp.contains("1,600"));
    }

    @Test
    public void previewTotalProceedsFormattedInGBP() {
        service.buy("AAPL", 100, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 50, 155.0);

        assertTrue(preview.estimatedTotalProceedsGbp.startsWith("£"));
    }

    @Test
    public void previewWithDecimalPrices() {
        service.buy("AAPL", 100, 150.75);
        
        SalePreview preview = service.getSalePreview("AAPL", 50, 155.25);

        assertEquals(155.25, preview.estimatedPricePerShare);
        assertTrue(preview.estimatedTotalProceedsGbp.contains("7,762.50")); // 50 * 155.25
    }

    @Test
    public void previewWithLargeQuantities() {
        service.buy("PENNY", 10000, 0.50);
        
        SalePreview preview = service.getSalePreview("PENNY", 5000, 0.75);

        assertEquals(5000, preview.shares);
        assertEquals(0.75, preview.estimatedPricePerShare);
        assertTrue(preview.estimatedTotalProceedsGbp.contains("3,750")); // 5000 * 0.75
    }

    // ===== Tests for preview validation =====

    @Test
    public void previewThrowsExceptionForZeroShares() {
        service.buy("AAPL", 10, 150.0);

        assertThrows(IllegalArgumentException.class, () -> service.getSalePreview("AAPL", 0, 155.0));
    }

    @Test
    public void previewThrowsExceptionForNegativeShares() {
        service.buy("AAPL", 10, 150.0);

        assertThrows(IllegalArgumentException.class, () -> service.getSalePreview("AAPL", -5, 155.0));
    }

    @Test
    public void previewThrowsExceptionForZeroPrice() {
        service.buy("AAPL", 10, 150.0);

        assertThrows(IllegalArgumentException.class, () -> service.getSalePreview("AAPL", 5, 0.0));
    }

    @Test
    public void previewThrowsExceptionForNegativePrice() {
        service.buy("AAPL", 10, 150.0);

        assertThrows(IllegalArgumentException.class, () -> service.getSalePreview("AAPL", 5, -155.0));
    }

    // ===== Tests for preview workflow - user reviews before selling =====

    @Test
    public void userCanReviewPreviewBeforeSelling() {
        service.buy("AAPL", 100, 150.0);
        
        // Step 1: Get preview
        SalePreview preview = service.getSalePreview("AAPL", 50, 155.0);
        
        // User reviews the preview
        assertEquals(50, preview.shares);
        assertEquals(155.0, preview.estimatedPricePerShare);
        assertTrue(preview.estimatedTotalProceedsGbp.contains("7,750"));
        
        // Step 2: User confirms and sells (if satisfied)
        SellConfirmation confirmation = service.sell("AAPL", 50, 155.0);
        
        // Verify confirmation matches preview
        assertEquals(preview.shares, confirmation.shares);
        assertEquals(preview.estimatedPricePerShare, confirmation.finalSalePrice);
    }

    @Test
    public void previewDataDoesNotChangeUntilSellConfirmed() {
        service.buy("AAPL", 10, 150.0);
        
        SalePreview preview1 = service.getSalePreview("AAPL", 5, 155.0);
        String proceeds1 = preview1.estimatedTotalProceedsGbp;
        
        // Get another preview with same parameters
        SalePreview preview2 = service.getSalePreview("AAPL", 5, 155.0);
        String proceeds2 = preview2.estimatedTotalProceedsGbp;
        
        // Should be identical
        assertEquals(proceeds1, proceeds2);
    }

    @Test
    public void previewDoesNotModifyAccountState() {
        account.deposit(10000);
        int balanceBeforePreview = account.getBalance();
        
        service.buy("AAPL", 10, 150.0);
        int balanceAfterBuy = account.getBalance();
        
        // Get preview - should not change balance
        service.getSalePreview("AAPL", 5, 155.0);
        int balanceAfterPreview = account.getBalance();
        
        assertEquals(balanceAfterBuy, balanceAfterPreview);
    }

    @Test
    public void multiplePreviewsCanBeRetrievedBeforeSelling() {
        service.buy("AAPL", 100, 150.0);
        
        // User might review multiple times or different quantities
        SalePreview preview1 = service.getSalePreview("AAPL", 25, 155.0);
        SalePreview preview2 = service.getSalePreview("AAPL", 50, 155.0);
        SalePreview preview3 = service.getSalePreview("AAPL", 100, 155.0);
        
        assertEquals(25, preview1.shares);
        assertEquals(50, preview2.shares);
        assertEquals(100, preview3.shares);
    }

    @Test
    public void previewWithDifferentPricePoints() {
        service.buy("AAPL", 100, 150.0);
        
        // User might want to see what different prices would yield
        SalePreview preview1 = service.getSalePreview("AAPL", 50, 150.0);
        SalePreview preview2 = service.getSalePreview("AAPL", 50, 160.0);
        SalePreview preview3 = service.getSalePreview("AAPL", 50, 170.0);
        
        // All should be accessible
        assertNotNull(preview1.estimatedTotalProceedsGbp);
        assertNotNull(preview2.estimatedTotalProceedsGbp);
        assertNotNull(preview3.estimatedTotalProceedsGbp);
    }

    // ===== Tests for preview format and consistency =====

    @Test
    public void previewFormatIsUserFriendly() {
        service.buy("AAPL", 100, 150.0);
        
        SalePreview preview = service.getSalePreview("AAPL", 50, 155.0);

        // Symbol and name accessible
        assertNotNull(preview.symbol);
        assertNotNull(preview.name);
        
        // Quantities clearly specified
        assertTrue(preview.shares > 0);
        assertTrue(preview.estimatedPricePerShare > 0);
        
        // Total formatted clearly
        assertTrue(preview.estimatedTotalProceedsGbp.startsWith("£"));
    }

    @Test
    public void previewAllFieldsPopulated() {
        service.buy("GOOGL", 20, 2800.0);
        
        SalePreview preview = service.getSalePreview("GOOGL", 10, 2850.0);

        assertNotNull(preview.symbol);
        assertNotNull(preview.name);
        assertTrue(preview.shares > 0);
        assertTrue(preview.estimatedPricePerShare > 0);
        assertNotNull(preview.estimatedTotalProceedsGbp);
    }

    // ===== Tests for preview matching confirmation =====

    @Test
    public void confirmationProducedFromPreviewMatchesPreviewData() {
        service.buy("AAPL", 100, 150.0);
        
        // Get preview
        SalePreview preview = service.getSalePreview("AAPL", 30, 160.0);
        
        // Use preview data to execute sale
        SellConfirmation confirmation = service.sell(preview.symbol, preview.shares, preview.estimatedPricePerShare);
        
        // Confirmation should match preview
        assertEquals(preview.symbol, confirmation.ticker);
        assertEquals(preview.shares, confirmation.shares);
        assertEquals(preview.estimatedPricePerShare, confirmation.finalSalePrice);
    }

}

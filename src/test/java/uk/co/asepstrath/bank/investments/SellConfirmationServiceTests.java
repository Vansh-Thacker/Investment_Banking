package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import static org.junit.jupiter.api.Assertions.*;

public class SellConfirmationServiceTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(500000);
        service = new InvestmentService(account);
    }

    // ===== Tests for return of SellConfirmation after successful sale =====

    @Test
    public void sellReturnsConfirmationObject() {
        service.buy("AAPL", 10, 150.0);
        
        Object result = service.sell("AAPL", 5, 155.0);

        assertNotNull(result);
        assertTrue(result instanceof SellConfirmation);
    }

    @Test
    public void sellConfirmationContainsTickerOfSoldStock() {
        service.buy("GOOGL", 10, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 5, 2850.0);

        assertEquals("GOOGL", confirmation.ticker);
    }

    @Test
    public void sellConfirmationContainsNumberOfSharesSold() {
        service.buy("MSFT", 100, 300.0);
        
        SellConfirmation confirmation = service.sell("MSFT", 50, 310.0);

        assertEquals(50, confirmation.shares);
    }

    // ===== Tests for success message content =====

    @Test
    public void sellConfirmationHasSuccessMessage() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertNotNull(confirmation.successMessage);
        assertTrue(confirmation.successMessage.length() > 0);
    }

    @Test
    public void successMessageIndicatesSuccessfullSale() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertTrue(confirmation.successMessage.contains("successful") || 
                  confirmation.successMessage.contains("Success") ||
                  confirmation.successMessage.contains("Successful"),
                  "Success message should indicate successful sale");
    }

    @Test
    public void successMessageIncludesTickerSymbol() {
        service.buy("GOOGL", 10, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 3, 2850.0);

        assertTrue(confirmation.successMessage.contains("GOOGL"),
                  "Success message should include stock ticker");
    }

    @Test
    public void successMessageIncludesNumberOfSharesSold() {
        service.buy("MSFT", 100, 300.0);
        
        SellConfirmation confirmation = service.sell("MSFT", 25, 310.0);

        assertTrue(confirmation.successMessage.contains("25"),
                  "Success message should include number of shares sold");
    }

    @Test
    public void successMessageIncludesFinalSalePrice() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 165.50);

        assertTrue(confirmation.successMessage.contains("165.50") ||
                  confirmation.successMessage.contains("165.5") ||
                  confirmation.successMessage.contains("£165"),
                  "Success message should include final sale price");
    }

    @Test
    public void successMessageIncludesTotalEarnings() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 10, 155.0);

        // Total earnings: 10 * 155 = 1550
        assertTrue(confirmation.successMessage.contains("1,550") || 
                  confirmation.successMessage.contains("1550"),
                  "Success message should include total earnings");
    }

    @Test
    public void successMessageContainsAllRequiredInformation() {
        service.buy("GOOGL", 20, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 10, 2900.0);

        String message = confirmation.successMessage;
        
        assertTrue(message.contains("GOOGL"), "Should contain ticker");
        assertTrue(message.contains("10"), "Should contain shares");
        assertTrue(message.contains("2900") || message.contains("2,900"), "Should contain price");
        assertTrue(message.contains("29000") || message.contains("29,000"), "Should contain total earnings");
    }

    // ===== Tests for confirmation data fields =====

    @Test
    public void confirmationContainsFinalSalePricePerShare() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 160.0);

        assertEquals(160.0, confirmation.finalSalePrice);
    }

    @Test
    public void confirmationContainsFinalSalePriceInGBP() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 160.0);

        assertNotNull(confirmation.finalSalePriceGbp);
        assertTrue(confirmation.finalSalePriceGbp.startsWith("£"));
        assertTrue(confirmation.finalSalePriceGbp.contains("160"));
    }

    @Test
    public void confirmationContainsTotalEarningsInGBP() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 10, 155.0);

        assertNotNull(confirmation.totalEarningsGbp);
        assertTrue(confirmation.totalEarningsGbp.startsWith("£"));
        // 10 * 155 = 1550
        assertTrue(confirmation.totalEarningsGbp.contains("1,550"));
    }

    // ===== Tests for navigation URLs =====

    @Test
    public void confirmationContainsPortfolioNavigationUrl() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertNotNull(confirmation.portfolioUrl);
        assertTrue(confirmation.portfolioUrl.contains("/investments/positions"));
    }

    @Test
    public void confirmationContainsTransactionHistoryNavigationUrl() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertNotNull(confirmation.transactionHistoryUrl);
        assertTrue(confirmation.transactionHistoryUrl.contains("/investments/transactions"));
    }

    @Test
    public void confirmationNavigationUrlsAreValidEndpoints() {
        service.buy("GOOGL", 10, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 5, 2850.0);

        assertEquals("/investments/positions", confirmation.portfolioUrl);
        assertEquals("/investments/transactions", confirmation.transactionHistoryUrl);
    }

    // ===== Tests for confirmation accuracy with different sale amounts =====

    @Test
    public void confirmationAccurateForPartialSale() {
        service.buy("AAPL", 100, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 50, 155.0);

        assertEquals(50, confirmation.shares);
        assertEquals(155.0, confirmation.finalSalePrice);
        assertTrue(confirmation.totalEarningsGbp.contains("7,750")); // 50 * 155
    }

    @Test
    public void confirmationAccurateForCompleteSale() {
        service.buy("MSFT", 25, 300.0);
        
        SellConfirmation confirmation = service.sell("MSFT", 25, 310.0);

        assertEquals(25, confirmation.shares);
        assertEquals(310.0, confirmation.finalSalePrice);
        assertTrue(confirmation.totalEarningsGbp.contains("7,750")); // 25 * 310
    }

    @Test
    public void confirmationWithHighPricedStocks() {
        service.buy("BRK.B", 5, 450000.0);
        
        SellConfirmation confirmation = service.sell("BRK.B", 2, 455000.0);

        assertEquals(2, confirmation.shares);
        assertEquals(455000.0, confirmation.finalSalePrice);
        assertTrue(confirmation.totalEarningsGbp.contains("910,000"));
    }

    // ===== Tests for confirmation with various price points =====

    @Test
    public void confirmationWithDecimalPrices() {
        service.buy("AAPL", 100, 150.75);
        
        SellConfirmation confirmation = service.sell("AAPL", 50, 155.25);

        assertEquals(155.25, confirmation.finalSalePrice);
        assertTrue(confirmation.finalSalePriceGbp.contains("155.25"));
    }

    @Test
    public void confirmationWithSmallPrices() {
        service.buy("PENNY", 10000, 0.50);
        
        SellConfirmation confirmation = service.sell("PENNY", 5000, 0.75);

        assertEquals(0.75, confirmation.finalSalePrice);
        assertTrue(confirmation.totalEarningsGbp.contains("3,750")); // 5000 * 0.75
    }

    @Test
    public void confirmationGBPFormattingIsConsistent() {
        service.buy("AAPL", 100, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 100, 160.0);

        assertTrue(confirmation.finalSalePriceGbp.startsWith("£"));
        assertTrue(confirmation.totalEarningsGbp.startsWith("£"));
        assertFalse(confirmation.finalSalePriceGbp.contains("$"));
        assertFalse(confirmation.totalEarningsGbp.contains("$"));
    }

    // ===== Tests for confirmation state and immutability =====

    @Test
    public void confirmationDataRemainsConsistent() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);
        
        // Verify data doesn't change on repeated access
        String message1 = confirmation.successMessage;
        String message2 = confirmation.successMessage;
        
        assertEquals(message1, message2);
    }

    @Test
    public void multipleSalesReturnDifferentConfirmations() {
        service.buy("AAPL", 20, 150.0);
        service.buy("GOOGL", 10, 2800.0);
        
        SellConfirmation aapl = service.sell("AAPL", 10, 155.0);
        SellConfirmation googl = service.sell("GOOGL", 5, 2850.0);

        assertEquals("AAPL", aapl.ticker);
        assertEquals("GOOGL", googl.ticker);
        assertNotEquals(aapl.successMessage, googl.successMessage);
    }

    // ===== Tests for confirmation with validation edge cases =====

    @Test
    public void confirmationAfterLegalSaleWithMinimalShares() {
        service.buy("AAPL", 1, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 1, 155.0);

        assertEquals(1, confirmation.shares);
        assertNotNull(confirmation.successMessage);
        assertTrue(confirmation.successMessage.contains("1"));
    }

    @Test
    public void sellWithInsufficientHoldingsProcessesAnyway() {
        service.buy("AAPL", 10, 150.0);
        int balanceBefore = account.getBalance();

        SellConfirmation confirmation = service.sell("AAPL", 20, 155.0);

        assertNotNull(confirmation);
        assertEquals(20, confirmation.shares);
        assertEquals(155.0, confirmation.finalSalePrice);
        assertEquals(balanceBefore + (int) Math.round(20 * 155.0), account.getBalance());
        assertNull(service.getPosition("AAPL"));
    }

    @Test
    public void sellForNonExistentStockProcessesAnyway() {
        int balanceBefore = account.getBalance();

        SellConfirmation confirmation = service.sell("NONEXIST", 10, 150.0);

        assertNotNull(confirmation);
        assertEquals("NONEXIST", confirmation.ticker);
        assertEquals(10, confirmation.shares);
        assertEquals(balanceBefore + (int) Math.round(10 * 150.0), account.getBalance());
    }

}

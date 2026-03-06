package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import static org.junit.jupiter.api.Assertions.*;

public class SellConfirmationControllerTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(500000);
        service = new InvestmentService(account);
    }

    // ===== Tests for POST /sell endpoint response =====

    @Test
    public void sellEndpointReturnsConfirmationObject() {
        service.buy("AAPL", 10, 150.0);
        TradeRequest request = new TradeRequest();
        request.ticker = "AAPL";
        request.shares = 5;
        request.price = 155.0;

        Object response = service.sell(request.ticker, request.shares, request.price);

        assertTrue(response instanceof SellConfirmation);
    }

    @Test
    public void sellEndpointReturnsCompleteConfirmation() {
        service.buy("GOOGL", 10, 2800.0);
        TradeRequest request = new TradeRequest();
        request.ticker = "GOOGL";
        request.shares = 5;
        request.price = 2850.0;

        SellConfirmation confirmation = service.sell(request.ticker, request.shares, request.price);

        assertNotNull(confirmation.successMessage);
        assertNotNull(confirmation.ticker);
        assertNotNull(confirmation.shares);
        assertNotNull(confirmation.finalSalePrice);
        assertNotNull(confirmation.finalSalePriceGbp);
        assertNotNull(confirmation.totalEarningsGbp);
        assertNotNull(confirmation.portfolioUrl);
        assertNotNull(confirmation.transactionHistoryUrl);
    }

    // ===== Tests for success message in endpoint response =====

    @Test
    public void endpointResponseIncludesSuccessMessage() {
        service.buy("AAPL", 20, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 10, 160.0);

        assertNotNull(confirmation.successMessage);
        assertTrue(confirmation.successMessage.length() > 0);
    }

    @Test
    public void successMessageInResponseIsReadable() {
        service.buy("MSFT", 50, 300.0);
        
        SellConfirmation confirmation = service.sell("MSFT", 25, 310.0);

        String message = confirmation.successMessage;
        assertNotNull(message);
        assertTrue(message.contains("Sale") || message.contains("sale") ||
                  message.contains("Sold") || message.contains("sold"),
                  "Message should indicate a sale occurred");
    }

    @Test
    public void successMessageFormatIsUserFriendly() {
        service.buy("AAPL", 100, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 50, 155.0);

        String message = confirmation.successMessage;
        
        // Should be a complete sentence with punctuation
        assertTrue(message.endsWith(".") || message.endsWith(":"));
        assertTrue(message.length() > 20, "Message should be descriptive");
    }

    // ===== Tests for confirmation details in response =====

    @Test
    public void endpointConfirmationIncludesTickerSymbol() {
        service.buy("GOOGL", 15, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 7, 2900.0);

        assertEquals("GOOGL", confirmation.ticker);
    }

    @Test
    public void endpointConfirmationIncludesSharesCount() {
        service.buy("MSFT", 100, 300.0);
        
        SellConfirmation confirmation = service.sell("MSFT", 35, 310.0);

        assertEquals(35, confirmation.shares);
    }

    @Test
    public void endpointConfirmationIncludesFinalSalePrice() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 165.50);

        assertEquals(165.50, confirmation.finalSalePrice);
    }

    @Test
    public void endpointConfirmationIncludesPriceInGBP() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 160.0);

        assertNotNull(confirmation.finalSalePriceGbp);
        assertTrue(confirmation.finalSalePriceGbp.startsWith("£"));
    }

    @Test
    public void endpointConfirmationIncludesTotalEarnings() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 10, 155.0);

        assertNotNull(confirmation.totalEarningsGbp);
        assertTrue(confirmation.totalEarningsGbp.startsWith("£"));
    }

    @Test
    public void totalEarningsCalculatedCorrectly() {
        service.buy("AAPL", 100, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 50, 160.0);

        // 50 * 160 = 8000
        assertTrue(confirmation.totalEarningsGbp.contains("8,000"));
    }

    // ===== Tests for navigation URLs in response =====

    @Test
    public void endpointConfirmationIncludesPortfolioUrl() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertNotNull(confirmation.portfolioUrl);
        assertEquals("/investments/positions", confirmation.portfolioUrl);
    }

    @Test
    public void endpointConfirmationIncludesTransactionHistoryUrl() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertNotNull(confirmation.transactionHistoryUrl);
        assertEquals("/investments/transactions", confirmation.transactionHistoryUrl);
    }

    @Test
    public void navigationUrlsAreValidEndpoints() {
        service.buy("GOOGL", 20, 2800.0);
        
        SellConfirmation confirmation = service.sell("GOOGL", 10, 2850.0);

        assertTrue(confirmation.portfolioUrl.startsWith("/"));
        assertTrue(confirmation.transactionHistoryUrl.startsWith("/"));
        assertTrue(confirmation.portfolioUrl.contains("positions"));
        assertTrue(confirmation.transactionHistoryUrl.contains("transactions"));
    }

    @Test
    public void portfolioUrlAllowsUserToViewUpdatedHoldings() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertEquals("/investments/positions", confirmation.portfolioUrl);
        // After clicking this URL, user should see updated positions
    }

    @Test
    public void transactionHistoryUrlAllowsUserToViewTradeHistory() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        assertEquals("/investments/transactions", confirmation.transactionHistoryUrl);
        // After clicking this URL, user should see both buy and sell transactions
    }

    // ===== Tests for confirmation data accuracy =====

    @Test
    public void confirmationDataMatchesRequestParameters() {
        service.buy("MSFT", 50, 300.0);
        TradeRequest request = new TradeRequest();
        request.ticker = "MSFT";
        request.shares = 20;
        request.price = 310.0;

        SellConfirmation confirmation = service.sell(request.ticker, request.shares, request.price);

        assertEquals(request.ticker, confirmation.ticker);
        assertEquals(request.shares, confirmation.shares);
        assertEquals(request.price, confirmation.finalSalePrice);
    }

    @Test
    public void confirmationForDifferentStocks() {
        service.buy("AAPL", 50, 150.0);
        service.buy("GOOGL", 20, 2800.0);

        SellConfirmation aapl = service.sell("AAPL", 10, 155.0);
        SellConfirmation googl = service.sell("GOOGL", 5, 2850.0);

        assertEquals("AAPL", aapl.ticker);
        assertEquals("GOOGL", googl.ticker);
        assertNotEquals(aapl.successMessage, googl.successMessage);
    }

    @Test
    public void confirmationMessageContainsAllDetails() {
        service.buy("AAPL", 100, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 50, 155.0);

        String message = confirmation.successMessage;
        
        // Verify message contains all key details
        assertTrue(message.contains("AAPL"), "Message must include stock symbol");
        assertTrue(message.contains("50"), "Message must include number of shares");
        assertTrue(message.contains("155") || message.contains("£155"), "Message must include sale price");
        assertTrue(message.contains("7,750") || message.contains("7750"), "Message must include total earnings");
    }

    // ===== Tests for confirmation with various transaction sizes =====

    @Test
    public void confirmationForSmallTrade() {
        service.buy("AAPL", 5, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 1, 155.0);

        assertEquals(1, confirmation.shares);
        assertNotNull(confirmation.successMessage);
    }

    @Test
    public void confirmationForLargeTrade() {
        service.buy("AAPL", 5000, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 2000, 155.0);

        assertEquals(2000, confirmation.shares);
        assertTrue(confirmation.totalEarningsGbp.contains("310,000")); // 2000 * 155
    }

    @Test
    public void confirmationForHighValueTrade() {
        service.buy("BRK.B", 10, 450000.0);
        
        SellConfirmation confirmation = service.sell("BRK.B", 5, 455000.0);

        assertEquals(5, confirmation.shares);
        assertTrue(confirmation.totalEarningsGbp.contains("2,275,000")); // 5 * 455000
    }

    // ===== Tests for confirmation response format consistency =====

    @Test
    public void allConfirmationResponsesHaveSameStructure() {
        service.buy("AAPL", 50, 150.0);
        service.buy("GOOGL", 20, 2800.0);
        service.buy("MSFT", 30, 300.0);

        SellConfirmation c1 = service.sell("AAPL", 10, 155.0);
        SellConfirmation c2 = service.sell("GOOGL", 5, 2850.0);
        SellConfirmation c3 = service.sell("MSFT", 10, 310.0);

        // All should have same structure
        assertNotNull(c1.successMessage);
        assertNotNull(c2.successMessage);
        assertNotNull(c3.successMessage);
        
        assertTrue(c1.portfolioUrl.equals("/investments/positions"));
        assertTrue(c2.portfolioUrl.equals("/investments/positions"));
        assertTrue(c3.portfolioUrl.equals("/investments/positions"));
    }

    @Test
    public void confirmationResponseIsSerializable() {
        service.buy("AAPL", 10, 150.0);
        
        SellConfirmation confirmation = service.sell("AAPL", 5, 155.0);

        // Can access all fields without exception
        String serialized = confirmation.successMessage + "|" + 
                           confirmation.ticker + "|" +
                           confirmation.shares + "|" +
                           confirmation.finalSalePriceGbp + "|" +
                           confirmation.totalEarningsGbp + "|" +
                           confirmation.portfolioUrl + "|" +
                           confirmation.transactionHistoryUrl;

        assertTrue(serialized.length() > 0);
    }

}

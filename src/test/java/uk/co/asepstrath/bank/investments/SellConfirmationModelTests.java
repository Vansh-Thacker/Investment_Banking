package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import static org.junit.jupiter.api.Assertions.*;

public class SellConfirmationModelTests {

    // ===== Tests for SellConfirmation object structure =====

    @Test
    public void sellConfirmationHasAllRequiredFields() {
        SellConfirmation confirmation = new SellConfirmation();
        
        // Set all required fields
        confirmation.successMessage = "Sale successful. Sold 10 shares of AAPL at £150.00 for total earnings £1,500.00.";
        confirmation.ticker = "AAPL";
        confirmation.shares = 10;
        confirmation.finalSalePrice = 150.0;
        confirmation.finalSalePriceGbp = "£150.00";
        confirmation.totalEarningsGbp = "£1,500.00";
        confirmation.portfolioUrl = "/investments/positions";
        confirmation.transactionHistoryUrl = "/investments/transactions";

        assertNotNull(confirmation.successMessage);
        assertNotNull(confirmation.ticker);
        assertNotNull(confirmation.shares);
        assertNotNull(confirmation.finalSalePrice);
        assertNotNull(confirmation.finalSalePriceGbp);
        assertNotNull(confirmation.totalEarningsGbp);
        assertNotNull(confirmation.portfolioUrl);
        assertNotNull(confirmation.transactionHistoryUrl);
    }

    @Test
    public void sellConfirmationCanBePopulatedWithData() {
        SellConfirmation confirmation = new SellConfirmation();
        
        confirmation.ticker = "GOOGL";
        confirmation.shares = 5;
        confirmation.finalSalePrice = 2800.0;
        confirmation.finalSalePriceGbp = "£2,800.00";
        confirmation.totalEarningsGbp = "£14,000.00";

        assertEquals("GOOGL", confirmation.ticker);
        assertEquals(5, confirmation.shares);
        assertEquals(2800.0, confirmation.finalSalePrice);
        assertEquals("£2,800.00", confirmation.finalSalePriceGbp);
        assertEquals("£14,000.00", confirmation.totalEarningsGbp);
    }

    @Test
    public void sellConfirmationNavigationUrlsAreSet() {
        SellConfirmation confirmation = new SellConfirmation();
        
        confirmation.portfolioUrl = "/investments/positions";
        confirmation.transactionHistoryUrl = "/investments/transactions";

        assertEquals("/investments/positions", confirmation.portfolioUrl);
        assertEquals("/investments/transactions", confirmation.transactionHistoryUrl);
    }

}

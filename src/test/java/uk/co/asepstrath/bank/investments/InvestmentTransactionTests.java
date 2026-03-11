package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvestmentTransactionTests {

    @Test
    public void getTotalCostGbpCalculatesCorrectly() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("1,500"));
    }

    @Test
    public void getTotalCostGbpFormatsInGBP() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 5;
        tx.price = 2800.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("14,000"));
        assertTrue(totalCost.startsWith("£"));
    }

    @Test
    public void getTotalCostGbpWithDecimalPrice() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 100;
        tx.price = 3.50;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("350"));
    }

    @Test
    public void getTotalCostGbpWithZeroSharesReturnsZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 0;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void getTotalCostGbpWithZeroPriceReturnsZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = 0.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void getTotalCostGbpWithNegativeSharesReturnsZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = -10;
        tx.price = 150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void getTotalCostGbpWithNegativePriceReturnsZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.shares = 10;
        tx.price = -150.0;

        String totalCost = tx.getTotalCostGbp();

        assertTrue(totalCost.contains("0"));
    }

    @Test
    public void transactionHasCorrectTypeForBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.ticker = "AAPL";
        tx.shares = 10;
        tx.price = 150.0;

        assertEquals("BUY", tx.type);
        assertEquals("AAPL", tx.ticker);
        assertEquals(10, tx.shares);
        assertEquals(150.0, tx.price);
    }

    @Test
    public void transactionHasCorrectTypeForSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.ticker = "GOOGL";
        tx.shares = 5;
        tx.price = 2800.0;

        assertEquals("SELL", tx.type);
        assertEquals("GOOGL", tx.ticker);
        assertEquals(5, tx.shares);
        assertEquals(2800.0, tx.price);
    }

    @Test
    public void transactionTimestampCanBeSet() {
        InvestmentTransaction tx = new InvestmentTransaction();
        assertNull(tx.timestamp);

        tx.timestamp = java.time.LocalDateTime.now();
        assertNotNull(tx.timestamp);
    }

    @Test
    public void transactionProceedsCanBeSet() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.proceeds = 7750.0; // From selling 5 shares at 1550 each

        assertEquals(7750.0, tx.proceeds);
    }

}

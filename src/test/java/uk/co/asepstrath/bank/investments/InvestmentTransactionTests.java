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

    // ===== getTotalEarningsSigned: positive for SELL, negative for BUY =====

    @Test
    public void getTotalEarningsSignedNegativeForBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 10;
        tx.price = 150.0;

        double signed = tx.getTotalEarningsSigned();

        assertEquals(-1500.0, signed, 0.01);
    }

    @Test
    public void getTotalEarningsSignedPositiveForSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 5;
        tx.price = 155.0;
        tx.proceeds = 775.0;

        double signed = tx.getTotalEarningsSigned();

        assertEquals(775.0, signed, 0.01);
    }

    @Test
    public void getTotalEarningsSignedZeroForZeroShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 0;
        tx.price = 150.0;

        assertEquals(0.0, tx.getTotalEarningsSigned(), 0.01);
    }

    @Test
    public void getTotalEarningsSignedUsesProceedsForSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 4;
        tx.price = 110.0;
        tx.proceeds = 440.0;

        assertEquals(440.0, tx.getTotalEarningsSigned(), 0.01);
    }

    // ===== getTotalEarningsGbp: formatted in GBP with sign =====

    @Test
    public void getTotalEarningsGbpNegativeForBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 10;
        tx.price = 150.0;

        String gbp = tx.getTotalEarningsGbp();

        assertTrue(gbp.contains("1,500"));
        assertTrue(gbp.startsWith("-") || gbp.contains("1,500"));
        assertTrue(gbp.contains("£"));
    }

    @Test
    public void getTotalEarningsGbpPositiveForSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 5;
        tx.proceeds = 775.0;

        String gbp = tx.getTotalEarningsGbp();

        assertTrue(gbp.contains("775"));
        assertTrue(gbp.startsWith("£"));
    }

    @Test
    public void getTotalEarningsGbpFormattedInGBP() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 100;
        tx.proceeds = 15500.0;

        String gbp = tx.getTotalEarningsGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("15,500"));
    }

    @Test
    public void getTotalEarningsGbpZeroWhenInvalid() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 0;
        tx.price = 150.0;

        String gbp = tx.getTotalEarningsGbp();

        assertTrue(gbp.contains("0"));
    }

    // ===== getSharesSigned: + for BUY, - for SELL =====

    @Test
    public void getSharesSignedPositiveForBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 10;

        assertEquals(10, tx.getSharesSigned());
    }

    @Test
    public void getSharesSignedNegativeForSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 5;

        assertEquals(-5, tx.getSharesSigned());
    }

    @Test
    public void getSharesSignedZeroWhenZeroShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 0;

        assertEquals(0, tx.getSharesSigned());
    }

    @Test
    public void getSharesSignedZeroForSellWithZeroShares() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 0;

        assertEquals(0, tx.getSharesSigned());
    }

    // ===== getPricePerShareGbp: formatted in GBP =====

    @Test
    public void getPricePerShareGbpFormatsPrice() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.price = 150.50;

        String gbp = tx.getPricePerShareGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("150.50") || gbp.contains("150,50"));
    }

    @Test
    public void getPricePerShareGbpZeroWhenPriceZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.price = 0;

        String gbp = tx.getPricePerShareGbp();

        assertTrue(gbp.contains("0"));
        assertTrue(gbp.startsWith("£"));
    }
}

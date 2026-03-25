package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class InvestmentTransactionTest {

    /* ============================================================
       SHARES SIGNED
       ============================================================ */

    @Test
    void testSharesSignedBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 5;

        assertEquals(5, tx.getSharesSigned());
    }

    @Test
    void testSharesSignedSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 5;

        assertEquals(-5, tx.getSharesSigned());
    }

    @Test
    void testSharesSignedZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 0;

        assertEquals(0, tx.getSharesSigned());
    }

    /* ============================================================
       PRICE PER SHARE GBP
       ============================================================ */

    @Test
    void testPricePerShareGbp() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.price = 12.34;

        assertEquals("£12.34", tx.getPricePerShareGbp());
    }

    @Test
    void testPricePerShareZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.price = 0;

        assertEquals("£0.00", tx.getPricePerShareGbp());
    }

    /* ============================================================
       TOTAL EARNINGS SIGNED
       ============================================================ */

    @Test
    void testTotalEarningsSignedBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 3;
        tx.price = 10.0;

        assertEquals(-30.0, tx.getTotalEarningsSigned());
    }

    @Test
    void testTotalEarningsSignedSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.shares = 3;
        tx.price = 10.0;
        tx.proceeds = 30.0;

        assertEquals(30.0, tx.getTotalEarningsSigned());
    }

    @Test
    void testTotalEarningsSignedZero() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 0;
        tx.price = 10.0;

        assertEquals(0.0, tx.getTotalEarningsSigned());
    }

    /* ============================================================
       TOTAL EARNINGS GBP
       ============================================================ */

    @Test
    void testTotalEarningsGbpBuy() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.shares = 2;
        tx.price = 10.0;

        assertEquals("-£20.00", tx.getTotalEarningsGbp());
    }

    @Test
    void testTotalEarningsGbpSell() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.proceeds = 45.0;

        assertEquals("£45.00", tx.getTotalEarningsGbp());
    }

    /* ============================================================
       POSITION CLOSED
       ============================================================ */

    @Test
    void testPositionClosedTrue() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.sharesAfter = 0;

        assertTrue(tx.isPositionClosed());
    }

    @Test
    void testPositionClosedFalse() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.sharesAfter = 5;

        assertFalse(tx.isPositionClosed());
    }

    @Test
    void testPositionClosedBuyAlwaysFalse() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "BUY";
        tx.sharesAfter = 0;

        assertFalse(tx.isPositionClosed());
    }

    /* ============================================================
       TIMESTAMP FORMATTING
       ============================================================ */

    @Test
    void testTimestampFormatted() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = LocalDateTime.of(2024, 1, 1, 12, 0);

        assertEquals("2024-01-01T12:00", tx.getTimestampFormatted());
    }

    @Test
    void testTimestampFormattedNull() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = null;

        assertEquals("", tx.getTimestampFormatted());
    }

    @Test
    void testFieldsAndGetters() {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.timestamp = LocalDateTime.of(2025, 2, 3, 10, 0);
        tx.ticker = "ABC";
        tx.type = "SELL";
        tx.shares = 5;
        tx.price = 100.0;
        tx.proceeds = 500.0;
        tx.realizedGainLoss = 50.0;
        tx.formattedTimestamp = "03 Feb 2025 10:00";

        assertEquals("ABC", tx.getTicker());
        assertEquals("SELL", tx.getType());
        assertEquals(5, tx.getShares());
        assertEquals(100.0, tx.getPrice());
        assertEquals(500.0, tx.getProceeds());
        assertEquals(50.0, tx.getRealizedGainLoss());
        assertEquals("03 Feb 2025 10:00", tx.getFormattedTimestamp());
    }

}
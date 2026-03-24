package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    void testAverageBookPriceFormatting() {
        Position p = new Position();
        p.averageBookPrice = 1234; // pence

        assertEquals("£12.34", p.getAverageBookPrice());
    }

    @Test
    void testTotalInvestedFormatting() {
        Position p = new Position();
        p.totalInvested = 9876;

        assertEquals("£98.76", p.getTotalInvested());
    }

    @Test
    void testCurrentPriceFormatting() {
        Position p = new Position();
        p.currentPrice = 50.5;

        assertEquals("£50.50", p.getCurrentPrice());
    }

    @Test
    void testCurrentPriceNull() {
        Position p = new Position();
        p.currentPrice = null;

        assertEquals("£0.00", p.getCurrentPrice());
    }

    @Test
    void testCurrentValueFormatting() {
        Position p = new Position();
        p.currentValue = 123.45;

        assertEquals("£123.45", p.getCurrentValue());
    }

    @Test
    void testCurrentValueNull() {
        Position p = new Position();
        p.currentValue = null;

        assertEquals("£0.00", p.getCurrentValue());
    }

    @Test
    void testUnrealizedPnLPositive() {
        Position p = new Position();
        p.totalInvested = 10000; // £100
        p.currentValue = 150.0;  // £150

        assertEquals(50.0, p.getUnrealizedPnL());
    }

    @Test
    void testUnrealizedPnLNegative() {
        Position p = new Position();
        p.totalInvested = 20000; // £200
        p.currentValue = 150.0;  // £150

        assertEquals(-50.0, p.getUnrealizedPnL());
    }

    @Test
    void testUnrealizedPnLNull() {
        Position p = new Position();
        p.currentValue = null;

        assertNull(p.getUnrealizedPnL());
    }

    @Test
    void testUnrealizedPnLPercentPositive() {
        Position p = new Position();
        p.totalInvested = 10000; // £100
        p.currentValue = 150.0;  // £150

        assertEquals(50.0, p.getUnrealizedPnLPercent());
    }

    @Test
    void testUnrealizedPnLPercentNegative() {
        Position p = new Position();
        p.totalInvested = 20000; // £200
        p.currentValue = 150.0;  // £150

        assertEquals(-25.0, p.getUnrealizedPnLPercent());
    }

    @Test
    void testUnrealizedPnLPercentNull() {
        Position p = new Position();
        p.currentValue = null;

        assertNull(p.getUnrealizedPnLPercent());
    }

    @Test
    void testUnrealizedPnLGbpFormatting() {
        Position p = new Position();
        p.totalInvested = 10000; // £100
        p.currentValue = 150.0;  // £150

        assertEquals("£50.00", p.getUnrealizedPnLGbp());
    }

    @Test
    void testUnrealizedPnLGbpNull() {
        Position p = new Position();
        p.currentValue = null;

        assertEquals("—", p.getUnrealizedPnLGbp());
    }

    @Test
    void testUnrealizedPnLPercentFormatted() {
        Position p = new Position();
        p.totalInvested = 10000; // £100
        p.currentValue = 150.0;  // £150

        assertEquals("50.00%", p.getUnrealizedPnLPercentFormatted());
    }

    @Test
    void testUnrealizedPnLPercentFormattedNull() {
        Position p = new Position();
        p.currentValue = null;

        assertEquals("—", p.getUnrealizedPnLPercentFormatted());
    }
}
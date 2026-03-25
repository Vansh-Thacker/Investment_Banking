package uk.co.asepstrath.bank.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioBreakdownTest {

    @Test
    @DisplayName("Constructor stores label correctly")
    void testLabel() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Technology", 10000.0, 25.0);
        assertEquals("Technology", bd.label);
    }

    @Test
    @DisplayName("Constructor stores totalValue correctly")
    void testTotalValue() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Energy", 50000.0, 50.0);
        assertEquals(50000.0, bd.totalValue, 0.001);
    }

    @Test
    @DisplayName("Constructor stores percentage correctly")
    void testPercentage() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Finance", 75000.0, 75.0);
        assertEquals(75.0, bd.percentage, 0.001);
    }

    @Test
    @DisplayName("Handles zero totalValue")
    void testZeroTotalValue() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Unknown", 0.0, 0.0);
        assertEquals(0.0, bd.totalValue, 0.001);
        assertEquals(0.0, bd.percentage, 0.001);
    }

    @Test
    @DisplayName("Handles Other label for grouped small segments")
    void testOtherLabel() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Other", 2500.0, 2.5);
        assertEquals("Other", bd.label);
        assertEquals(2.5, bd.percentage, 0.001);
    }

    @Test
    @DisplayName("Handles large marketCap values")
    void testLargeMarketCap() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Finance", 48000000000.0, 60.0);
        assertEquals(48000000000.0, bd.totalValue, 1.0);
    }

    @Test
    @DisplayName("Two breakdowns with same label are independent objects")
    void testTwoBreakdownsAreIndependent() {
        PortfolioBreakdown a = new PortfolioBreakdown("Finance", 10000.0, 25.0);
        PortfolioBreakdown b = new PortfolioBreakdown("Finance", 20000.0, 50.0);
        assertNotEquals(a.totalValue, b.totalValue);
        assertNotEquals(a.percentage, b.percentage);
    }
}
package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for Position, including average purchase price (GBP) display. */
public class PositionTests {

    @Test
    public void getAveragePurchasePriceGbpFormatsInGBP() {
        Position p = new Position();
        p.ticker = "AAPL";
        p.shares = 10;
        p.averageBookPrice = 150;
        p.totalInvested = 1500;

        String gbp = p.getAveragePurchasePriceGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("150"));
    }

    @Test
    public void getAveragePurchasePriceGbpZeroWhenAverageZero() {
        Position p = new Position();
        p.ticker = "XYZ";
        p.shares = 0;
        p.averageBookPrice = 0;
        p.totalInvested = 0;

        String gbp = p.getAveragePurchasePriceGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("0"));
    }

    @Test
    public void getAveragePurchasePriceGbpFormatsLargeAmount() {
        Position p = new Position();
        p.averageBookPrice = 2800;

        String gbp = p.getAveragePurchasePriceGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("2,800") || gbp.contains("2800"));
    }

    // ===== getTotalInvestedGbp: total price of units held (holdings table) =====

    @Test
    public void getTotalInvestedGbpFormatsInGBP() {
        Position p = new Position();
        p.ticker = "MSFT";
        p.shares = 20;
        p.totalInvested = 6000;

        String gbp = p.getTotalInvestedGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("6,000") || gbp.contains("6000"));
    }

    @Test
    public void getTotalInvestedGbpZeroWhenNoInvestment() {
        Position p = new Position();
        p.totalInvested = 0;

        String gbp = p.getTotalInvestedGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("0"));
    }

    // ===== currentPrice / currentValue / getCurrentValueGbp (§3 – current prices at request time) =====

    @Test
    public void getCurrentValueGbpFormatsWhenCurrentValueSet() {
        Position p = new Position();
        p.shares = 10;
        p.currentValue = 1550.0;

        String gbp = p.getCurrentValueGbp();

        assertTrue(gbp.startsWith("£"));
        assertTrue(gbp.contains("1,550") || gbp.contains("1550"));
    }

    @Test
    public void getCurrentValueGbpReturnsDashWhenCurrentValueNull() {
        Position p = new Position();
        p.currentValue = null;

        assertEquals("—", p.getCurrentValueGbp());
    }

    // ===== §3 Unrealized P&L: book value, P&L, P&L % =====

    @Test
    public void getBookValueIsSharesTimesAverageBookPrice() {
        Position p = new Position();
        p.shares = 20;
        p.averageBookPrice = 150;

        assertEquals(3000, p.getBookValue());
    }

    @Test
    public void getUnrealizedPnLWhenCurrentValueAvailable() {
        Position p = new Position();
        p.shares = 10;
        p.averageBookPrice = 100;
        p.currentValue = 1100.0; // book 1000, gain 100

        assertEquals(100.0, p.getUnrealizedPnL(), 0.01);
    }

    @Test
    public void getUnrealizedPnLNullWhenCurrentValueNull() {
        Position p = new Position();
        p.currentValue = null;

        assertNull(p.getUnrealizedPnL());
    }

    @Test
    public void getUnrealizedPnLPercentWhenBookValuePositive() {
        Position p = new Position();
        p.shares = 10;
        p.averageBookPrice = 100;
        p.currentValue = 110.0 * 10; // 1100, book 1000, +10%

        assertEquals(10.0, p.getUnrealizedPnLPercent(), 0.01);
    }

    @Test
    public void getUnrealizedPnLPercentNullWhenBookValueZero() {
        Position p = new Position();
        p.shares = 0;
        p.averageBookPrice = 150;
        p.currentValue = 0.0;

        assertNull(p.getUnrealizedPnLPercent());
    }
}

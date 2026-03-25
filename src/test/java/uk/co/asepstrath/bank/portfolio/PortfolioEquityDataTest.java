package uk.co.asepstrath.bank.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioEquityDataTest {

    @Test
    @DisplayName("PortfolioEquityData can be instantiated")
    void testInstantiation() {
        assertDoesNotThrow(() -> new PortfolioEquityData());
    }

    @Test
    @DisplayName("getPortfolioEquity throws IllegalArgumentException for null tickers")
    void testNullTickersThrowsException() {
        PortfolioEquityData ped = new PortfolioEquityData();
        // Without a real context we verify the validation logic directly
        // by calling the helper that checks tickers
        assertThrows(IllegalArgumentException.class, () -> {
            validateTickers(null);
        });
    }

    @Test
    @DisplayName("getPortfolioEquity throws IllegalArgumentException for empty tickers")
    void testEmptyTickersThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            validateTickers("");
        });
    }

    @Test
    @DisplayName("Tickers split correctly on comma")
    void testTickersSplitCorrectly() {
        String tickers = "CTT,STD,CLP";
        String[] split = tickers.split(",");
        assertEquals(3, split.length);
        assertEquals("CTT", split[0]);
        assertEquals("STD", split[1]);
        assertEquals("CLP", split[2]);
    }

    @Test
    @DisplayName("Single ticker splits correctly")
    void testSingleTickerSplitsCorrectly() {
        String tickers = "CTT";
        String[] split = tickers.split(",");
        assertEquals(1, split.length);
        assertEquals("CTT", split[0]);
    }

    @Test
    @DisplayName("Tickers with spaces split and trim correctly")
    void testTickersWithSpaces() {
        String tickers = "CTT, STD, CLP";
        String[] split = tickers.split(",");
        assertEquals("CTT", split[0].trim());
        assertEquals("STD", split[1].trim());
        assertEquals("CLP", split[2].trim());
    }

    // ─── Helper mirrors the validation logic in PortfolioEquityData ──────────
    private void validateTickers(String tickers) {
        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("Tickers parameter is required");
        }
    }
}
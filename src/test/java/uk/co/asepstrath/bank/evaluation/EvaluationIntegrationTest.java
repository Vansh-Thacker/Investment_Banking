package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationIntegrationTest {

    private Path tempPriceCsv;
    private CurrentPriceLoader loader;
    private CurrentPriceService service;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Create a REAL temporary CSV file to simulate an integration point
        tempPriceCsv = Files.createTempFile("currentprices", ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(tempPriceCsv)) {
            writer.write("Ticker,PricePence\n");
            writer.write("AAPL,15050\n");
            writer.write("TSLA,20000\n");
            writer.write("MSFT,30000\n");
        }

        // 2. Initialize the REAL loader with the REAL file
        // (This is integration because we are testing the Loader + File System)
        loader = new CurrentPriceLoader(tempPriceCsv.toString());

        // 3. Initialize the REAL service with the REAL loader
        // (Integration between Service and Loader layers)
        service = new CurrentPriceService(loader);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up the integration file
        Files.deleteIfExists(tempPriceCsv);
    }

    @Test
    void testPriceServiceIntegrationWithFile() {
        // Test Case 1: Known ticker in the file
        int aaplPrice = service.getPricePence("AAPL");
        assertEquals(15050, aaplPrice, "Service should return the price parsed from the CSV file");

        // Test Case 2: Case sensitivity (Service usually handles normalization)
        int tslaPrice = service.getPricePence("tsla");
        assertEquals(20000, tslaPrice, "Service should handle lowercase tickers by delegating to loader");

        // Test Case 3: Missing ticker (Verify the fallback logic across layers)
        int missingPrice = service.getPricePence("GOOG");
        assertEquals(0, missingPrice, "Service should return 0 if ticker is not in the CSV file");
    }

    @Test
    void testEquitiesAndPriceIntegration() {
        // This tests how the whole 'evaluation' package works together
        EquitiesService equitiesService = new EquitiesService();

        EquityRecord apple = new EquityRecord();
        apple.ticker = "AAPL";
        apple.name = "Apple Inc.";
        equitiesService.addEquity(apple);

        // Integrate Name lookup with Price lookup
        String displayName = equitiesService.getName("AAPL");
        int price = service.getPricePence("AAPL");

        assertEquals("Apple Inc.", displayName);
        assertEquals(15050, price);
    }
}
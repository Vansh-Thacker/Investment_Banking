package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CurrentPriceLoaderTest {

    @Test
    void testLoadsPricesCorrectly() throws Exception {
        Path temp = Files.createTempFile("prices", ".csv");
        try (FileWriter fw = new FileWriter(temp.toFile())) {
            fw.write("Ticker,Price\n");
            fw.write("ABC,1234\n");
            fw.write("XYZ,5678\n");
        }

        CurrentPriceLoader loader = new CurrentPriceLoader(temp.toString());

        assertEquals(1234, loader.getCurrentPrice("ABC"));
        assertEquals(5678, loader.getCurrentPrice("xyz")); // lowercase test
    }

    @Test
    void testMissingTickerReturnsNull() throws Exception {
        Path temp = Files.createTempFile("prices", ".csv");
        try (FileWriter fw = new FileWriter(temp.toFile())) {
            fw.write("Ticker,Price\n");
            fw.write("ABC,1234\n");
        }

        CurrentPriceLoader loader = new CurrentPriceLoader(temp.toString());
        assertNull(loader.getCurrentPrice("NOPE"));
    }
}
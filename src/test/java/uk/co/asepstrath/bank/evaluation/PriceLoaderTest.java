package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PriceLoaderTest {

    @Test
    void testLoadsHistoricalPrices() throws Exception {
        Path temp = Files.createTempFile("prices", ".csv");
        try (FileWriter fw = new FileWriter(temp.toFile())) {
            fw.write("Ticker,Timestamp,Price\n");
            fw.write("ABC,2024-01-01T10:00,10.5\n");
            fw.write("ABC,2024-01-01T11:00,11.0\n");
        }

        PriceLoader loader = new PriceLoader(temp.toString());

        Double price = loader.getPrice("ABC", LocalDateTime.parse("2024-01-01T10:30"));
        assertEquals(10.5, price);
    }

    @Test
    void testGetAllPricesForTicker() throws Exception {
        Path temp = Files.createTempFile("prices", ".csv");
        try (FileWriter fw = new FileWriter(temp.toFile())) {
            fw.write("Ticker,Timestamp,Price\n");
            fw.write("ABC,2024-01-01T10:00,10.5\n");
            fw.write("ABC,2024-01-01T11:00,11.0\n");
        }

        PriceLoader loader = new PriceLoader(temp.toString());

        assertEquals(2, loader.getAllPricesForTicker("ABC").size());
    }
}

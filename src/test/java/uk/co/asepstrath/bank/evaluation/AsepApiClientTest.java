package uk.co.asepstrath.bank.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AsepApiClientTest {

    @Test
    public void parseLatestPicksLastChronologicalPrice() {
        String json = """
                {
                  "prices": [
                    { "date": "2025-02-03", "price": 100 },
                    { "date": "2025-02-04", "price": 100.58 }
                  ]
                }
                """;
        AsepApiClient client = new AsepApiClient("http://localhost", new ObjectMapper());
        Optional<AsepLatestPrice> latest = client.parseLatest(json);
        assertTrue(latest.isPresent());
        assertEquals(100.58, latest.get().pricePerShare, 0.001);
        assertEquals("2025-02-04", latest.get().date);
    }

    @Test
    public void parseLatestIgnoresNonPositiveOrInvalidDates() {
        String json = """
                {
                  "prices": [
                    { "date": "2025-02-03", "price": -1 },
                    { "date": "not-a-date", "price": 50 },
                    { "date": "2025-02-05", "price": 42 }
                  ]
                }
                """;
        AsepApiClient client = new AsepApiClient("http://localhost", new ObjectMapper());
        Optional<AsepLatestPrice> latest = client.parseLatest(json);
        assertTrue(latest.isPresent());
        assertEquals(42.0, latest.get().pricePerShare, 0.001);
        assertEquals("2025-02-05", latest.get().date);
    }

    @Test
    public void parseLatestReturnsEmptyForEmptyArray() {
        AsepApiClient client = new AsepApiClient("http://localhost", new ObjectMapper());
        assertTrue(client.parseLatest("{\"prices\":[]}").isEmpty());
    }

    @Test
    public void parsePriorTradingDayEodPicksSecondToLastChronologicalPrice() {
        String json = """
                {
                  "prices": [
                    { "date": "2025-02-03", "price": 100 },
                    { "date": "2025-02-04", "price": 100.58 },
                    { "date": "2025-02-05", "price": 101 }
                  ]
                }
                """;
        AsepApiClient client = new AsepApiClient("http://localhost", new ObjectMapper());
        Optional<AsepLatestPrice> prior = client.parsePriorTradingDayEod(json);
        assertTrue(prior.isPresent());
        assertEquals(100.58, prior.get().pricePerShare, 0.001);
        assertEquals("2025-02-04", prior.get().date);
    }

    @Test
    public void parsePriorTradingDayEodReturnsEmptyWhenOnlyOneObservation() {
        String json = """
                { "prices": [ { "date": "2025-02-04", "price": 100.58 } ] }
                """;
        AsepApiClient client = new AsepApiClient("http://localhost", new ObjectMapper());
        assertTrue(client.parsePriorTradingDayEod(json).isEmpty());
    }
}

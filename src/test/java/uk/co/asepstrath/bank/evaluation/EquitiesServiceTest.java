package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquitiesServiceTest {

    @Test
    void testAddAndRetrieveEquity() {
        EquitiesService service = new EquitiesService();

        EquityRecord rec = new EquityRecord();
        rec.name = "Test Corp";
        rec.ticker = "TST";
        rec.sector = "Tech";

        service.addEquity(rec);

        EquityRecord out = service.getEquity("tst");
        assertNotNull(out);
        assertEquals("Test Corp", out.name);
    }

    @Test
    void testGetNameFallback() {
        EquitiesService service = new EquitiesService();

        assertEquals("ABC", service.getName("ABC")); // no record → fallback
    }
}
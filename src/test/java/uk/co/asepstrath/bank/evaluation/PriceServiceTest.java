package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceServiceTest {

    @Test
    void testAddAndRetrievePrice() {
        PriceService service = new PriceService();

        PriceRecord rec = new PriceRecord();
        rec.ticker = "ABC";
        rec.price = 1234;
        rec.date = "2024-01-01";

        service.addPrice(rec);

        PriceRecord out = service.getPrice("ABC");
        assertNotNull(out);
        assertEquals(1234, out.price);
    }

    @Test
    void testMissingTickerReturnsNull() {
        PriceService service = new PriceService();
        assertNull(service.getPrice("NOPE"));
    }
}

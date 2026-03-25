package uk.co.asepstrath.bank.capitalgains;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TaxYearInfoTest {

    @Test
    void testTaxYearBeforeApril6() {
        LocalDate d = LocalDate.of(2025, 3, 20);
        assertEquals(2024, TaxYearInfo.getTaxYearStart(d));
    }

    @Test
    void testTaxYearAfterApril6() {
        LocalDate d = LocalDate.of(2025, 4, 10);
        assertEquals(2025, TaxYearInfo.getTaxYearStart(d));
    }

    @Test
    void testContains() {
        TaxYearInfo y = new TaxYearInfo(2024);
        LocalDateTime dt = LocalDateTime.of(2024, 11, 1, 12, 0);
        assertTrue(y.contains(dt));
    }

    @Test
    void testLabelFormatting() {
        TaxYearInfo y = new TaxYearInfo(2024);
        assertEquals("2024/25", y.label);
    }
}
package uk.co.asepstrath.bank.capitalgains;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CapitalGainsSummaryTest {

    @Test
    void testSummaryValuesAndGetters() {
        CapitalGainsSummary s = new CapitalGainsSummary(
                100.0, 20.0, 80.0,
                3000.0, 0.0,
                0.0, 0.20, 0.0
        );

        assertEquals(100.0, s.getTotalGains());
        assertEquals(20.0, s.getTotalLosses());
        assertEquals(80.0, s.getNetGain());
        assertEquals(3000.0, s.getAllowanceUsed());
        assertEquals(0.0, s.getAllowanceRemaining());
        assertEquals(0.0, s.getTaxableGain());
        assertEquals(0.20, s.getTaxRate());
        assertEquals(0.0, s.getTaxOwed());
    }
}
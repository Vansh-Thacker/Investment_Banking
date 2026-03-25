package uk.co.asepstrath.bank.capitalgains;

import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.investments.InvestmentTransaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CapitalGainsServiceTest {

    private InvestmentService mockInvService() {
        return mock(InvestmentService.class);
    }

    @Test
    void testPriceCsvLoads() throws Exception {
        Path temp = Files.createTempFile("prices", ".csv");
        Files.writeString(temp, """
                ticker,timestamp,price
                ABC,2025-02-03T10:00,100.0
                """);

        CapitalGainsService svc = new CapitalGainsService(mockInvService());
        svc.loadPricesCsv(temp.toString());

        // If no exception thrown, CSV loaded successfully
        assertTrue(true);
    }

    @Test
    void testHistoryLoadsAndCreatesTransactions() throws Exception {
        Path prices = Files.createTempFile("prices", ".csv");
        Files.writeString(prices, """
            ticker,timestamp,price
            ABC,2025-02-03T10:00,100.0
            """);

        Path history = Files.createTempFile("history", ".csv");
        Files.writeString(history, """
            timestamp,type,ticker,amount
            2025-02-03T10:00,BUY,ABC,10000
            2025-02-03T10:00,SELL,ABC,12000
            """);

        CapitalGainsService svc = new CapitalGainsService(mockInvService());
        svc.loadPricesCsv(prices.toString());
        svc.loadHistoryCsv(history.toString());

        List<InvestmentTransaction> realised = svc.getRealisedTransactionsForYear(2024);

        assertEquals(1, realised.size());
        assertEquals("ABC", realised.get(0).getTicker());
        assertNotNull(realised.get(0).getRealizedGainLoss());
    }

    @Test
    void testSummaryCalculation() throws Exception {
        CapitalGainsService svc = new CapitalGainsService(mockInvService());

        // Inject fake SELL transaction
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.type = "SELL";
        tx.timestamp = LocalDateTime.of(2024, 6, 1, 12, 0);
        tx.realizedGainLoss = 500.0;

        var field = svc.getClass().getDeclaredField("transactions");
        field.setAccessible(true);
        List<InvestmentTransaction> list = (List<InvestmentTransaction>) field.get(svc);
        list.add(tx);

        CapitalGainsSummary summary = svc.getSummaryForYear(2024);

        assertEquals(500.0, summary.getTotalGains());
        assertEquals(0.0, summary.getTotalLosses());
        assertEquals(500.0, summary.getNetGain());
    }
}
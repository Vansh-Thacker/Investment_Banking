package uk.co.asepstrath.bank.investments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.EquityRecord;
import uk.co.asepstrath.bank.evaluation.EquitiesService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that transaction list enrichment sets stockName from EquitiesService (per GET /api/equities).
 * Uses the same enrichment logic as InvestmentController.transactions().
 */
public class TransactionListStockNameTests {

    private InvestmentService service;
    private Account account;
    private EquitiesService equitiesService;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(100_000);
        service = new InvestmentService(account);
        equitiesService = new EquitiesService();
    }

    @Test
    public void enrichmentSetsStockNameWhenEquityKnown() {
        EquityRecord apple = new EquityRecord();
        apple.name = "Apple Inc";
        apple.ticker = "AAPL";
        apple.sector = "Technology";
        equitiesService.addEquity(apple);

        service.buy("AAPL", 10, 150.0);
        List<InvestmentTransaction> result = service.getHistory();

        for (InvestmentTransaction tx : result) {
            tx.stockName = equitiesService.getName(tx.ticker);
        }

        assertEquals(1, result.size());
        assertEquals("Apple Inc", result.get(0).stockName);
        assertEquals("AAPL", result.get(0).ticker);
    }

    @Test
    public void enrichmentFallsBackToTickerWhenEquityUnknown() {
        service.buy("UNKN", 5, 100.0);
        List<InvestmentTransaction> result = service.getHistory();

        for (InvestmentTransaction tx : result) {
            tx.stockName = equitiesService.getName(tx.ticker);
        }

        assertEquals(1, result.size());
        assertEquals("UNKN", result.get(0).stockName);
    }

    @Test
    public void enrichmentMixesKnownAndUnknownTickers() {
        EquityRecord apple = new EquityRecord();
        apple.name = "Apple Inc";
        apple.ticker = "AAPL";
        apple.sector = "Technology";
        equitiesService.addEquity(apple);

        service.buy("AAPL", 10, 150.0);
        service.buy("XYZ", 5, 80.0);
        List<InvestmentTransaction> result = service.getHistory();

        for (InvestmentTransaction tx : result) {
            tx.stockName = equitiesService.getName(tx.ticker);
        }

        assertEquals(2, result.size());
        InvestmentTransaction aapl = result.stream().filter(t -> "AAPL".equals(t.ticker)).findFirst().orElseThrow();
        InvestmentTransaction xyz = result.stream().filter(t -> "XYZ".equals(t.ticker)).findFirst().orElseThrow();
        assertEquals("Apple Inc", aapl.stockName);
        assertEquals("XYZ", xyz.stockName);
    }
}

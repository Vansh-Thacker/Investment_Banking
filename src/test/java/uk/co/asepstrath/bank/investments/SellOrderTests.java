package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.StatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;

import javax.sql.DataSource;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the sell-order user story: select stock, enter shares, see current price, reject invalid amounts.
 */
public class SellOrderTests {

    private InvestmentService service;
    private Account account;

    @BeforeEach
    public void setUp() {
        account = new Account();
        account.deposit(500_000);
        service = new InvestmentService(account);
    }

    // ===== User can select a stock they own =====

    @Test
    public void positionsListContainsOnlyStocksUserOwns() {
        assertTrue(service.getPositions().isEmpty());

        service.buy("AAPL", 10, 150.0);
        service.buy("MSFT", 5, 300.0);
        Collection<Position> positions = service.getPositions();
        assertEquals(2, positions.size());
        assertTrue(positions.stream().anyMatch(p -> "AAPL".equals(p.ticker)));
        assertTrue(positions.stream().anyMatch(p -> "MSFT".equals(p.ticker)));

        service.sell("AAPL", 10, 155.0);
        positions = service.getPositions();
        assertEquals(1, positions.size());
        assertEquals("MSFT", positions.iterator().next().ticker);
    }

    @Test
    public void userWithNoPositionsGetsEmptyPositionsList() {
        assertTrue(service.getPositions().isEmpty());
    }

    // ===== System prevents negative or zero share amounts =====

    @Test
    public void sellPreviewRejectsZeroShares() {
        service.buy("AAPL", 10, 150.0);
        assertThrows(IllegalArgumentException.class,
                () -> service.getSalePreview("AAPL", 0, 155.0));
    }

    @Test
    public void sellPreviewRejectsNegativeShares() {
        service.buy("AAPL", 10, 150.0);
        assertThrows(IllegalArgumentException.class,
                () -> service.getSalePreview("AAPL", -1, 155.0));
    }

    @Test
    public void sellRejectsZeroShares() {
        service.buy("AAPL", 10, 150.0);
        assertThrows(IllegalArgumentException.class,
                () -> service.sell("AAPL", 0, 155.0));
    }

    @Test
    public void sellRejectsNegativeShares() {
        service.buy("AAPL", 10, 150.0);
        assertThrows(IllegalArgumentException.class,
                () -> service.sell("AAPL", -5, 155.0));
    }

    // ===== Fetch and show current market price before confirming =====

    @Test
    public void salePreviewShowsEstimatedPriceAndProceedsForConfirmation() {
        service.buy("AAPL", 100, 150.0);
        double currentMarketPrice = 155.50;
        int sharesToSell = 50;

        SalePreview preview = service.getSalePreview("AAPL", sharesToSell, currentMarketPrice);

        assertEquals("AAPL", preview.symbol);
        assertEquals(sharesToSell, preview.shares);
        assertEquals(currentMarketPrice, preview.estimatedPricePerShare);
        assertTrue(preview.estimatedTotalProceedsGbp.contains("7,775")); // 50 * 155.50
        assertNotNull(preview.estimatedTotalProceedsGbp);
        assertTrue(preview.estimatedTotalProceedsGbp.startsWith("£"));
    }

    @Test
    public void currentPriceEndpointReturnsPriceWhenAvailable() {
        PriceService priceService = new PriceService();
        PriceRecord record = new PriceRecord();
        record.ticker = "AAPL";
        record.price = 155;
        record.date = "2025-03-13";
        priceService.addPrice(record);

        Account acc = new Account();
        acc.deposit(100_000);
        DataSource ds = null;
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellOrderTests.class);
        InvestmentController controller = new InvestmentController(ds, log, acc, priceService);

        Context ctx = mock(Context.class);
        when(ctx.setResponseCode(any(StatusCode.class))).thenReturn(ctx);

        Object result = controller.currentPrice("AAPL", ctx);

        assertNotNull(result);
        assertTrue(result instanceof CurrentPriceResponse);
        CurrentPriceResponse resp = (CurrentPriceResponse) result;
        assertEquals("AAPL", resp.symbol);
        assertEquals(155.0, resp.price);
        assertEquals("2025-03-13", resp.date);
    }

    @Test
    public void currentPriceEndpointReturnsNotFoundWhenSymbolNotInPriceService() {
        PriceService priceService = new PriceService();
        Account acc = new Account();
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellOrderTests.class);
        InvestmentController controller = new InvestmentController(null, log, acc, priceService);

        Context ctx = mock(Context.class);
        when(ctx.setResponseCode(any(StatusCode.class))).thenReturn(ctx);

        Object result = controller.currentPrice("XYZ", ctx);

        ArgumentCaptor<StatusCode> statusCaptor = ArgumentCaptor.forClass(StatusCode.class);
        verify(ctx).setResponseCode(statusCaptor.capture());
        assertEquals(StatusCode.NOT_FOUND, statusCaptor.getValue());
        assertNotNull(result);
        assertTrue(result instanceof java.util.Map);
    }

    @Test
    public void currentPriceEndpointRejectsInvalidTickerFormat() {
        InvestmentController controller = new InvestmentController(null,
                org.slf4j.LoggerFactory.getLogger(SellOrderTests.class), new Account(), new PriceService());
        Context ctx = mock(Context.class);
        when(ctx.setResponseCode(any(StatusCode.class))).thenReturn(ctx);

        controller.currentPrice("invalid_ticker", ctx);

        ArgumentCaptor<StatusCode> statusCaptor = ArgumentCaptor.forClass(StatusCode.class);
        verify(ctx).setResponseCode(statusCaptor.capture());
        assertEquals(StatusCode.BAD_REQUEST, statusCaptor.getValue());
    }

    // ===== Correct SELL processing (cash, position, validation) =====

    @Test
    public void sellUpdatesCashBalanceCorrectly() {
        account.deposit(10_000);
        service.buy("AAPL", 20, 50.0); // cost 1000
        int balanceAfterBuy = account.getBalance();

        service.sell("AAPL", 5, 60.0); // proceeds 300
        assertEquals(balanceAfterBuy + 300, account.getBalance());
    }

    @Test
    public void sellDecreasesPositionCorrectly() {
        service.buy("AAPL", 20, 50.0);
        service.sell("AAPL", 5, 60.0);

        Position pos = service.getPosition("AAPL");
        assertNotNull(pos);
        assertEquals(15, pos.shares);
    }

    @Test
    public void sellingAllSharesClosesPositionAndUpdatesCash() {
        service.buy("GOOGL", 5, 200.0);
        int balanceBefore = account.getBalance();
        service.sell("GOOGL", 5, 210.0);

        assertNull(service.getPosition("GOOGL"));
        assertEquals(balanceBefore + (5 * 210), account.getBalance());
    }

    @Test
    public void sellWithInsufficientSharesLogsErrorButProcesses() {
        service.buy("AAPL", 10, 150.0);
        int balanceBefore = account.getBalance();

        SellConfirmation conf = service.sell("AAPL", 20, 155.0);

        assertNotNull(conf);
        assertEquals(20, conf.shares);
        assertEquals(balanceBefore + (int) Math.round(20 * 155.0), account.getBalance());
        assertNull(service.getPosition("AAPL"));
    }
}

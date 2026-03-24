package uk.co.asepstrath.bank.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentPriceServiceTest {

    @Test
    void testReturnsPriceWhenPresent() {
        CurrentPriceLoader loader = mock(CurrentPriceLoader.class);
        when(loader.getCurrentPrice("ABC")).thenReturn(1500);

        CurrentPriceService service = new CurrentPriceService(loader);

        assertEquals(1500, service.getPricePence("ABC"));
    }

    @Test
    void testReturnsZeroWhenMissing() {
        CurrentPriceLoader loader = mock(CurrentPriceLoader.class);
        when(loader.getCurrentPrice("ABC")).thenReturn(null);

        CurrentPriceService service = new CurrentPriceService(loader);

        assertEquals(0, service.getPricePence("ABC"));
    }
}

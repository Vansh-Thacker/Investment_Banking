package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioControllerTest {

    private PortfolioController controller;
    private Context ctx;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Mocking dependencies to isolate the controller logic
        ctx = mock(Context.class);
        logger = mock(Logger.class);
        controller = new PortfolioController(logger);
    }

    @Test
    @DisplayName("Dashboard returns the correct view name")
    void testDashboardViewName() {
        ModelAndView mav = controller.dashboard(ctx);
        assertNotNull(mav);
        // Matches the return new ModelAndView("dashboard", model) in your controller
        assertEquals("dashboard", mav.getView());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Dashboard model contains all required keys for the frontend")
    void testDashboardModelContents() {
        ModelAndView mav = controller.dashboard(ctx);

        // FIX: Cast Object to Map so we can use .containsKey()
        Map<String, Object> model = (Map<String, Object>) mav.getModel();

        assertNotNull(model, "Model should not be null");
        assertTrue(model.containsKey("user"), "Model missing 'user'");
        assertTrue(model.containsKey("equities"), "Model missing 'equities'");
        assertTrue(model.containsKey("totalValue"), "Model missing 'totalValue'");
        assertTrue(model.containsKey("countries"), "Model missing 'countries'");
        assertTrue(model.containsKey("regions"), "Model missing 'regions'");
        assertTrue(model.containsKey("industries"), "Model missing 'industries'");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Total Value is formatted as a String with decimal points")
    void testTotalValueFormatting() {
        ModelAndView mav = controller.dashboard(ctx);
        Map<String, Object> model = (Map<String, Object>) mav.getModel();

        Object totalObj = model.get("totalValue");
        assertNotNull(totalObj, "totalValue should exist in the model");

        String totalValue = totalObj.toString();
        // Check for decimal point as per String.format("%.2f", ...)
        assertTrue(totalValue.contains("."), "Total value should be a formatted decimal string");
    }

    @Test
    @DisplayName("Debug endpoint returns connectivity information")
    void testDebugEndpoint() {
        String debugResponse = controller.debug(ctx);
        assertNotNull(debugResponse);
        // Verify it returns either the success status or the error message from the catch block
        assertTrue(debugResponse.contains("STATUS") || debugResponse.contains("ERROR"));
    }

    @Test
    @DisplayName("Controller handles dashboard request without crashing")
    void testDashboardExecution() {
        // General smoke test to ensure no NullPointerExceptions occur during typical execution
        assertDoesNotThrow(() -> controller.dashboard(ctx));
    }
}
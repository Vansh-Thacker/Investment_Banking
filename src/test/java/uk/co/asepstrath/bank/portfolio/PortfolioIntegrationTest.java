package uk.co.asepstrath.bank.portfolio;

import io.jooby.test.MockRouter;
import io.jooby.StatusCode;
import io.jooby.ModelAndView;
import io.jooby.Session;
import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.App;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PortfolioIntegrationTest {

    // Initialize the real App
    private final MockRouter router = new MockRouter(new App());

    @Test

    public void testDashboardWithAuthenticatedSession() {
        // 1. Create a MockContext for the specific route
        io.jooby.test.MockContext ctx = new io.jooby.test.MockContext();
        ctx.setMethod("GET");
        ctx.setRequestPath("/portfolio");

        // 2. "Seed" the session to bypass the 302 Redirect
        ctx.setSession(new io.jooby.test.MockSession());
        ctx.session().put("user", "TestUser");

        // 3. Pass the context to the router
        router.get("/portfolio", ctx, rsp -> {
            // If the 302 was caused by a missing session, it should now be 200
            assertEquals(StatusCode.OK, rsp.getStatusCode(),
                    "Expected 200 but got " + rsp.getStatusCode());

            // 4. Verify the View
            Object result = rsp.value();
            assertTrue(result instanceof ModelAndView, "Result should be a template (ModelAndView)");

            ModelAndView mav = (ModelAndView) result;
            assertEquals("dashboard.hbs", mav.getView());

            // 5. Verify the Data Model
            Map<String, Object> model = (Map<String, Object>) mav.getModel();
            assertTrue(model.containsKey("totalValue"), "Model should contain totalValue");
        });
    }

    @Test
    public void testDebugEndpointDoesNotRedirect() {
        router.get("/portfolio/debug", rsp -> {
            // If debug doesn't require a login, this should be 200 automatically
            assertEquals(StatusCode.OK, rsp.getStatusCode());
            String body = rsp.value().toString();
            assertTrue(body.contains("STATUS") || body.contains("ERROR"));
        });
    }
}
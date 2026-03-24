package uk.co.asepstrath.bank.portfolio;

import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import org.slf4j.Logger;
import io.jooby.Context;

import java.util.*;

@Path("/portfolio")
public class PortfolioController extends Jooby {

    private final Logger logger;
    private final PortfolioService service;

    public PortfolioController(Logger logger) {
        this.logger = logger;
        this.service = new PortfolioService("uk", "Europe", "Finance");
    }

    @GET
    public ModelAndView dashboard(Context ctx) {
        List<String> tickers = List.of("hsba", "bp", "ulvr", "tsco", "nvda");
        List<Map<String, Object>> equities = service.getEquityData(tickers);

        // Compute total value
        double total = equities.stream()
                .mapToDouble(e -> ((Number) e.getOrDefault("marketCap", 0)).doubleValue())
                .sum();

        Map<String, Object> model = new HashMap<>();
        model.put("equities", equities);
        model.put("totalValue", String.format("%.0f", total));
        return new ModelAndView("portfolio/dashboard", model);
    }
}
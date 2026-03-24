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
    public ModelAndView index(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        // Add data to model as needed
        return new ModelAndView("portfolio/index", model);
    }
}
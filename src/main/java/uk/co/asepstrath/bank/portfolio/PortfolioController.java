package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import org.slf4j.Logger;
import java.util.*;

@Path("/portfolio")
public class PortfolioController extends Jooby {
    private final PortfolioService service;

    public PortfolioController(Logger log) {
        this.service = new PortfolioService("uk", "Europe", "Finance");    }

    @GET
    public ModelAndView dashboard(Context ctx) {
        String user = ctx.session().get("user").value((String) null);
        if (user == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        List<String> tickers = List.of("hsba", "bp", "ulvr", "tsco", "nvda");
        List<Map<String, Object>> equities = service.getEquityData(tickers);

        long total = 0;
        for (Map<String, Object> eq : equities) {
            Object cap = eq.get("marketCap");
            if (cap instanceof Number) {
                total += ((Number) cap).longValue();
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("equities", equities);
        model.put("totalValue", total);
        model.put("user", user);

        return new ModelAndView("portfolio/dashboard.hbs", model);
    }

}
package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import org.slf4j.Logger;
import java.util.*;

@Path("/portfolio")
public class PortfolioController {
    private final PortfolioService service;
    private final Logger logger;

    public PortfolioController(Logger log) {
        this.logger = log;
        this.service = new PortfolioService("uk", "Europe", "Finance");
    }

    @GET
    public ModelAndView dashboard(Context ctx) {
        ///String user = "Test user";
        
        String user = ctx.session().get("user").value((String) null);
        if (user == null) {
            ctx.sendRedirect("/login");
            return null;
        }

         




        // Fetch all equities (includes country/region/sector from metadata API)
        List<Map<String, Object>> equities = service.getAllEquities();

        // Log to verify country/region are populated
        for (Map<String, Object> eq : equities) {
            logger.info("Equity: {} country={} region={} sector={}",
                    eq.get("ticker"), eq.get("country"), eq.get("region"), eq.get("sector"));
        }

        // Calculate grand total from marketCap
        double grandTotal = 0;
        for (Map<String, Object> eq : equities) {
            try {
                grandTotal += Double.parseDouble(eq.getOrDefault("marketCap", 0).toString());
            } catch (NumberFormatException ignored) {}
        }

        // Build breakdowns for all three tabs
        List<PortfolioBreakdown> countryData  = service.getBreakdown(equities, "country");
        List<PortfolioBreakdown> regionData   = service.getBreakdown(equities, "region");
        List<PortfolioBreakdown> industryData = service.getBreakdown(equities, "sector");

        Map<String, Object> model = new HashMap<>();
        model.put("user",       user);
        model.put("equities",   equities);
        model.put("totalValue", String.format("%.2f", grandTotal));
        model.put("countries",  countryData);
        model.put("regions",    regionData);
        model.put("industries", industryData);

        // FIX 1: correct template path, no .hbs extension
        return new ModelAndView("dashboard.hbs", model);
    }

    @GET
    @Path("/debug")
    public String debug(Context ctx) {
        try {
            var r1 = kong.unirest.core.Unirest.get("https://api.asep-strath.co.uk/api/equities").asString();
            var r2 = kong.unirest.core.Unirest.get("https://api.asep-strath.co.uk/api/equities/metadata")
                    .queryString("tickers", "CTT,STD,CLP,OLC,APX").asString();
            return "STATUS1: " + r1.getStatus() + "\nBODY1: " + r1.getBody()
                    + "\n\nSTATUS2: " + r2.getStatus() + "\nBODY2: " + r2.getBody();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.Jooby;
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
        // Maintaining your existing constructor style
        this.service = new PortfolioService("uk", "Europe", "Finance");
    }

    @GET
    public ModelAndView dashboard(Context ctx) {
        String user = "Test user";

        // Fetch all equities with prices
        List<Map<String, Object>> equities = service.getAllEquities();

        // Calculate grand total
        double grandTotal = 0;
        for (Map<String, Object> eq : equities) {
            try {
                grandTotal += Double.parseDouble(eq.getOrDefault("marketCap", 0).toString());
            } catch (NumberFormatException ignored) {}
        }

        // Breakdowns — sector works, country/region will show "Unknown"
        // since the API doesn't provide that data
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

        return new ModelAndView("dashboard.hbs", model);
    }
    /**
     * Logic to group equities and apply Emma's "Under 5% = Other" rule
     */
    private List<PortfolioBreakdown> getGroupedBreakdown(List<Map<String, Object>> equities, String categoryType, double grandTotal) {
        Map<String, Double> totals = new HashMap<>();

        for (Map<String, Object> eq : equities) {
            String key = (String) eq.getOrDefault(categoryType, "Unknown");
            double value = Double.parseDouble(eq.getOrDefault("marketCap", 0).toString());
            totals.put(key, totals.getOrDefault(key, 0.0) + value);
        }

        List<PortfolioBreakdown> results = new ArrayList<>();
        double otherValue = 0;

        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            double percent = (entry.getValue() / grandTotal) * 100;

            if (percent < 5.0) {
                otherValue += entry.getValue();
            } else {
                // Assuming PortfolioBreakdown has a constructor (label, value, percent)
                results.add(new PortfolioBreakdown(entry.getKey(), entry.getValue(), percent));
            }
        }

        if (otherValue > 0) {
            results.add(new PortfolioBreakdown("Other", otherValue, (otherValue / grandTotal) * 100));
        }

        return results;
    }
}
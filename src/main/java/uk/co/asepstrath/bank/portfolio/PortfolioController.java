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
        // 1. Session Check
        //String user = ctx.session().get("user").value((String) null);
        String user = "Test user";
        /*if (user == null) {
            ctx.sendRedirect("/login");
            return null;
        }

         */

        // 2. Fetch Data (Sarah's Requirement: Current Market Value)
        List<String> tickers = List.of("hsba", "bp", "ulvr", "tsco", "nvda");
        List<Map<String, Object>> equities = service.getEquityData(tickers);

        // 3. Calculate Grand Total for the Summary
        double grandTotal = 0;

        for (Map<String, Object> eq : equities) {
            Object cap = eq.get("marketCap");
            if (cap instanceof Number) {
                grandTotal += ((Number) cap).doubleValue();
            }
        }

        // 4. Generate Groupings (Country, Region, Industry) with Emma's 5% Rule
        List<PortfolioBreakdown> countryData = getGroupedBreakdown(equities, "country", grandTotal);
        List<PortfolioBreakdown> regionData = getGroupedBreakdown(equities, "region", grandTotal);
        List<PortfolioBreakdown> industryData = getGroupedBreakdown(equities, "sector", grandTotal);

        // 5. Build the Model for Handlebars
        Map<String, Object> model = new HashMap<>();
        model.put("user", user); // Using 'user' as requested
        model.put("equities", equities);
        model.put("totalValue", grandTotal);

        // These keys allow the .hbs to render 3 separate charts
        model.put("countries", countryData);
        model.put("regions", regionData);
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
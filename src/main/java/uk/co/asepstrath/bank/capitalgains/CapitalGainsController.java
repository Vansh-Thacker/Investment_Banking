package uk.co.asepstrath.bank.capitalgains;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/capitalgains")
public class CapitalGainsController {

    private final CapitalGainsService service;
    private final Logger log;

    public CapitalGainsController(Logger log, InvestmentService investmentService) {
        this.log = log;
        this.service = new CapitalGainsService(investmentService);
    }

    /** For tests: inject custom CapitalGainsService. */
    CapitalGainsController(Logger log, CapitalGainsService service) {
        this.log = log;
        this.service = service;
    }

    @GET
    public void index(Context ctx) {
        ctx.sendRedirect("/capitalgains/summary");
    }

    /** HTML summary view: current or selected tax year. */
    @GET("/summary")
    public ModelAndView summary(@QueryParam Integer year, Context ctx) {
        // Determine tax year start
        int taxYearStart;
        if (year != null) {
            taxYearStart = year;
        } else {
            taxYearStart = service.getCurrentTaxYear().startYear;
        }

        CapitalGainsSummary summary = service.getSummaryForYear(taxYearStart);
        List<TaxYearInfo> years = service.getAvailableTaxYears();
        for (TaxYearInfo y : years) {
            y.selected = (y.startYear == taxYearStart);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("summary", summary);
        model.put("years", years);
        model.put("selectedYear", taxYearStart);

        // Optional: message if no SELLs
        if (summary.transactions == null || summary.transactions.isEmpty()) {
            model.put("noSalesMessage", "No realised gains or losses for this tax year.");
        }

        return new ModelAndView("capitalgains.hbs", model);
    }

    /** JSON: SELL transactions for a given tax year. */
    @GET("/transactions")
    public Object transactions(@QueryParam Integer year, Context ctx) {
        int taxYearStart;
        if (year != null) {
            taxYearStart = year;
        } else {
            taxYearStart = service.getCurrentTaxYear().startYear;
        }
        return service.getTransactionsForYear(taxYearStart);
    }

    /** JSON: summary for a given tax year. */
    @GET("/summary/json")
    public Object summaryJson(@QueryParam Integer year, Context ctx) {
        int taxYearStart;
        if (year != null) {
            taxYearStart = year;
        } else {
            taxYearStart = service.getCurrentTaxYear().startYear;
        }
        return service.getSummaryForYear(taxYearStart);
    }
}


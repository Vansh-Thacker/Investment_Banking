package uk.co.asepstrath.bank.capitalgains;

import io.jooby.Context;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import io.jooby.TemplateEngine;

import uk.co.asepstrath.bank.investments.InvestmentTransaction;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;
import uk.co.asepstrath.bank.investments.InvestmentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;


@Path("/capitalgains")
public class CapitalGainsController {

    private final Logger log;
    private final UserService userService;

    public CapitalGainsController(Logger log, UserService userService) {
        this.log = log;
        this.userService = userService;
    }

    /* ============================================================
       SUMMARY PAGE
       ============================================================ */
    @GET
    @Path("/summary")
    public Object summary(Context ctx) {

        // --- FIXED SESSION HANDLING ---
        var userIdValue = ctx.session().get("userId");
        if (userIdValue == null || userIdValue.value() == null) {
            return ctx.sendRedirect("/login");
        }
        String userId = userIdValue.value();

        User user = userService.findById(userId);
        InvestmentService invService = user.getInvestmentService();

        CapitalGainsService cgService = new CapitalGainsService(invService);

        cgService.loadPricesCsv("prices.csv");
        cgService.loadHistoryCsv("UserStats/" + userId + "/history.csv");

        // Load available years
        List<TaxYearInfo> years = cgService.getAvailableTaxYears();

        // Determine selected year
        String yearParam = ctx.query("year").valueOrNull();
        TaxYearInfo selectedYear;

        if (yearParam != null && !yearParam.isBlank()) {
            try {
                int y = Integer.parseInt(yearParam);
                selectedYear = new TaxYearInfo(y);
            } catch (NumberFormatException e) {
                selectedYear = years.get(0);
            }
        } else {
            selectedYear = years.get(0);
        }

        // Mark selected year
        for (TaxYearInfo y : years) {
            y.selected = (y.startYear == selectedYear.startYear);
        }

        // Compute summary
        CapitalGainsSummary summary = cgService.getSummaryForYear(selectedYear.startYear);

        // Build model
        Map<String, Object> model = new HashMap<>();
        model.put("years", years);
        model.put("selectedYear", selectedYear);
        model.put("summary", summary);

        return new ModelAndView("capitalgains.hbs", model);
    }

    /* ============================================================
       TRANSACTIONS PAGE
       ============================================================ */
    @GET
    @Path("/transactions")
    public Object transactions(Context ctx) {

        // --- FIXED SESSION HANDLING ---
        var userIdValue = ctx.session().get("userId");
        if (userIdValue == null || userIdValue.value() == null) {
            return ctx.sendRedirect("/login");
        }
        String userId = userIdValue.value();

        User user = userService.findById(userId);
        InvestmentService invService = user.getInvestmentService();

        CapitalGainsService cgService = new CapitalGainsService(invService);

        cgService.loadPricesCsv("prices.csv");
        cgService.loadHistoryCsv("UserStats/" + userId + "/history.csv");

        // Load available years
        List<TaxYearInfo> years = cgService.getAvailableTaxYears();

        // Determine selected year
        String yearParam = ctx.query("year").valueOrNull();
        TaxYearInfo selectedYear;

        if (yearParam != null && !yearParam.isBlank()) {
            try {
                int y = Integer.parseInt(yearParam);
                selectedYear = new TaxYearInfo(y);
            } catch (NumberFormatException e) {
                selectedYear = years.get(0);
            }
        } else {
            selectedYear = years.get(0);
        }

        // Mark selected year
        for (TaxYearInfo y : years) {
            y.selected = (y.startYear == selectedYear.startYear);
        }

        // Load realised transactions
        List<InvestmentTransaction> realised =
                cgService.getRealisedTransactionsForYear(selectedYear.startYear);

        // Build model
        Map<String, Object> model = new HashMap<>();
        model.put("years", years);
        model.put("selectedYear", selectedYear);
        model.put("transactions", realised);

        return new ModelAndView("capitalgains-transactions.hbs", model);
    }
}


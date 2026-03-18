package uk.co.asepstrath.bank.cash;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.annotation.*;
import io.jooby.ModelAndView;
import org.slf4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.user.UserService;


@Path("/cash")
public class CashController extends Jooby {

    private UserService users;
    private final CashService service;
    private final Logger log;

    public CashController(Logger log, Account account) {
        this(log, account, new CashService(account));
    }

    // Test constructor (inject custom CashService)
    public CashController(Logger log, Account account, CashService service) {
        this.log = log;
        this.service = service;
    }

    public CashService getService() {
        return service;
    }

    public void setUserService(UserService users) {
        this.users = users;
    }

    // Helper: per-user CashService using UserStats/<username>/cashTransactionHistory.csv
    private CashService getUserService(Context ctx) {

        // TEST MODE: if no UserService was injected, use the injected CashService
        if (this.users == null) {
            return this.service;
        }

        // REAL APP MODE: per-user CSV
        String username = ctx.session().get("user").value();
        String path = "UserStats/" + username + "/cashTransactionHistory.csv";
        return new CashService(new Account(), path);
    }

    
    @GET
    public void index(Context ctx) {
        ctx.sendRedirect("/cash/cashsidefrontend");
    }

    @POST("/deposit")
    public void deposit(CashRequest req, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.deposit(req.amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            // still update saved balance (even though CSV is now source of truth)
            if (users != null) {
                String username = ctx.session().get("user").value();
                users.updateBalance(username, userService.getBalance());
            }

        } catch (IllegalArgumentException e){
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    // Simple GET endpoint to allow HTML forms / links to deposit
    @GET("/deposit")
    public void depositGet(@QueryParam("amount") int amount, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.deposit(amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            if (users != null) {
                String username = ctx.session().get("user").value();
                users.updateBalance(username, userService.getBalance());
            }

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }

    }

    @POST("/withdraw")
    public void withdraw(CashRequest req, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.withdraw(req.amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            if (users != null) {
                String username = ctx.session().get("user").value();
                users.updateBalance(username, userService.getBalance());
            }

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }

    }

    // Simple GET endpoint to allow HTML forms / links to withdraw
    @GET("/withdraw")
    public void withdrawGet(@QueryParam("amount") int amount, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.withdraw(amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            if (users != null) {
                String username = ctx.session().get("user").value();
                users.updateBalance(username, userService.getBalance());
            }

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    @GET("/balance")
    public Object balance(Context ctx) {
        CashService userService = getUserService(ctx);
        return userService.getBalance();
    }

    @GET("/transactions")
    public Object transactions(Context ctx) {
        CashService userService = getUserService(ctx);
        return userService.getHistory();
    }

    // New: dedicated pages
    @GET("/deposit-page")
    public ModelAndView depositPage(Context ctx) {
        return new ModelAndView("deposit.hbs", new HashMap<>());
    }

    @GET("/withdraw-page")
    public ModelAndView withdrawPage(Context ctx) {
        return new ModelAndView("withdraw.hbs", new HashMap<>());
    }

    @GET("/history")
    public ModelAndView historyPage(Context ctx) {
        CashService userService = getUserService(ctx);
        Map<String, Object> model = new HashMap<>();
        model.put("history", userService.getHistory());
        return new ModelAndView("history.hbs", model);
    }

    @GET("/cashsidefrontend")
    public ModelAndView showDashboard(Context ctx) {

        // Get logged-in username from session
        String username = ctx.session().get("user").value((String) null);
        if (username == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        CashService userService = getUserService(ctx);

        Map<String, Object> model = new HashMap<>();
        model.put("balance", userService.getBalance());
        model.put("history", userService.getHistory());
        model.put("name", username);

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("cash.hbs", model);
    }
}
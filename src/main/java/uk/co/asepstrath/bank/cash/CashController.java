package uk.co.asepstrath.bank.cash;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.annotation.*;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
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

    public CashController(Logger log, Account account) {
        this(log, account, new CashService(account));
    }

    // Test constructor (inject custom CashService)
    public CashController(Logger log, Account account, CashService service) {
        this.service = service;
    }

    public CashService getService() {
        return service;
    }

    public void setUserService(UserService users) {
        this.users = users;
    }

    @GET
    public void index(Context ctx) {
        ctx.sendRedirect("/cash/cashsidefrontend");
    }



    @POST("/deposit")
    public void deposit(CashRequest req, Context ctx) {
        try {
            service.deposit(req.amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            // saved balance updated after every transaction
            String username = ctx.session().get("user").value();
            users.updateBalance(username, service.getBalance());

        } catch (IllegalArgumentException e){
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    // Simple GET endpoint to allow HTML forms / links to deposit
    @GET("/deposit")
    public void depositGet(@QueryParam("amount") int amount, Context ctx) {
        // try, catch to stop page from crashing due to errors
        try {
            service.deposit(amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            // saved balance updated after every transaction
            String username = ctx.session().get("user").value();
            users.updateBalance(username, service.getBalance());
            
        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }

    }

    @POST("/withdraw")
    public void withdraw(CashRequest req, Context ctx) {
        try {
            service.withdraw(req.amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            // saved balance updated after every transaction
            String username = ctx.session().get("user").value();
            users.updateBalance(username, service.getBalance());

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }

    }

    // Simple GET endpoint to allow HTML forms / links to withdraw
    @GET("/withdraw")
    public void withdrawGet(@QueryParam("amount") int amount, Context ctx) {
        try {
            service.withdraw(amount);
            ctx.sendRedirect("/cash/cashsidefrontend");

            // saved balance updated after every transaction
            String username = ctx.session().get("user").value();
            users.updateBalance(username, service.getBalance());

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }


    @GET("/balance")
    public Object balance() {
        return service.getBalance();
    }

    @GET("/transactions")
    public Object transactions() {
        return service.getHistory();
    }

    @GET("/cashsidefrontend")
    public ModelAndView showDashboard(Context ctx) {

        // Get logged-in username from session
        String username = ctx.session().get("user").value((String) null);
        if (username == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        // Load saved balance from CSV
        int savedBalance = users.getBalance(username);

        // Only reset if the current balance is NOT already the saved balance
        if (service.getBalance() != savedBalance) {
            // Reset to 0 only if balance is > 0 (avoid withdraw(0) error)
            if (service.getBalance() > 0) {
                service.withdraw(service.getBalance());
            }

            // Load saved balance (only if > 0 to avoid deposit(0) errors)
            if (savedBalance > 0) {
                service.deposit(savedBalance);
            }
        }


        Map<String, Object> model = new HashMap<>();
        model.put("balance", service.getBalance());
        model.put("history", service.getHistory());
        model.put("name", "Customer");

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("cash.hbs", model);
    }
}


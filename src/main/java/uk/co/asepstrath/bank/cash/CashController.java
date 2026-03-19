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
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

@Path("/cash")
public class CashController extends Jooby {

    private UserService users;
    private final CashService service;
    private final Logger log;

    public CashController(Logger log, Account account) {
        this(log, account, new CashService(account));
    }

    public CashController(Logger log, Account account, CashService service) {
        this.log = log;
        this.service = service;
    }

    public void setUserService(UserService users) {
        this.users = users;
    }

    // Build per-user CashService using UserStats/<id>/history.csv
    private CashService getUserService(Context ctx) {

        if (this.users == null) {
            return this.service;
        }

        String userId = ctx.session().get("userId").value();
        String path = "UserStats/" + userId + "/history.csv";

        // Load user balance from users.csv
        User user = users.findById(userId);

        // Create account with correct starting balance
        Account account = new Account();
        account.forceSetBalance(user.getBalancePence());

        return new CashService(account, path);
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

            // Update balance in users.csv
            if (users != null) {
                String userId = ctx.session().get("userId").value();
                users.updateBalance(userId, userService.getBalance());
            }

            ctx.sendRedirect("/cash/cashsidefrontend");

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    @GET("/deposit")
    public void depositGet(@QueryParam("amount") int amount, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.deposit(amount);

            if (users != null) {
                String userId = ctx.session().get("userId").value();
                users.updateBalance(userId, userService.getBalance());
            }

            ctx.sendRedirect("/cash/cashsidefrontend");

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

            if (users != null) {
                String userId = ctx.session().get("userId").value();
                users.updateBalance(userId, userService.getBalance());
            }

            ctx.sendRedirect("/cash/cashsidefrontend");

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    @GET("/withdraw")
    public void withdrawGet(@QueryParam("amount") int amount, Context ctx) {
        try {
            CashService userService = getUserService(ctx);
            userService.withdraw(amount);

            if (users != null) {
                String userId = ctx.session().get("userId").value();
                users.updateBalance(userId, userService.getBalance());
            }

            ctx.sendRedirect("/cash/cashsidefrontend");

        } catch (IllegalArgumentException e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.sendRedirect("/cash/cashsidefrontend?error=" + msg);
        }
    }

    @GET("/balance")
    public Object balance(Context ctx) {
        return getUserService(ctx).getBalance();
    }

    @GET("/transactions")
    public Object transactions(Context ctx) {
        return getUserService(ctx).getHistory();
    }

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

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        User user = users.findById(userId);
        CashService userService = getUserService(ctx);

        Map<String, Object> model = new HashMap<>();
        model.put("balance", userService.getBalance());
        model.put("history", userService.getHistory());
        model.put("name", user.getName());

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("cash.hbs", model);
    }
}
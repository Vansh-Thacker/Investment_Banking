package uk.co.asepstrath.bank.cash;
import io.jooby.Context;
import io.jooby.annotation.*;
import io.jooby.ModelAndView;
import io.jooby.StatusCode;
import io.jooby.annotation.*;
import io.jooby.exception.StatusCodeException;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import uk.co.asepstrath.bank.Account;


@Path("/cash")
public class CashController{

    private final CashService service;
    private final Logger log;

    public CashController(DataSource ds, Logger log, Account account) {
        this.service = new CashService(account);
        this.log = log;
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
        Map<String, Object> model = new HashMap<>();
        model.put("balance", service.getBalance());
        model.put("history", service.getHistory());
        model.put("name", "Customer");        // or pull from session
        // try, catch to stop page from crashing due to errors
        try {
            String err = ctx.query("error").value((String) null);
            model.put("error", err);
        } catch (Exception ignore) {
            model.put("error", null);
        }
        return new ModelAndView("cash.hbs", model);
    }


}

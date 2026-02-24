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
    public void deposit(CashRequest amount, Context ctx) {
        service.deposit(amount.amount);
        ctx.sendRedirect("/cash/cashsidefrontend");
    }

    @POST("/withdraw")
    public void withdraw(CashRequest amount, Context ctx) {
        service.withdraw(amount.amount);
        ctx.sendRedirect("/cash/cashsidefrontend");
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
        return new ModelAndView("cash.hbs", model);
    }


}

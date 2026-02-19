package uk.co.asepstrath.bank.cash;
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
    @POST("/deposit")
    public Object deposit(CashRequest amount) {
        service.deposit(amount.amount);
        return service.getBalance();
    }

    @POST("/withdraw")
    public Object withdraw(CashRequest amount) {
        service.withdraw(amount.amount);
        return service.getBalance();
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
    public ModelAndView showDashboard() {
        Map<String, Object> model = new HashMap<>();
        // Use the service to get data for the frontend
        model.put("balance", service.getBalance());
        model.put("history", service.getHistory());

        // This looks for src/main/resources/views/cash.hbs
        return new ModelAndView("cash.hbs", model);
    }

}

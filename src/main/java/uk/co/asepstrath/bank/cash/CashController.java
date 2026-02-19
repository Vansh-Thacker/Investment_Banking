package uk.co.asepstrath.bank.cash;
import io.jooby.ModelAndView;
import io.jooby.StatusCode;
import io.jooby.annotation.*;
import io.jooby.exception.StatusCodeException;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import org.jetbrains.annotations.NotNull;
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
public class CashController {

    private final CashService service;
    private final Logger log;

    public CashController(DataSource ds, Logger log, Account account) {
        this.service = new CashService(account);
        this.log = log;
    }

    @POST("/deposit")
    public Object deposit(@NotNull CashRequest amount) {
        service.deposit(amount.amount);
        return service.getBalance();
    }

    @POST("/withdraw")
    public Object withdraw(@NotNull CashRequest amount) {
        service.withdraw(amount.amount);
        return service.getBalance();
    }

    @GET("/balance")
    public Object balance() {
        return service.getBalance();
    }

    @GET("/transactions")
    public Object transactions() {
        return service.getHistory(); // Is this getting the transcation type?
    }
    @GET("/transcations_type")
    public Object transcationsType() {
        return service.getTranscationtype();
    }
}


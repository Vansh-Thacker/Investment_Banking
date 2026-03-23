package uk.co.asepstrath.bank.portfolio;

import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.StatusCode;
import io.jooby.annotation.*;
import io.jooby.exception.StatusCodeException;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import io.jooby.Context;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


@Path("/portfolio")
public class PortfolioController extends Jooby{

    private final DataSource dataSource;
    private final Logger logger;
    private final PortfolioService service;
    public PortfolioController(DataSource dataSource, Logger logger) {
        this.dataSource = dataSource;
        this.logger = logger;
        this.service = new PortfolioService("uk", "Europe", "Finance");
    }
    }


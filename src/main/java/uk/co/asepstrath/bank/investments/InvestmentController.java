package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.annotation.*;
import io.jooby.StatusCode;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Path("/investments")
public class InvestmentController {

    private static final String FILTER_PURCHASES = "purchases";

    private final InvestmentService service;
    private final Logger log;
    private final PriceService priceService;

    public InvestmentController(DataSource ds, Logger log, Account account) {
        this(ds, log, account, null);
    }

    public InvestmentController(DataSource ds, Logger log, Account account, PriceService priceService) {
        this.service = new InvestmentService(account);
        this.log = log;
        this.priceService = priceService;
    }

    @GET("/positions")
    public Object positions() {
        return service.getPositions();
    }

    // current market price for a symbol – for showing price before confirming sell
    @GET("/prices/{symbol}")
    public Object currentPrice(@PathParam String symbol, Context ctx) {
        if (symbol == null || symbol.isBlank()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Symbol is required");
        }
        String normalized = symbol.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{1,5}")) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Ticker must be 1–5 uppercase letters");
        }
        if (priceService == null) {
            ctx.setResponseCode(StatusCode.NOT_FOUND);
            return Collections.singletonMap("error", "Price not available");
        }
        PriceRecord record = priceService.getPrice(normalized);
        if (record == null) {
            ctx.setResponseCode(StatusCode.NOT_FOUND);
            return Collections.singletonMap("error", "Price not found for symbol: " + normalized);
        }
        CurrentPriceResponse resp = new CurrentPriceResponse();
        resp.symbol = normalized;
        resp.price = record.price;
        resp.date = record.date;
        return resp;
    }

    // stock history. filter=purchases see only buy transactions sorted by timestamp
    @GET("/transactions")
    public Object transactions(@QueryParam String filter, @QueryParam String sort) {
        if (FILTER_PURCHASES.equalsIgnoreCase(filter)) {
            return service.getPurchaseHistory(sort);
        }
        return service.getHistory();
    }

    // preview sale details for the confirmation screen (symbol, name, shares, est price and proceeds)
    // no sale executed, user must call post /sell to confirm and execute
    @POST("/sell/preview")
    public Object sellPreview(TradeRequest request, Context ctx) {
        String ticker = normalizeTicker(request.ticker, ctx);
        if (ticker == null) return errorResponse(ctx);
        if (request.shares <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Number of shares must be positive");
        }
        if (request.price <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Price must be positive");
        }
        try {
            return service.getSalePreview(ticker, request.shares, request.price);
        } catch (IllegalArgumentException e) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    // execute sale. User should have reviewed details via post /sell/preview first. Returns confirmation
    @POST("/sell")
    public Object sell(TradeRequest request, Context ctx) {
        String ticker = normalizeTicker(request.ticker, ctx);
        if (ticker == null) return errorResponse(ctx);
        if (request.shares <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Number of shares must be positive");
        }
        if (request.price <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Price must be positive");
        }
        try {
            return service.sell(ticker, request.shares, request.price);
        } catch (IllegalArgumentException e) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    private String normalizeTicker(String ticker, Context ctx) {
        if (ticker == null || ticker.isBlank()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return null;
        }
        String normalized = ticker.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{1,5}")) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return null;
        }
        return normalized;
    }

    private Object errorResponse(Context ctx) {
        return Collections.singletonMap("error", "Ticker must be 1–5 uppercase letters");
    }
}

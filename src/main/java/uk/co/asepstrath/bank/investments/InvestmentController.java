package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.annotation.*;
import io.jooby.StatusCode;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Path("/investments")
public class InvestmentController {

    private static final String FILTER_PURCHASES = "purchases";

    private final InvestmentService service;
    private final Logger log;
    private final PriceService priceService;
    private final EquitiesService equitiesService;

    public InvestmentController(DataSource ds, Logger log, Account account) {
        this(ds, log, account, null, null);
    }

    public InvestmentController(DataSource ds, Logger log, Account account, PriceService priceService) {
        this(ds, log, account, priceService, null);
    }

    public InvestmentController(DataSource ds, Logger log, Account account, PriceService priceService, EquitiesService equitiesService) {
        this.service = new InvestmentService(account);
        this.log = log;
        this.priceService = priceService;
        this.equitiesService = equitiesService;
    }

    public InvestmentController(DataSource ds, Logger log, Account account, PriceService priceService, EquitiesService equitiesService, InvestmentService service) {
        this.service = service;
        this.log = log;
        this.priceService = priceService;
        this.equitiesService = equitiesService;
    }

    @GET("/positions")
    public Object positions() {
        Collection<Position> result = service.getPositions();
        if (equitiesService != null) {
            for (Position p : result) {
                p.stockName = equitiesService.getName(p.ticker);
            }
        }
        enrichPositionsWithPrices(result);
        return result;
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

    // stock history. filter=purchases = buy-only. sort=asc oldest first, desc/null = newest first. limit/offset for pagination.
    @GET("/transactions")
    public Object transactions(@QueryParam String filter, @QueryParam String sort,
                               @QueryParam Integer limit, @QueryParam Integer offset) {
        List<InvestmentTransaction> result = FILTER_PURCHASES.equalsIgnoreCase(filter)
                ? service.getPurchaseHistory(sort)
                : service.getHistorySorted(sort);
        if (equitiesService != null) {
            for (InvestmentTransaction tx : result) {
                tx.stockName = equitiesService.getName(tx.ticker);
            }
        }
        if (limit != null) {
            int off = offset != null ? Math.max(0, offset) : 0;
            int total = result.size();
            int from = Math.min(off, total);
            int to = Math.min(from + Math.max(0, limit), total);
            TransactionListResponse resp = new TransactionListResponse();
            resp.transactions = result.subList(from, to);
            resp.total = total;
            return resp;
        }
        return result;
    }

    // buy stocks checks funds and takes money, updates shares, returns result
    @POST("/buy")
    public Object buy(TradeRequest request, Context ctx) {
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
        return service.newTransaction(ticker, request.shares, request.price);
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

    // currentPrice and currentValue on each pos from PriceService
    private void enrichPositionsWithPrices(Collection<Position> positions) {
        if (priceService == null) {
            return;
        }
        for (Position p : positions) {
            PriceRecord pr = priceService.getPrice(p.ticker);
            if (pr != null) {
                p.currentPrice = (double) pr.price;
                p.currentValue = p.shares * p.currentPrice;
            }
        }
    }

    // portfolio summary: t_value = sum of pos current values + cash
    @GET("/portfolio/summary")
    public Object portfolioSummary() {
        Collection<Position> positions = service.getPositions();
        enrichPositionsWithPrices(positions);
        double positionsValue = 0.0;
        for (Position p : positions) {
            if (p.currentValue != null) {
                positionsValue += p.currentValue;
            }
        }
        int cashBalance = service.getCashBalance();
        double totalPortfolioValue = positionsValue + cashBalance;

        PortfolioSummaryResponse resp = new PortfolioSummaryResponse();
        resp.totalPortfolioValue = totalPortfolioValue;
        resp.positionsValue = positionsValue;
        resp.cashBalance = cashBalance;
        return resp;
    }
}

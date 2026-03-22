package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import io.jooby.StatusCode;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.PriceLoader;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

import javax.sql.DataSource;
import java.util.*;

@Path("/investments")
public class InvestmentController {

    private static final String FILTER_PURCHASES = "purchases";

    private UserService users;
    private final InvestmentService service;
    private final Logger log;
    private final PriceService priceService;
    private final EquitiesService equitiesService;

    private InvestmentService getUserInvestmentService(Context ctx) {

        if (this.users == null) {
            return this.service; // fallback for tests
        }

        String userId = ctx.session().get("userId").value();
        String path = "UserStats/" + userId + "/history.csv";

        User user = users.findById(userId);

        Account account = new Account();
        account.forceSetBalance(user.getBalancePence());

        InvestmentService loaded = new InvestmentService(account);

        PriceLoader loader = new PriceLoader("prices.csv");

        try {
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            System.out.println("DEBUG: Looking for history at: " + p.toAbsolutePath());
            System.out.println("DEBUG: File exists? " + java.nio.file.Files.exists(p));

            if (java.nio.file.Files.exists(p)) {
                java.io.InputStream in = java.nio.file.Files.newInputStream(p);
                loaded.importCsv(in, loader);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load investment CSV for user " + userId, e);
        }

        // make the global service reflect the imported data
        this.service.copyFrom(loaded);

        return this.service;
    }

    public void setUserService(UserService users) {
        this.users = users;
    }

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

    public InvestmentController(UserService users, Logger log, PriceService priceService, EquitiesService equitiesService) {
        this.users = users;
        this.log = log;
        this.priceService = priceService;
        this.equitiesService = equitiesService;
        this.service = new InvestmentService(new Account());
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

    @GET("/investmentsfrontend")
    public ModelAndView showInvestments(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        User user = users.findById(userId);

        InvestmentService invService = getUserInvestmentService(ctx);

        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("cashBalance", invService.getCashBalance());
        model.put("positions", invService.getPositions());

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("investments.hbs", model);
    }

    @GET("/historyfrontend")
    public ModelAndView showInvestmentHistory(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        User user = users.findById(userId);
        InvestmentService invService = getUserInvestmentService(ctx);

        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("transactions", invService.getHistorySorted("desc"));

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("investment-history.hbs", model);
    }
}

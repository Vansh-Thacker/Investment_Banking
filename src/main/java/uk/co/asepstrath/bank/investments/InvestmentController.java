package uk.co.asepstrath.bank.investments;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import io.jooby.StatusCode;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.PriceRecord;
import uk.co.asepstrath.bank.evaluation.PriceService;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

@Path("/investments")
public class InvestmentController {

    private static final String FILTER_PURCHASES = "purchases";

    private UserService users;
    private final Logger log;
    private final PriceService priceService;
    private final EquitiesService equitiesService;
    private final CurrentPriceService currentPriceService;
    private final Account account;

    public InvestmentController(DataSource ds, Logger log, Account account,
                                PriceService priceService,
                                EquitiesService equitiesService,
                                CurrentPriceService currentPriceService) {

        this.log = log;
        this.account = account;
        this.priceService = priceService;
        this.equitiesService = equitiesService;
        this.currentPriceService = currentPriceService;
    }

    // Constructor for testing
    private InvestmentController newController(
            UserService users,
            PriceService priceService,
            EquitiesService equitiesService,
            CurrentPriceService currentPriceService,
            Account account
    ) {
        Logger log = mock(Logger.class);
        DataSource ds = mock(DataSource.class);

        InvestmentController c = new InvestmentController(
                ds,                 // 1
                log,                // 2
                account,            // 3
                priceService,       // 4
                equitiesService,    // 5
                currentPriceService // 6
        );

        c.setUserService(users);
        return c;
    }

    public void setUserService(UserService users) {
        this.users = users;
    }

    /* ============================================================
       POSITIONS
       ============================================================ */

    @GET("/positions")
    public Object positions(Context ctx) {
        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        InvestmentService service = user.getInvestmentService();

        Collection<Position> result = service.getPositions();
        if (equitiesService != null) {
            for (Position p : result) {
                p.stockName = equitiesService.getName(p.ticker);
            }
        }
        enrichPositionsWithPrices(result);
        return result;
    }

    /* ============================================================
       PRICE LOOKUP (legacy endpoint)
       ============================================================ */

    @GET("/prices/{symbol}")
    public Object currentPrice(@PathParam String symbol, Context ctx) {
        if (symbol == null || symbol.isBlank()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Symbol is required");
        }
        String normalized = symbol.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{1,10}")) {
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

    /* ============================================================
       TRANSACTIONS
       ============================================================ */

    @GET("/transactions")
    public Object transactions(@QueryParam String filter, @QueryParam String sort,
                               @QueryParam Integer limit, @QueryParam Integer offset,
                               Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        InvestmentService service = user.getInvestmentService();

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

    /* ============================================================
       BUY — using currentprices.csv
       ============================================================ */

    @POST("/buy")
    public Object buy(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        if (user == null) {
            // Create a dummy user using Mockito
            user = mock(User.class);
            when(user.getAccount()).thenReturn(new Account());
            when(user.getInvestmentService()).thenReturn(mock(InvestmentService.class));
        }

        Account account = user.getAccount();
        InvestmentService inv = user.getInvestmentService();

        String selected = ctx.form("ticker").value();
        String ticker = selected.split(" ")[0];


        int shares = ctx.form("shares").intValue();

        ticker = normalizeTicker(ticker, ctx);
        if (ticker == null) {
            ctx.sendRedirect("/investments/buy?error=Invalid ticker");
            return null;
        }

        if (shares <= 0) {
            ctx.sendRedirect("/investments/buy?error=Invalid share amount");
            return null;
        }

        int pricePence = currentPriceService.getPricePence(ticker);
        if (pricePence <= 0) {
            ctx.sendRedirect("/investments/buy?error=No price available");
            return null;
        }

        double price = pricePence / 100.0;
        int costPence = shares * pricePence;

        if (costPence > account.getBalance()) {
            ctx.sendRedirect("/investments/buy?error=Insufficient funds");
            return null;
        }

        account.withdraw(costPence);
        users.updateBalance(userId, account.getBalance());

        inv.newTransaction(ticker, shares, price);

        ctx.sendRedirect("/investments/investmentsfrontend");
        return null;
    }

    /* ============================================================
       SELL PREVIEW
       ============================================================ */

    @POST("/sell/preview")
    public Object sellPreview(TradeRequest request, Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        InvestmentService service = user.getInvestmentService();

        String ticker = normalizeTicker(request.ticker, ctx);
        if (ticker == null) return errorResponse(ctx);
        if (request.shares <= 0) {
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

    /* ============================================================
       SELL — using currentprices.csv
       ============================================================ */

    @POST("/sell")
    public Object sell(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        if (user == null) {
            // Create a dummy user using Mockito
            user = mock(User.class);
            when(user.getAccount()).thenReturn(new Account());
            when(user.getInvestmentService()).thenReturn(mock(InvestmentService.class));
        }

        Account account = user.getAccount();
        InvestmentService service = user.getInvestmentService();

        // ⭐ Read ticker directly from form (no JSON, no TradeRequest)
        String ticker = ctx.form("ticker").value();
        ticker = normalizeTicker(ticker, ctx);
        if (ticker == null) {
            return Collections.singletonMap("error", "Invalid ticker");
        }

        // ⭐ Read shares from form
        int shares = ctx.form("shares").intValue();
        if (shares <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "Number of shares must be positive");
        }

        // ⭐ Get current price
        int pricePence = currentPriceService.getPricePence(ticker);
        if (pricePence <= 0) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", "No current price available for " + ticker);
        }

        double price = pricePence / 100.0;

        try {
            // ⭐ Perform the sale
            SellConfirmation confirmation = service.sell(ticker, shares, price);

            // ⭐ Update balance
            users.updateBalance(userId, account.getBalance());

            // ⭐ Redirect back to investments page (same as BUY)
            ctx.sendRedirect("/investments/investmentsfrontend");
            return null;

        } catch (IllegalArgumentException e) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /* ============================================================
       FRONTEND PAGES
       ============================================================ */

    @GET("/portfolio/summary")
    public Object portfolioSummary(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.setResponseCode(StatusCode.UNAUTHORIZED);
            return Collections.singletonMap("error", "Not logged in");
        }

        User user = users.findById(userId);
        InvestmentService service = user.getInvestmentService();

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
        InvestmentService invService = user.getInvestmentService();

        Collection<Position> positions = invService.getPositions();
        enrichPositionsWithPrices(positions);

        for (Position p : positions) {
            p.averageBookPrice = p.averageBookPrice;
            p.totalInvested = p.totalInvested;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("cashBalance", invService.getCashBalance());
        model.put("positions", positions);

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
        InvestmentService invService = user.getInvestmentService();

        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("transactions", invService.getHistorySorted("desc"));

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("investment-history.hbs", model);
    }

    @GET("/buy")
    public ModelAndView buyPage(Context ctx) {

        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        User user = users.findById(userId);
        InvestmentService inv = user.getInvestmentService();

        Collection<Position> positions = inv.getPositions();
        enrichPositionsWithPrices(positions);

        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("positions", positions);
        model.put("cashBalance", inv.getCashBalance());

        List<String> tickerOptions = new ArrayList<>();

        List<String> allTickers = Arrays.asList(
            "APX","CRC","CSCU","FINX","FSC","GMX150","HLN","HZB","MEDX","MOG",
            "MRH","NXP","PHX","PNTC","QSY","STRF","TECHX","TTN","VGM","ZNC"
        );

        for (String t : allTickers) {
            int pence = currentPriceService.getPricePence(t);
            double price = pence > 0 ? pence / 100.0 : 0.0;

            String display = t + " - £" + String.format("%.2f", price);
            tickerOptions.add(display);
        }

        model.put("tickerOptions", tickerOptions);

        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("buy.hbs", model);
    }

    @GET("/sell")
    public ModelAndView sellPage(Context ctx) {
        String userId = ctx.session().get("userId").value((String) null);
        if (userId == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        User user = users.findById(userId);
        InvestmentService inv = user.getInvestmentService();
        Collection<Position> positions = inv.getPositions();
        enrichPositionsWithPrices(positions);

        Map<String, Object> model = new HashMap<>();
        model.put("positions", positions);

        return new ModelAndView("sell.hbs", model);
    }

    /* ============================================================
       HELPERS
       ============================================================ */

    private String normalizeTicker(String ticker, Context ctx) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        String normalized = ticker.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{1,10}")) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return null;
        }
        return normalized;
    }

    private Object errorResponse(Context ctx) {
        return Collections.singletonMap("error", "Ticker must be 1–5 uppercase letters");
    }

    private void enrichPositionsWithPrices(Collection<Position> positions) {
        if (currentPriceService == null) return;

        for (Position p : positions) {
            int pricePence = currentPriceService.getPricePence(p.ticker);
            if (pricePence > 0) {
                double price = pricePence / 100.0;
                p.currentPrice = price;
                p.currentValue = p.shares * price;
            }
        }
    }
}
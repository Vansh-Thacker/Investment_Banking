package uk.co.asepstrath.bank;

import io.jooby.Cookie;
import io.jooby.ServerOptions;
import io.jooby.SessionStore;
import io.jooby.netty.NettyServer;
import uk.co.asepstrath.bank.cash.CashController;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.cash.CashController_;
import uk.co.asepstrath.bank.evaluation.AsepApiClient;
import uk.co.asepstrath.bank.evaluation.EvaluationController;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
import uk.co.asepstrath.bank.evaluation.PortfolioEvaluationService;
import uk.co.asepstrath.bank.evaluation.PriceService;
import uk.co.asepstrath.bank.investments.InvestmentController;
import uk.co.asepstrath.bank.investments.InvestmentController_;
import uk.co.asepstrath.bank.investments.InvestmentService;
import io.jooby.Jooby;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.helper.UniRestExtension;
import io.jooby.hikari.HikariModule;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.user.LoginController;
import uk.co.asepstrath.bank.user.LoginController_;
import uk.co.asepstrath.bank.user.UserService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class App extends Jooby {

    {
        /*
        This section is used for setting up the Jooby Framework modules
         */
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));

        setSessionStore(SessionStore.memory(Cookie.session("scotbank")));

        /*
        This will host any files in src/main/resources/assets on <host>/assets
        For example in the dice template (dice.hbs) it references "assets/dice.png" which is in resources/assets folder
         */
        assets("/assets/*", "/assets");
        assets("/service_worker.js","/service_worker.js");



        Logger log = getLog();

        UserService users = new UserService();
        Account account = new Account();

        // Create controller manually
        CashController cash = new CashController(log, account);

        // Inject UserService
        cash.setUserService(users);

        // Register it with Jooby
        mvc(new CashController_(cash));
        mvc(new LoginController_(log, users));

        DataSource ds = require(DataSource.class);
        PriceService priceService = new PriceService();
        EquitiesService equitiesService = new EquitiesService();
        InvestmentService investmentService = new InvestmentService(account);
        InvestmentController investments =
                new InvestmentController(ds, log, account, priceService, equitiesService, investmentService);
        investments.setUserService(users);
        mvc(new InvestmentController_(investments));

        String asepBaseUrl = System.getenv("ASEP_BASE_URL");
        if (asepBaseUrl == null || asepBaseUrl.isBlank()) {
            asepBaseUrl = getConfig().getString("asep.baseUrl");
        }
        AsepApiClient asepClient = new AsepApiClient(asepBaseUrl.trim());
        PortfolioEvaluationService portfolioEvaluationService =
                new PortfolioEvaluationService(investmentService, equitiesService, asepClient);
        EvaluationController evaluationController = new EvaluationController(portfolioEvaluationService);
        get("/evaluation/portfolio/snapshot", ctx -> evaluationController.portfolioSnapshot());
        get("/evaluation/portfolio/performance", ctx -> evaluationController.portfolioPerformanceOverview());
        get("/evaluation/positions/average-book-price", ctx -> evaluationController.averageBookPriceOverview());
        get("/evaluation/positions/cost-basis", ctx -> evaluationController.costBasisOverview());
        get("/evaluation/positions/prior-day-eod", ctx -> evaluationController.priorDayEodOverview());
        get("/evaluation/positions/market-value", ctx -> evaluationController.marketValueOverview());
        get("/evaluation/positions/value-at-prior-close", ctx -> evaluationController.valueAtPriorCloseOverview());
        get("/evaluation/positions/unrealized-pnl", ctx -> evaluationController.unrealizedPnLOverview());
        get("/evaluation/positions/unrealized-gains-losses", ctx -> evaluationController.unrealizedGainsLossesOverview());
        get("/evaluation/positions/gains-losses", ctx -> evaluationController.absoluteAndPercentageGainsLossesOverview());


        /*
        Finally we register our application lifecycle methods
         */
        onStarted(() -> onStart());
        onStop(() -> onStop());
    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }

    /*
    This function will be called when the application starts up,
    it should be used to ensure that the DB is properly setup
     */
    public void onStart() {
        Logger log = getLog();
        log.info("Starting Up...");

        // Fetch DB Source
        DataSource ds = require(DataSource.class);
        // Open Connection to DB

        try (Connection connection = ds.getConnection()) {
            //
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE `Example` (`Key` varchar(255),`Value` varchar(255))");
            stmt.executeUpdate("INSERT INTO Example " + "VALUES ('WelcomeMessage', 'Welcome to A Bank')");
        } catch (SQLException e) {
            log.error("Database Creation Error",e);
        }


    }

    /*
    This function will be called when the application shuts down
     */
    public void onStop() {
        System.out.println("Shutting Down...");
    }

}

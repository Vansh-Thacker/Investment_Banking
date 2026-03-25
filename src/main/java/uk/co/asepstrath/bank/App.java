package uk.co.asepstrath.bank;

import io.jooby.Cookie;
import io.jooby.ServerOptions;
import io.jooby.SessionStore;
import io.jooby.netty.NettyServer;
import uk.co.asepstrath.bank.cash.CashController;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.cash.CashController_;
import uk.co.asepstrath.bank.evaluation.CurrentPriceLoader;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;
import uk.co.asepstrath.bank.evaluation.EquitiesService;
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
import uk.co.asepstrath.bank.capitalgains.CapitalGainsController_;
import uk.co.asepstrath.bank.investments.InvestmentService;

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

        Account account = new Account();
        InvestmentService investmentService = new InvestmentService(account);


        // --- Current prices (single source of truth) ---
        CurrentPriceLoader currentPriceLoader = new CurrentPriceLoader("currentprices.csv");
        CurrentPriceService currentPriceService = new CurrentPriceService(currentPriceLoader);

        // --- UserService now needs CurrentPriceService ---
        UserService users = new UserService(currentPriceService);

        // --- Cash controller ---
        CashController cash = new CashController(log, account);
        cash.setUserService(users);
        mvc(new CashController_(cash));
        mvc(new LoginController_(log, users));

        // --- Price + equities services ---
        DataSource ds = require(DataSource.class);
        PriceService priceService = new PriceService();
        EquitiesService equitiesService = new EquitiesService();

        // --- Investment controller uses SAME CurrentPriceService ---
        InvestmentController investments =
                new InvestmentController(ds, log, account, priceService, equitiesService, currentPriceService);
        investments.setUserService(users);
        mvc(new InvestmentController_(investments));


        get("/import-api", ctx -> {
            uk.co.asepstrath.bank.apiimport.PricesImporter.importPrices();
            return "Import complete";
        });



        String asepBaseUrl = System.getenv("ASEP_BASE_URL");
        if (asepBaseUrl == null || asepBaseUrl.isBlank()) {
            asepBaseUrl = getConfig().getString("asep.baseUrl");
        }
        //mvc(new InvestmentController_(ds, log, account, priceService, equitiesService));
        mvc(new InvestmentController_(ds, log, account, priceService, equitiesService, investmentService));
        mvc(new CapitalGainsController_(log, investmentService));


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

        // 1. Run the API Importers
        try {
            log.info("Importing data from API...");
            uk.co.asepstrath.bank.apiimport.PricesImporter.importPrices();
            log.info("API Import successful.");
        } catch (Exception e) {
            log.error("Failed to import data from API", e);
        }

        // 2. Database Setup (Existing code)
        DataSource ds = require(DataSource.class);
        try (Connection connection = ds.getConnection()) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE `Example` (`Key` varchar(255),`Value` varchar(255))");
            stmt.executeUpdate("INSERT INTO Example VALUES ('WelcomeMessage', 'Welcome to A Bank')");
        } catch (SQLException e) {
            log.error("Database Creation Error", e);
        }
    }

    /*
    This function will be called when the application shuts down
     */
    public void onStop() {
        System.out.println("Shutting Down...");
    }

}

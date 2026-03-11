package uk.co.asepstrath.bank;

import io.jooby.Cookie;
import io.jooby.ServerOptions;
import io.jooby.SessionStore;
import io.jooby.netty.NettyServer;
import uk.co.asepstrath.bank.cash.CashController;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.cash.CashController_;
//import uk.co.asepstrath.bank.evaluation.EvaluationController;
//import uk.co.asepstrath.bank.example.ExampleController;
//import uk.co.asepstrath.bank.investments.InvestmentController;
import io.jooby.Jooby;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.helper.UniRestExtension;
import io.jooby.hikari.HikariModule;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.example.ExampleController_;
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

        /*
        Now we set up our controllers and their dependencies
         */
        //DataSource ds = require(DataSource.class);



        /*  // Commented out previous controller stuff
        Logger log = getLog();

        mvc(new CashController_(log, new Account())); // the new controllers
        mvc(new LoginController_(log, new UserService())); // the new controllers
        */


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



        //mvc(new InvestmentController(ds, log));
        //mvc(new EvaluationController(ds, log));


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

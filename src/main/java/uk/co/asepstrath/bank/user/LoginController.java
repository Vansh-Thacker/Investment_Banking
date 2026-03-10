package uk.co.asepstrath.bank.user;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

@Path("/")
public class LoginController {

    private final UserService users;
    private final Logger log;

    public LoginController(Logger log, UserService users) {
        this.log = log;
        this.users = users;
    }

    // --- GET pages moved from CashController ---

    @GET("/login")
    public ModelAndView showLogin(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Customer");

        // If already logged in → redirect to dashboard
        String user = ctx.session().get("user").value((String) null);
        if (user != null) {
            ctx.sendRedirect("/cash/cashsidefrontend");
            return null;
        }


        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("login.hbs", model);
    }


   @GET("/signup")
    public ModelAndView showSignup(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Customer");

        // If already logged in → redirect to dashboard
        String user = ctx.session().get("user").value((String) null);
        if (user != null) {
            ctx.sendRedirect("/cash/cashsidefrontend");
            return null;
        }


        String error = ctx.query("error").value((String) null);
        model.put("error", error);

        return new ModelAndView("signup.hbs", model);
    }


    // --- POST login ---

    @POST("/login")
    public void login(Context ctx) {
        String username = ctx.form("username").value();
        String password = ctx.form("password").value();

        User u = users.authenticate(username, password);
        if (u == null) {
            ctx.sendRedirect("/login?error=Invalid%20credentials");
            return;
        }

        ctx.session().put("user", u.getUsername());
        ctx.sendRedirect("/cash/cashsidefrontend");
    }

    // --- POST signup ---

    @POST("/signup")
    public void signup(Context ctx) {
        String username = ctx.form("username").value();
        String password = ctx.form("password").value();

        try {
            users.createUser(username, password);
            ctx.sendRedirect("/login?success=Account%20created");
        } catch (IllegalArgumentException e) {
            ctx.sendRedirect("/signup?error=" + e.getMessage());
        }
    }

    // --- logout ---

    @GET("/logout")
    public void logout(Context ctx) {
        ctx.session().destroy();
        ctx.sendRedirect("/login");
    }
}
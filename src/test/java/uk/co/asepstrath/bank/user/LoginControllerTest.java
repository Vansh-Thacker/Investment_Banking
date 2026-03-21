package uk.co.asepstrath.bank.user;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.Session;
import io.jooby.value.Value;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoginControllerTest {

    private LoginController newController(UserService users) {
        Logger log = mock(Logger.class);
        return new LoginController(log, users);
    }

    @Test
    void testShowLoginWhenNotLoggedIn() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);

        // session.get("user") returns a Value
        Value sessionValue = mock(Value.class);
        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(sessionValue);
        when(sessionValue.value((String) null)).thenReturn(null);

        // error query param
        Value errorValue = mock(Value.class);
        when(ctx.query("error")).thenReturn(errorValue);
        when(errorValue.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.showLogin(ctx);

        assertEquals("login.hbs", mv.getView());
    }

    @Test
    void testShowLoginRedirectsWhenLoggedIn() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);

        Value sessionValue = mock(Value.class);
        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(sessionValue);
        when(sessionValue.value((String) null)).thenReturn("alice");

        controller.showLogin(ctx);

        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testLoginSuccess() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);

        when(ctx.session()).thenReturn(session);

        // form fields
        Value userField = mock(Value.class);
        Value passField = mock(Value.class);

        when(ctx.form("username")).thenReturn(userField);
        when(ctx.form("password")).thenReturn(passField);

        when(userField.value()).thenReturn("alice");
        when(passField.value()).thenReturn("pw");

        User mockUser = new User("50", "alice", "pw", 0);
        when(users.findByName("alice")).thenReturn(mockUser);

        controller.login(ctx);

        verify(session).put("userId", "50");
        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

    @Test
    void testLoginFailure() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);

        Value userField = mock(Value.class);
        Value passField = mock(Value.class);

        when(ctx.form("username")).thenReturn(userField);
        when(ctx.form("password")).thenReturn(passField);

        when(userField.value()).thenReturn("alice");
        when(passField.value()).thenReturn("wrong");

        User mockUser = new User("50", "alice", "pw", 0);
        when(users.findByName("alice")).thenReturn(mockUser);

        controller.login(ctx);

        verify(ctx).sendRedirect("/login?error=Invalid%20credentials");
    }

    @Test
    void testLogout() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);

        when(ctx.session()).thenReturn(session);

        controller.logout(ctx);

        verify(session).destroy();
        verify(ctx).sendRedirect("/login");
    }

    @Test
    void testSignupSuccess() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Value userField = mock(Value.class);
        Value passField = mock(Value.class);

        when(ctx.form("username")).thenReturn(userField);
        when(ctx.form("password")).thenReturn(passField);

        when(userField.value()).thenReturn("newuser");
        when(passField.value()).thenReturn("pw");

        controller.signup(ctx);

        //verify(users).createUser("newuser", "pw");
        verify(ctx).sendRedirect("/login?success=Account%20created");
    }

    @Test
    void testSignupDuplicate() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Value userField = mock(Value.class);
        Value passField = mock(Value.class);

        when(ctx.form("username")).thenReturn(userField);
        when(ctx.form("password")).thenReturn(passField);

        when(userField.value()).thenReturn("alice");
        when(passField.value()).thenReturn("pw");

        doThrow(new IllegalArgumentException("Username already exists")).when(users).createUser("alice", "pw");

        controller.signup(ctx);

        verify(ctx).sendRedirect("/signup?error=Username already exists");
    }

    @Test
    void testShowSignupWhenNotLoggedIn() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);
        Value sessionValue = mock(Value.class);
        Value errorValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(sessionValue);
        when(sessionValue.value((String) null)).thenReturn(null);

        when(ctx.query("error")).thenReturn(errorValue);
        when(errorValue.value((String) null)).thenReturn(null);

        ModelAndView mv = controller.showSignup(ctx);

        assertEquals("signup.hbs", mv.getView());
    }

    @Test
    void testShowSignupRedirectsWhenLoggedIn() {
        UserService users = mock(UserService.class);
        LoginController controller = newController(users);

        Context ctx = mock(Context.class);
        Session session = mock(Session.class);
        Value sessionValue = mock(Value.class);

        when(ctx.session()).thenReturn(session);
        when(session.get("userId")).thenReturn(sessionValue);
        when(sessionValue.value((String) null)).thenReturn("alice");

        controller.showSignup(ctx);

        verify(ctx).sendRedirect("/cash/cashsidefrontend");
    }

}


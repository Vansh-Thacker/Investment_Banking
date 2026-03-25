package uk.co.asepstrath.bank.cash;

import io.jooby.Context;
import io.jooby.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashControllerIntegrationTest {

    private CashController controller;
    private UserService mockUserService;
    private Context mockCtx;

    @BeforeEach
    void setUp() throws IOException {
        mockCtx = mock(Context.class);
        mockUserService = mock(UserService.class);

        // 1. MOCK THE SESSION CHAIN (No Value Class mentioned)
        Session mockSession = mock(Session.class, RETURNS_DEEP_STUBS);
        when(mockCtx.session()).thenReturn(mockSession);

        // FIX FOR AMBIGUITY:
        // We use (String) null to force the compiler to pick the String version of value()
        String nullStr = null;

        // Mock the version with no arguments
        doReturn("user123").when(mockSession.get(anyString())).value();

        // Mock the version with a String argument (this solves the Ambiguity)
        doReturn("user123").when(mockSession.get(anyString())).value(nullStr);
        doReturn("user123").when(mockSession.get(anyString())).value(any(String.class));

        // 2. CREATE THE USER (Using the 4-argument constructor)
        // User(String id, String name, String email, int balance)
        User testUser = new User("user123", "Test User", "test@example.com", 5000);
        when(mockUserService.findById("user123")).thenReturn(testUser);

        // 3. INITIALIZE CONTROLLER
        controller = new CashController(mock(Logger.class), new Account());
        controller.setUserService(mockUserService);

        // Clean up test file
        if (Files.exists(Paths.get("UserStats/user123/history.csv"))) {
            Files.deleteIfExists(Paths.get("UserStats/user123/history.csv"));
        }
    }

    @Test
    void testDepositIntegration() {
        // Act: £10.00 deposit
        controller.depositGet(1000, mockCtx);

        // Assert: Verify real file creation on disk
        assertTrue(Files.exists(Paths.get("UserStats/user123/history.csv")));

        // Assert: Verify Service Update (5000 initial + 1000 deposit)
        verify(mockUserService).updateBalance("user123", 6000);

        // Assert: Verify Redirect
        verify(mockCtx).sendRedirect(contains("cashsidefrontend"));
    }

    @Test
    void testWithdrawIntegration() {
        // Act: 5000 initial - 2000 withdraw = 3000
        controller.withdrawGet(2000, mockCtx);

        // Assert
        verify(mockUserService).updateBalance("user123", 3000);
    }
}
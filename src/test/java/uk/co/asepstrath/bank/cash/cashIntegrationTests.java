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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class cashIntegrationTests {
    private CashController controller;
    private UserService mockUserService;
    private Context mockCtx;
    private final String ID = "user123";

    @BeforeEach
    void setUp() throws IOException {
        mockCtx = mock(Context.class);
        mockUserService = mock(UserService.class);

        // Use Deep Stubs to avoid 'Value' class imports
        Session mockSession = mock(Session.class, RETURNS_DEEP_STUBS);
        when(mockCtx.session()).thenReturn(mockSession);

        // Resolve Ambiguity by casting null to String
        String s = null;
        doReturn(ID).when(mockSession).get(anyString()).value();
        doReturn(ID).when(mockSession).get(anyString()).value(s);

        // User(id, name, email, balance)
        User testUser = new User(ID, "Test", "t@t.com", 5000);
        when(mockUserService.findById(ID)).thenReturn(testUser);

        controller = new CashController(mock(Logger.class), new Account());
        controller.setUserService(mockUserService);

        Path p = Paths.get("UserStats", ID);
        if (Files.exists(p)) {
            Files.walk(p).sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    @Test
    void testDeposit() {
        controller.depositGet(1000, mockCtx);
        assertTrue(Files.exists(Paths.get("UserStats", ID, "history.csv")));
        verify(mockUserService).updateBalance(ID, 6000);
    }
}
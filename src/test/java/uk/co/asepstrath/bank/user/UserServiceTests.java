package uk.co.asepstrath.bank.user;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import uk.co.asepstrath.bank.evaluation.CurrentPriceLoader;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;

public class UserServiceTests {

    @TempDir
    Path tempDir;

    private Path csvFile;
    private UserService service;

    @BeforeEach
    void setup() throws Exception {
        csvFile = tempDir.resolve("users.csv");

        Files.writeString(csvFile, """
                id,name,passwordHash,balancePence
                00000000-0000-0001-aaaa-bbbb,alice,pass123,100
                00000000-0000-0002-cccc-dddd,bob,secret,50
                """);

        Path workingCsv = Path.of("users.csv").toAbsolutePath();
        Files.copy(csvFile, workingCsv, StandardCopyOption.REPLACE_EXISTING);

        CurrentPriceService cps = mock(CurrentPriceService.class);
        service = new UserService(cps);
    }

    @Test
    void testUsernameExists() {
        assertTrue(service.nameExists("alice"));
        assertFalse(service.nameExists("charlie"));
    }

    @Test
    void testFindByNameLoadsPasswordCorrectly() {
        User u = service.findByName("alice");
        assertNotNull(u);
        assertEquals("pass123", u.getPassword());
    }

    @Test
    void testPasswordDoesNotMatchWrongValue() {
        User u = service.findByName("alice");
        assertNotNull(u);
        assertNotEquals("wrong", u.getPassword());
    }

    @Test
    void testFindByNameReturnsNullForMissingUser() {
        assertNull(service.findByName("nobody"));
    }

    @Test
    void testCreateUser() throws Exception {
        Files.createDirectories(Path.of("UserStats"));

        service.createUser("charlie", "mypw");

        String csv = Files.readString(Path.of("users.csv"));
        assertTrue(csv.contains("charlie"));
        assertTrue(csv.contains("mypw"));
        assertTrue(csv.contains("0"));
    }

    @Test
    void testCreateUserDuplicateThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createUser("alice", "whatever"));
    }

    @Test
    void testGetBalance() {
        User alice = service.findByName("alice");
        assertNotNull(alice);
        assertEquals(100, alice.getBalancePence());

        assertNull(service.findByName("ghost"));
    }

    @Test
    void testUpdateBalance() throws Exception {
        User bob = service.findByName("bob");
        assertNotNull(bob);

        service.updateBalance(bob.getId(), 999);

        User updatedBob = service.findById(bob.getId());
        assertEquals(999, updatedBob.getBalancePence());

        String csv = Files.readString(Path.of("users.csv"));
        assertTrue(csv.contains("bob"));
        assertTrue(csv.contains("999"));
    }
}
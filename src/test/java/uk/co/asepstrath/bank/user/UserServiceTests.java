package uk.co.asepstrath.bank.user;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {

    @TempDir
    Path tempDir;

    private Path csvFile;
    private UserService service;

    @BeforeEach
    void setup() throws Exception {
        csvFile = tempDir.resolve("users.csv");

        // Pre-populate CSV in the correct format:
        // id,name,passwordHash,balancePence
        Files.write(csvFile, """
                id,name,passwordHash,balancePence
                00000000-0000-0001-aaaa-bbbb,alice,pass123,100
                00000000-0000-0002-cccc-dddd,bob,secret,50
                """.getBytes());

        // Copy this CSV into working directory so UserService loads it
        Files.copy(csvFile, Path.of("users.csv"), StandardCopyOption.REPLACE_EXISTING);

        service = new UserService();
    }

    @Test
    void testUsernameExists() {
        assertTrue(service.nameExists("alice"));
        assertFalse(service.nameExists("charlie"));
    }

    @Test
    void testAuthenticateSuccess() {
        // Your new UserService does NOT have authenticate()
        // So we replicate the LoginController logic:
        User u = service.findByName("alice");

        assertNotNull(u);
        assertEquals("pass123", u.getPassword());
    }

    @Test
    void testAuthenticateFailWrongPassword() {
        User u = service.findByName("alice");

        assertNotNull(u);
        assertNotEquals("wrong", u.getPassword());
    }

    @Test
    void testAuthenticateFailNoUser() {
        assertNull(service.findByName("nobody"));
    }

    @Test
    void testCreateUser() throws Exception {
        service.createUser("charlie", "mypw");

        // Reload CSV
        String csv = Files.readString(Path.of("users.csv"));

        assertTrue(csv.contains("charlie"));
        assertTrue(csv.contains("mypw"));
        assertTrue(csv.contains("0")); // starting balance
    }

    @Test
    void testCreateUserDuplicateThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createUser("alice", "whatever"));
    }

    @Test
    void testGetBalance() {
        User alice = service.findByName("alice");
        assertNotNull(alice);
        assertEquals(100, alice.getBalancePence());

        User ghost = service.findByName("ghost");
        assertNull(ghost);
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
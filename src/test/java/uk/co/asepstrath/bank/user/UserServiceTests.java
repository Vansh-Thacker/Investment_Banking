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

        // Pre-populate CSV
        Files.write(csvFile, """
                alice,pass123,100
                bob,secret,50
                """.getBytes());

        service = new UserService(csvFile.toString());
    }

    @Test
    void testUsernameExists() {
        assertTrue(service.usernameExists("alice"));
        assertFalse(service.usernameExists("charlie"));
    }

    @Test
    void testAuthenticateSuccess() {
        User u = service.authenticate("alice", "pass123");
        assertNotNull(u);
        assertEquals("alice", u.getUsername());
    }

    @Test
    void testAuthenticateFailWrongPassword() {
        assertNull(service.authenticate("alice", "wrong"));
    }

    @Test
    void testAuthenticateFailNoUser() {
        assertNull(service.authenticate("nobody", "pass"));
    }

    @Test
    void testCreateUser() throws Exception {
        service.createUser("charlie", "mypw");

        assertTrue(service.usernameExists("charlie"));

        // Check file updated
        String csv = Files.readString(csvFile);
        assertTrue(csv.contains("charlie,mypw,0"));
    }

    @Test
    void testCreateUserDuplicateThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createUser("alice", "whatever"));
    }

    @Test
    void testGetBalance() {
        assertEquals(100, service.getBalance("alice"));
        assertEquals(0, service.getBalance("ghost")); // default behaviour
    }

    @Test
    void testUpdateBalance() throws Exception {
        service.updateBalance("bob", 999);

        assertEquals(999, service.getBalance("bob"));

        // Check file updated
        String csv = Files.readString(csvFile);
        assertTrue(csv.contains("bob,secret,999"));
    }
}


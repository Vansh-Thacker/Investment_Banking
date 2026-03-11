package uk.co.asepstrath.bank.user;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserService {
    private final String csvPath;
    private final Map<String, User> users = new HashMap<>();

    public UserService() {
        this("users.csv");
    }

    public UserService(String csvPath) {
        this.csvPath = csvPath;
        loadUsers();
    }

    private void loadUsers() {
        try {
            Path path = Paths.get(csvPath);
            if (!Files.exists(path)) return;

            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] p = line.split(",");
                if (p.length != 3) continue;

                String username = p[0];
                String password = p[1];
                int balance = Integer.parseInt(p[2]);

                users.put(username, new User(username, password, balance));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users.csv", e);
        }
    }

    private void saveUsers() {
        try (PrintWriter out = new PrintWriter(new FileWriter(csvPath))) {
            for (User u : users.values()) {
                out.println(u.getUsername() + "," + u.getPassword() + "," + u.getBalance());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write users.csv", e);
        }
    }

    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }

    public void createUser(String username, String password) {
        if (usernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User u = new User(username, password, 0);
        users.put(username, u);
        saveUsers();
    }

    public User authenticate(String username, String password) {
        User u = users.get(username);
        if (u == null) return null;

        if (u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }

    public int getBalance(String username) {
        User u = users.get(username);
        if (u == null) {
            return 0; // default if somehow missing
        }
        return u.getBalance();
    }

    public void updateBalance(String username, int newBalance) {
        User u = users.get(username);
        if (u != null) {
            u.setBalance(newBalance);
            saveUsers();
        }
    }
}
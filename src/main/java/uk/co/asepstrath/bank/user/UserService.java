package uk.co.asepstrath.bank.user;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserService {

    private final Path csvPath = Path.of("users.csv");
    private final Map<String, User> users = new HashMap<>();

    // File storing the last used hex ID
    private static final String LAST_ID_FILE = "UserStats/lastId.txt";

    public UserService() {
        loadUsers();
    }

    private void loadUsers() {
        try {
            if (!Files.exists(csvPath)) return;

            List<String> lines = Files.readAllLines(csvPath);

            // Skip header
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(",", -1);
                if (p.length != 4) continue;

                String id = p[0];
                String name = p[1];
                String password = p[2]; // plain text
                int balance = Integer.parseInt(p[3]);

                users.put(id, new User(id, name, password, balance));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load users.csv", e);
        }
    }

    private void saveUsers() {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csvPath))) {
            out.println("id,name,passwordHash,balancePence");

            for (User u : users.values()) {
                out.printf("%s,%s,%s,%d%n",
                        u.getId(),
                        u.getName(),
                        u.getPassword(),
                        u.getBalancePence()
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write users.csv", e);
        }
    }

    public User findByName(String name) {
        return users.values().stream()
                .filter(u -> u.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public User findById(String id) {
        return users.get(id);
    }

    public boolean nameExists(String name) {
        return findByName(name) != null;
    }

    // ⭐ NEW: createUser now uses the hex ID generator
    public User createUser(String name, String password) {
        if (nameExists(name)) {
            throw new IllegalArgumentException("Username already exists");
        }

        String id = generateNextId();   // ⭐ Use hex ID

        User u = new User(id, name, password, 0);

        users.put(id, u);
        saveUsers();

        return u;
    }

    public void updateBalance(String id, int newBalance) {
        User u = users.get(id);
        if (u != null) {
            u.setBalancePence(newBalance);
            saveUsers();
        }
    }

    // Hex ID generator stored in UserStats/lastId.txt
    private String generateNextId() {
        try {
            Path path = Paths.get(LAST_ID_FILE);

            // Ensure directory exists
            Files.createDirectories(path.getParent());

            int last = 0x4f; // default: next will be 50

            if (Files.exists(path)) {
                String hex = Files.readString(path).trim();
                if (!hex.isEmpty()) {
                    last = Integer.parseInt(hex, 16);
                }
            }

            int next = last + 1;

            // ⭐ lowercase hex, padded to 4 digits (e.g., 0050)
            String hexPart = String.format("%04x", next);

            // Save new ID
            Files.writeString(path, hexPart);

            // ⭐ Generate random tail
            String random4 = randomHex(4);
            String random12 = randomHex(12);

            // ⭐ Final ID format
            return "00000000-0000-" + hexPart + "-" + random4 + "-" + random12;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate user ID", e);
        }
    }


    // Helper function for generate next id
    private String randomHex(int length) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(r.nextInt(16)));
        }
        return sb.toString();
    }
}
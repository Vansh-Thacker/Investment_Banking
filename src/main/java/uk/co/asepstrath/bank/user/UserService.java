package uk.co.asepstrath.bank.user;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserService {
    private final String csvPath;
    private final Map<String, User> users = new HashMap<>();
    private boolean newSchema = false;
    private static final String DOWNLOAD_USERS_CSV = "C:\\Users\\jonas\\Downloads\\users.csv";
    private static final String ENV_USERS_CSV_PATH = "USERS_CSV_PATH";

    public UserService() {
        this(resolveCsvPath("users.csv"));
    }

    public UserService(String csvPath) {
        this.csvPath = csvPath;
        loadUsers();
    }

    private static String resolveCsvPath(String fallbackRelativePath) {
        String env = System.getenv(ENV_USERS_CSV_PATH);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        // If the "downloads" users file exists on the developer machine, prefer it.
        try {
            Path download = Paths.get(DOWNLOAD_USERS_CSV);
            if (Files.exists(download)) {
                return DOWNLOAD_USERS_CSV;
            }
        } catch (Exception ignored) {}

        return fallbackRelativePath;
    }

    private static List<String> parseCsvLine(String line) {
        // Minimal CSV parsing: splits on commas, respecting quoted fields.
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Toggle quote state, or handle escaped quotes ("")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString().trim());
        return fields;
    }

    private static String toCsvField(String raw) {
        if (raw == null) return "";
        boolean needsQuoting = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
        String escaped = raw.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private void loadUsers() {
        try {
            Path path = Paths.get(csvPath);
            if (!Files.exists(path)) return;

            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) return;

            String first = lines.get(0).trim();
            String firstLower = first.toLowerCase(Locale.ROOT);
            newSchema = firstLower.startsWith("id,") && firstLower.contains("passwordhash") && firstLower.contains("balancepence");

            int startIdx = newSchema ? 1 : 0;
            for (int idx = startIdx; idx < lines.size(); idx++) {
                String line = lines.get(idx);
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                List<String> fields = parseCsvLine(line);
                if (!newSchema) {
                    if (fields.size() != 3) continue;
                    String username = fields.get(0);
                    String password = fields.get(1);
                    int balance = Integer.parseInt(fields.get(2));
                    users.put(username, new User(username, password, balance));
                    continue;
                }

                // New schema: id,name,passwordHash,balancePence
                if (fields.size() < 4) continue;
                String id = fields.get(0);
                String username = fields.get(1);
                String passwordHash = fields.get(2);
                int balance = Integer.parseInt(fields.get(3));
                users.put(username, new User(id, username, passwordHash, balance));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users.csv", e);
        }
    }

    private void saveUsers() {
        try (PrintWriter out = new PrintWriter(new FileWriter(csvPath))) {
            if (newSchema) {
                out.println("id,name,passwordHash,balancePence");
                for (User u : users.values()) {
                    out.println(
                            toCsvField(u.getId()) + "," +
                            toCsvField(u.getUsername()) + "," +
                            toCsvField(u.getPassword()) + "," +
                            u.getBalance()
                    );
                }
            } else {
                for (User u : users.values()) {
                    out.println(u.getUsername() + "," + u.getPassword() + "," + u.getBalance());
                }
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

        User u = newSchema
                ? new User(UUID.randomUUID().toString(), username, password, 0)
                : new User(username, password, 0);
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
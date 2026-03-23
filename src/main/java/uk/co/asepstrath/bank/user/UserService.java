package uk.co.asepstrath.bank.user;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.evaluation.CurrentPriceService;

public class UserService {

    private final Path csvPath = Path.of("users.csv");
    private final Map<String, User> users = new HashMap<>();

    private static final String LAST_ID_FILE = "UserStats/lastId.txt";

    private final CurrentPriceService currentPriceService;

    public UserService(CurrentPriceService currentPriceService) {
        this.currentPriceService = currentPriceService;
        loadUsers();
    }

    private void loadUsers() {
        try {
            if (!Files.exists(csvPath)) return;

            List<String> lines = Files.readAllLines(csvPath);

            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(",", -1);
                if (p.length != 4) continue;

                String id = p[0];
                String name = p[1];
                String password = p[2];
                int balance = Integer.parseInt(p[3]);

                User u = new User(id, name, password, balance);

                Account acc = new Account();
                acc.forceSetBalance(balance);
                u.setAccount(acc);

                String invCsv = "UserStats/" + id + "/history.csv";
                InvestmentService inv = new InvestmentService(acc, currentPriceService, invCsv);
                u.setInvestmentService(inv);

                users.put(id, u);
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

    public User createUser(String name, String password) {
        String id = generateId();
        int balance = 0;

        User u = new User(id, name, password, balance);

        Account acc = new Account();
        acc.forceSetBalance(balance);
        u.setAccount(acc);

        String invCsv = "UserStats/" + id + "/history.csv";
        InvestmentService inv = new InvestmentService(acc, currentPriceService, invCsv);
        u.setInvestmentService(inv);

        users.put(id, u);
        saveUsers();
        return u;
    }

    public void updateBalance(String id, int newBalance) {
        User u = users.get(id);
        if (u != null) {
            u.setBalancePence(newBalance);
            u.getAccount().forceSetBalance(newBalance);
            saveUsers();
        }
    }

    private String generateId() {
        return generateNextId();
    }

    private String generateNextId() {
        try {
            Path path = Paths.get(LAST_ID_FILE);

            Files.createDirectories(path.getParent());

            int last = 0x4f;

            if (Files.exists(path)) {
                String hex = Files.readString(path).trim();
                if (!hex.isEmpty()) {
                    last = Integer.parseInt(hex, 16);
                }
            }

            int next = last + 1;

            String hexPart = String.format("%04x", next);

            Files.writeString(path, hexPart);

            String random4 = randomHex(4);
            String random12 = randomHex(12);

            return "00000000-0000-" + hexPart + "-" + random4 + "-" + random12;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate user ID", e);
        }
    }

    private static final java.security.SecureRandom RNG = new java.security.SecureRandom();

    private String randomHex(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(RNG.nextInt(16)));
        }
        return sb.toString();
    }
}
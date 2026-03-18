package uk.co.asepstrath.bank.cash;
import uk.co.asepstrath.bank.Account;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CashService {
    private final Account account;
    private final List<CashTransaction> history = new ArrayList<>();
    private final String csv_path;

    // constructor for tests (default global file)
    public CashService(Account account) {
        this(account, "cashTransactionHistory.csv");
    }

    // main constructor (can be used with per-user paths)
    public CashService(Account account, String csvPath) {
        this.account = account;
        this.csv_path = csvPath;

        // Ensure parent directory exists (for per-user paths like UserStats/<user>/...)
        try {
            Path p = Paths.get(csv_path).getParent();
            if (p != null) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories for " + csv_path, e);
        }

        loadHistoryFromCSV();
    }

    // load csv
    private void loadHistoryFromCSV() {
        try {
            Path path = Paths.get(csv_path);
            if (!Files.exists(path)) {
                return; // No CSV yet
            }

            List<String> lines = Files.readAllLines(path);

            // skip the first line
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");

                LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
                String type = parts[1];

                // pounds to pence
                double pounds = Double.parseDouble(parts[2]);
                int amountPence = (int) Math.round(pounds * 100);

                // Replay transaction into account
                if (type.equals("DEPOSIT")) {
                    account.deposit(amountPence);
                } else if (type.equals("WITHDRAWAL")) {
                    account.withdraw(amountPence);
                }

                CashTransaction tx = new CashTransaction(
                        timestamp,
                        type,
                        amountPence,
                        account.getBalance()
                );

                history.add(tx);
            }

            // reorder list from oldest to newest
            history.sort(Comparator.comparing(CashTransaction::getDateTime));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + csv_path, e);
        }
    }

    // writing to csv after changes
    private void writeHistoryToCsv() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csv_path))) {

            writer.println("timestamp,type,amount");

            for (CashTransaction tx : history) {
                double pounds = tx.getAmount() / 100.0;

                writer.println(
                        tx.getDateTime() + "," +
                        tx.getType() + "," +
                        String.format("%.2f", pounds)
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + csv_path, e);
        }
    }


    public void deposit(int amount) {
        if (amount <= 0) { throw new IllegalArgumentException("Deposit must be positive"); }
        CashTransaction transac = new CashTransaction(amount, "DEPOSIT", account.getBalance());
        history.add(transac);
        account.deposit(amount);
        writeHistoryToCsv();
    }

    public void withdraw(int amount) {
        if (amount <= 0 || amount > account.getBalance()) {
            throw new IllegalArgumentException("Invalid withdrawal amount");
        }

        CashTransaction transac = new CashTransaction(amount, "WITHDRAWAL", account.getBalance());
        history.add(transac);
        account.withdraw(amount);
        writeHistoryToCsv();
    }

    public int getBalance() { return account.getBalance(); }

    public List<CashTransaction> getHistory() {
        return history;
    }

}
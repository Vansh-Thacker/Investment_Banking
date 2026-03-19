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

        // Ensure parent directory exists (for per-user paths like UserStats/<id>/...)
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

    // load csv (unified 5-column format)
    private void loadHistoryFromCSV() {
        try {
            Path path = Paths.get(csv_path);
            if (!Files.exists(path)) {
                return;
            }

            List<String> lines = Files.readAllLines(path);

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",", -1);
                if (parts.length < 4) continue;

                LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
                String type = parts[1];
                String ticker = parts[2];
                int amountPence = Integer.parseInt(parts[3]);

                // Only load into history — DO NOT affect account balance
                CashTransaction tx = new CashTransaction(
                        timestamp,
                        type,
                        ticker,
                        amountPence
                );

                history.add(tx);
            }

            history.sort(Comparator.comparing(CashTransaction::getDateTime));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + csv_path, e);
        }
    }

    // writing to csv after changes (unified 5-column format)
    private void writeHistoryToCsv() {
    try (PrintWriter writer = new PrintWriter(new FileWriter(csv_path))) {

            writer.println("timestamp,type,ticker,amount");

            for (CashTransaction tx : history) {
                writer.println(
                        tx.getDateTime() + "," +
                        tx.getType() + "," +
                        tx.getTicker() + "," +
                        tx.getAmount()
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + csv_path, e);
        }
    }

    public void deposit(int amount) {
    if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");

    account.deposit(amount);

    CashTransaction tx = new CashTransaction(amount, "DEPOSIT");
    history.add(tx);
    writeHistoryToCsv();
}

public void withdraw(int amount) {
    if (amount <= 0 || amount > account.getBalance()) {
        throw new IllegalArgumentException("Invalid withdrawal amount");
    }

    account.withdraw(amount);

    CashTransaction tx = new CashTransaction(amount, "WITHDRAWAL");
    history.add(tx);
    writeHistoryToCsv();
}

    public int getBalance() {
        return account.getBalance();
    }

    public List<CashTransaction> getHistory() {
        return history;
    }
}
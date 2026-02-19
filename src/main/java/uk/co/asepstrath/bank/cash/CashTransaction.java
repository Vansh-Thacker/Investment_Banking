package uk.co.asepstrath.bank.cash;
import java.time.LocalDateTime;

public class CashTransaction {
    private LocalDateTime timestamp;
    private String type; // is it a deposit or a withdrawl
    private int amount;
    private int balanceAfter;
    public CashTransaction(int amount_, String transactionType, int currentBal) {
        this.amount = amount_;
        this.type = transactionType;
        this.timestamp = LocalDateTime.now();
        if (type.equals("WITHDRAWAL")) {
            balanceAfter = currentBal-amount;
        } else {
            balanceAfter = currentBal+amount;
        }
    }
    //csv constructor
    public CashTransaction(LocalDateTime timestamp, String type, int amount, int balanceAfter) {
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public int getAmount() {
        return amount;
    }
    public String getType() {
        return type;
    }
    public int getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getDateTime() {
        return timestamp;
    }

}

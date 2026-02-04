package uk.co.asepstrath.bank.cash;
import java.time.LocalDateTime;

public class CashTransaction {
    public LocalDateTime timestamp;
    public String type; // is it a deposit or a withdrawl
    public int amount;
    public int balanceAfter;
}

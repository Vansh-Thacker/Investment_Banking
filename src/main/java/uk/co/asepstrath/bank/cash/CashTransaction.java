package uk.co.asepstrath.bank.cash;

import java.time.LocalDateTime;

public class CashTransaction {

    private final LocalDateTime dateTime;
    private final String type;
    private final String ticker;
    private final int amount; // pence

    public CashTransaction(LocalDateTime dateTime, String type, String ticker, int amount) {
        this.dateTime = dateTime;
        this.type = type;
        this.ticker = ticker;
        this.amount = amount;
    }

    // For new DEPOSIT/WITHDRAWAL
    public CashTransaction(int amount, String type) {
        this.dateTime = LocalDateTime.now();
        this.type = type;
        this.ticker = "";
        this.amount = amount;
    }

    public LocalDateTime getDateTime() { return dateTime; }
    public String getType() { return type; }
    public String getTicker() { return ticker; }
    public int getAmount() { return amount; }
}
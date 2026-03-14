package uk.co.asepstrath.bank;

public class Account {
    private int balance = 0;

    public void deposit(int amount) {
        balance += amount;
    }
    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) {
        if (amount<=0) throw new IllegalArgumentException("must be positive");
        if (amount>balance) throw new IllegalStateException("not enough funds");
        balance -= amount;
    }

    /** Deduct amount even if balance would go negative (§1: log WARNING but still process). */
    public void withdrawAllowNegative(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("must be positive");
        balance -= amount;
    }
}

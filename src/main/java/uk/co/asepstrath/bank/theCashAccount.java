package uk.co.asepstrath.bank;

import uk.co.scotbank.CashAccount;

public class theCashAccount implements CashAccount {

    private double balance = 0.0;

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit must be positive");
        }

        double applied = Math.floor(amount * 100.0) / 100.0;
        balance += applied;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal must be positive");
        }

        double applied = Math.floor(amount * 100.0) / 100.0;

        if (applied > balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        balance -= applied;
    }
}

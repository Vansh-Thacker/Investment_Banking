package uk.co.asepstrath.bank.cash;
import uk.co.asepstrath.bank.Account;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

// consider transaction types by number and not string, devs would know eg 1 = deposit and 2 = withdraw. not sure if 
// needed or all the bonuses but im thinking might be easier to preform == operations on numbers and so on


public class CashService {
    private final Account account;
    private final List<CashTransaction> history = new ArrayList<>();

    private static final int DAILY_DEPOSIT_LIMIT = 10000; // Daily deposit limit -- are we setting values in pence or gbp?


    public CashService(Account account) {
        this.account = account; //store Account inside this.account so we can deposit withraw etc
    }

    private static String formatP(int pounds) { //formatting for good looking messages, can be expanded to wformat other stuff later- needs changed if our money is displayed in pence ( just pounds / 100)
        return String.format("£%, .2f", (double) pounds);
    }


    public void deposit(int amount) {
        int todayDeposits = todayDepositCount();
        //check if deposit doesnt exced daily limit
        if (todayDeposits + amount > DAILY_DEPOSIT_LIMIT) {
            throw new IllegalStateException(String.format("Your Deposit would exceed your daily limit. Todays deposits: %s, Attempted: %s, Limit: %s",
             formatP(todayDeposits),
             formatP(amount),
             formatP(DAILY_DEPOSIT_LIMIT)));
        }

        //deposit money
        account.deposit(amount);

        //add to history
        addTransaction("deposit", amount);

    }


    public void withdraw(int amount) {
        account.withdraw(amount);
        // add to history
        addTransaction("withdraw", amount);
    }


    public int getBalance() {
        return account.getBalance();
    }


    public List<CashTransaction> getHistory() {
        return history;
    }


    private int todayDepositCount() {
        LocalDate today = LocalDate.now();
        int total = 0;

        for (CashTransaction transaction : history) {
            if (transaction.type.equals("deposit") && transaction.timestamp.toLocalDate().equals(today)) { //if transaction is deposit and it happened today sum them
                total = total + transaction.amount;
            }
        }

        return total;
    }


    private void addTransaction(String type, int amount) {
        CashTransaction transaction = new CashTransaction();
        transaction.timestamp = LocalDateTime.now();
        transaction.type = type; // deposit or withdrawal
        transaction.amount = amount;
        transaction.balanceAfter = account.getBalance(); // balance after transaction

        history.add(transaction);
    }


    /*public void changeHistory(int amount) {
        CashTransaction transaction = new CashTransaction();

        history.add(transaction);
    }*/


}

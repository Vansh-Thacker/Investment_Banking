package uk.co.asepstrath.bank.cash;
import uk.co.asepstrath.bank.Account;
import java.util.ArrayList;
import java.util.List;

public class CashService {
    private final Account account;
    private final List<CashTransaction> history = new ArrayList<>();

    public CashService(Account account) {
        this.account = account; //store Account inside this.account so we can deposit withraw etc
    }

    public void deposit(int amount) {
        account.deposit(amount);
        //add to history
    }

    public void withdraw(int amount) {
        account.withdraw(amount);
        // add to history
    }

    public int getBalance() {
        return account.getBalance();
    }

    public List<CashTransaction> getHistory() {
        return history;
    }


}

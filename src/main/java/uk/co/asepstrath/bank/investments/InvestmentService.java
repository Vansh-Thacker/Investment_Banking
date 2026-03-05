package uk.co.asepstrath.bank.investments;

import uk.co.asepstrath.bank.Account;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

public class InvestmentService {

    private final Account account;
    private final Map<String, Position> positions = new HashMap<>();
    private final List<InvestmentTransaction> history = new ArrayList<>();

    public InvestmentService(Account account) {
        this.account = account;
    }

    public void buy(String ticker, int shares, double price) {
        //Deduct cash
        //Update position
        //Record transaction
    }

    public void sell(String ticker, int shares, double price) {
        //check if you can sell that amount
        // Add cash
        // Update position
        // Record transaction
    }

    public void importCsv(InputStream csvStream) {
    //imports the csv file and stores it into an investment transaction object
    }

    private void record(String type, String ticker, int shares, double price) {
        //save transaction to the history
        InvestmentTransaction invRecord = new InvestmentTransaction();
        invRecord.timestamp = LocalDateTime.now();
        invRecord.type = type;
        invRecord.ticker = ticker;
        invRecord.shares = shares;
        invRecord.price = price;
        history.add(invRecord);
    }

    public Collection<Position> getPositions() {
        return positions.values(); //get current stock holdings
    }

    public Position getPosition(String ticker) {
        return positions.get(ticker); //return specific stock holding
    }

    public List<InvestmentTransaction> getHistory() {
        return history; //returns buy and sell history
    }
}


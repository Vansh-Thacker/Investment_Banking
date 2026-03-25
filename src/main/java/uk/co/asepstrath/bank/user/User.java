package uk.co.asepstrath.bank.user;

import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;

public class User {
    private final String id;
    private final String name;
    private final String password; // plain text
    private int balancePence;
    private InvestmentService investmentService;
    private Account account;

    public User(String id, String name, String password, int balancePence) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.balancePence = balancePence;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public int getBalancePence() { return balancePence; }
    public InvestmentService getInvestmentService() { return investmentService; }
    public Account getAccount() { return account; }


    public void setBalancePence(int balancePence) {
        this.balancePence = balancePence;
    }

    public void setInvestmentService(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
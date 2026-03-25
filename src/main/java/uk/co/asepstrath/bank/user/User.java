package uk.co.asepstrath.bank.user;

public class User {
    private final String id;
    private final String username;
    private final String password;
    private int balance;

    public User(String username, String password, int balance) {
        this(null, username, password, balance);
    }

    public User(String id, String username, String password, int balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
}
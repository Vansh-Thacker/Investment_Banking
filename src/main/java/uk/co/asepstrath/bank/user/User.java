package uk.co.asepstrath.bank.user;

public class User {
    private final String id;
    private final String name;
    private final String password; // plain text
    private int balancePence;

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

    public void setBalancePence(int balancePence) {
        this.balancePence = balancePence;
    }
}
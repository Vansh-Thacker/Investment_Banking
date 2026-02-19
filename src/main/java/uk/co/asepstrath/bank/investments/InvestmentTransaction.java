package uk.co.asepstrath.bank.investments;

import java.time.LocalDateTime;

public class InvestmentTransaction {
    public LocalDateTime timestamp;
    public String type; // BUY or SELL
    public String ticker;
    public int shares;
    public double price;
}


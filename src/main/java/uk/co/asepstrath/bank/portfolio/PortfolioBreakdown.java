package uk.co.asepstrath.bank.portfolio;

public class PortfolioBreakdown {
    public String label;
    public double totalValue;
    public double percentage;

    public PortfolioBreakdown(String label, double totalValue, double percentage) {
        this.label = label;
        this.totalValue = totalValue;
        this.percentage = percentage;
    }
}
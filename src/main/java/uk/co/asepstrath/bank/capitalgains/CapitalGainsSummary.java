package uk.co.asepstrath.bank.capitalgains;

public class CapitalGainsSummary {

    public double totalGains;
    public double totalLosses;
    public double netGain;

    public double allowanceUsed;
    public double allowanceRemaining;

    public double taxableGain;
    public double taxRate;
    public double taxOwed;

    public CapitalGainsSummary(
            double totalGains,
            double totalLosses,
            double netGain,
            double allowanceUsed,
            double allowanceRemaining,
            double taxableGain,
            double taxRate,
            double taxOwed
    ) {
        this.totalGains = totalGains;
        this.totalLosses = totalLosses;
        this.netGain = netGain;
        this.allowanceUsed = allowanceUsed;
        this.allowanceRemaining = allowanceRemaining;
        this.taxableGain = taxableGain;
        this.taxRate = taxRate;
        this.taxOwed = taxOwed;
    }

    public double getTotalGains() { return totalGains; }
    public double getTotalLosses() { return totalLosses; }
    public double getNetGain() { return netGain; }
    public double getAllowanceUsed() { return allowanceUsed; }
    public double getAllowanceRemaining() { return allowanceRemaining; }
    public double getTaxableGain() { return taxableGain; }
    public double getTaxRate() { return taxRate; }
    public double getTaxOwed() { return taxOwed; }
}



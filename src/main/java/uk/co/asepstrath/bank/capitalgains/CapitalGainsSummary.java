package uk.co.asepstrath.bank.capitalgains;

import uk.co.asepstrath.bank.investments.InvestmentTransaction;

import java.util.List;

public class CapitalGainsSummary {
    public int taxYearStart;          // e.g. 2024 for tax year 2024/25
    public String taxYearLabel;       // e.g. "2024/25"

    public double totalGains;         // sum of positive realizedGainLoss
    public double totalLosses;        // sum of negative realizedGainLoss
    public double netGain;            // totalGains + totalLosses

    public double allowance;          // fixed: 3000
    public double allowanceUsed;      // min(max(netGain, 0), allowance)
    public double allowanceRemaining; // allowance - allowanceUsed

    public double taxableGain;        // max(0, netGain - allowance)
    public double taxRate;            // fixed: 0.24
    public double taxOwed;            // taxableGain * taxRate

    public List<InvestmentTransaction> transactions; // SELL transactions in this tax year
}


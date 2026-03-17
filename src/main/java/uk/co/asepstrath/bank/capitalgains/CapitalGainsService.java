package uk.co.asepstrath.bank.capitalgains;

import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.investments.InvestmentTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CapitalGainsService {

    private static final double ALLOWANCE = 3000.0;
    private static final double TAX_RATE = 0.24;

    private final InvestmentService investmentService;

    public CapitalGainsService(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    /** All SELL transactions from investment history. */
    public List<InvestmentTransaction> getAllSellTransactions() {
        return investmentService.getHistory().stream()
                .filter(tx -> "SELL".equalsIgnoreCase(tx.type))
                .collect(Collectors.toList());
    }

    /** Determine UK tax year info for a given timestamp. */
    public TaxYearInfo getTaxYear(LocalDateTime timestamp) {
        LocalDate date = timestamp.toLocalDate();
        int year = date.getYear();
        LocalDate taxYearStart = LocalDate.of(year, 4, 6); // 6 April year

        int startYear;
        if (date.isBefore(taxYearStart)) {
            startYear = year - 1;
        } else {
            startYear = year;
        }

        TaxYearInfo info = new TaxYearInfo();
        info.startYear = startYear;
        info.startDate = LocalDate.of(startYear, 4, 6);
        info.endDate = LocalDate.of(startYear + 1, 4, 5);
        info.label = startYear + "/" + String.valueOf((startYear + 1) % 100);
        return info;
    }

    /** All distinct tax years (startYear) present in SELL history. */
    public List<TaxYearInfo> getAvailableTaxYears() {
        Map<Integer, TaxYearInfo> byYear = new HashMap<>();
        for (InvestmentTransaction tx : getAllSellTransactions()) {
            if (tx.timestamp == null) {
                continue;
            }
            TaxYearInfo info = getTaxYear(tx.timestamp);
            byYear.putIfAbsent(info.startYear, info);
        }
        return byYear.values().stream()
                .sorted(Comparator.comparingInt(t -> t.startYear))
                .collect(Collectors.toList());
    }

    /** SELL transactions for a given tax year start (e.g. 2024 for 2024/25). */
    public List<InvestmentTransaction> getTransactionsForYear(int taxYearStart) {
        TaxYearInfo info = buildTaxYearInfo(taxYearStart);
        return getAllSellTransactions().stream()
                .filter(tx -> tx.timestamp != null)
                .filter(tx -> {
                    LocalDate d = tx.timestamp.toLocalDate();
                    return !d.isBefore(info.startDate) && !d.isAfter(info.endDate);
                })
                .sorted(Comparator.comparing(t -> t.timestamp))
                .collect(Collectors.toList());
    }

    /** Summary for a given tax year start (e.g. 2024 for 2024/25). */
    public CapitalGainsSummary getSummaryForYear(int taxYearStart) {
        TaxYearInfo info = buildTaxYearInfo(taxYearStart);
        List<InvestmentTransaction> txs = getTransactionsForYear(taxYearStart);

        double totalGains = 0.0;
        double totalLosses = 0.0;

        for (InvestmentTransaction tx : txs) {
            double gain = tx.realizedGainLoss != null ? tx.realizedGainLoss : 0.0;
            if (gain > 0) {
                totalGains += gain;
            } else if (gain < 0) {
                totalLosses += gain;
            }
        }

        double netGain = totalGains + totalLosses;
        double allowanceUsed = Math.max(0.0, Math.min(ALLOWANCE, netGain));
        double allowanceRemaining = ALLOWANCE - allowanceUsed;
        if (allowanceRemaining < 0) {
            allowanceRemaining = 0;
        }

        double taxableGain = Math.max(0.0, netGain - ALLOWANCE);
        double taxOwed = taxableGain * TAX_RATE;

        CapitalGainsSummary summary = new CapitalGainsSummary();
        summary.taxYearStart = info.startYear;
        summary.taxYearLabel = info.label;

        summary.totalGains = totalGains;
        summary.totalLosses = totalLosses;
        summary.netGain = netGain;

        summary.allowance = ALLOWANCE;
        summary.allowanceUsed = allowanceUsed;
        summary.allowanceRemaining = allowanceRemaining;

        summary.taxableGain = taxableGain;
        summary.taxRate = TAX_RATE;
        summary.taxOwed = taxOwed;

        summary.transactions = txs;
        return summary;
    }

    /** Helper to build TaxYearInfo from a start year without needing a timestamp. */
    private TaxYearInfo buildTaxYearInfo(int startYear) {
        TaxYearInfo info = new TaxYearInfo();
        info.startYear = startYear;
        info.startDate = LocalDate.of(startYear, 4, 6);
        info.endDate = LocalDate.of(startYear + 1, 4, 5);
        info.label = startYear + "/" + String.valueOf((startYear + 1) % 100);
        return info;
    }

    /** Current tax year based on today's date. */
    public TaxYearInfo getCurrentTaxYear() {
        return getTaxYear(LocalDateTime.now());
    }
}


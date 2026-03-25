package uk.co.asepstrath.bank.investments;

import java.util.List;

/// paginated transaction list
public class TransactionListResponse {
    public List<InvestmentTransaction> transactions;
    public int total;
}

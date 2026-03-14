package uk.co.asepstrath.bank.investments;

import java.util.List;

/** Paginated transaction list: slice of transactions plus total count for navigation. */
public class TransactionListResponse {
    public List<InvestmentTransaction> transactions;
    public int total;
}

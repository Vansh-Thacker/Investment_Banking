package uk.co.asepstrath.bank.cash;

import uk.co.asepstrath.bank.theCashAccount;
import uk.co.scotbank.CashAccountTests;

public class theCashAccountTests extends CashAccountTests<theCashAccount> {
    @Override
    protected theCashAccount createAccount() {
        return new theCashAccount();
    }
}


package model;

import pattern.AccountFactory;
import pattern.InterestFeeStrategy;

import java.time.LocalDate;

/** A normal savings account: no overdraft, no fixed term. A Savings account IS an Account. */
public class SavingsAccount extends Account {

    public SavingsAccount(String accountNumber, String customerId, double balance, LocalDate openedDate,
                          boolean open, InterestFeeStrategy strategy) {
        super(accountNumber, customerId, balance, openedDate, open, strategy);
    }

    @Override
    public String getType() {
        return AccountFactory.SAVINGS;
    }

    @Override
    public String getTermsDescription() {
        return "Savings: no overdraft, no fixed term";
    }
}

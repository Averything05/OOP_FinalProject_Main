package model;

import pattern.AccountFactory;
import pattern.BankConfig;
import pattern.InterestFeeStrategy;
import util.Money;

import java.time.LocalDate;

/** A checking account. It may go below zero, up to the bank's overdraft limit. */
public class CheckingAccount extends Account {

    public CheckingAccount(String accountNumber, String customerId, double balance, LocalDate openedDate,
                           boolean open, InterestFeeStrategy strategy) {
        super(accountNumber, customerId, balance, openedDate, open, strategy);
    }

    @Override
    public String getType() {
        return AccountFactory.CHECKING;
    }

    @Override
    protected double getMinimumAllowedBalance() {
        return -BankConfig.getInstance().getOverdraftLimit();
    }

    @Override
    public String getTermsDescription() {
        return "Checking: overdraft limit PHP " + Money.format(BankConfig.getInstance().getOverdraftLimit());
    }
}

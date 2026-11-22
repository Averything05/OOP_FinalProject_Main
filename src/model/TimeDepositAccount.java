package model;

import exception.InvalidRequestException;
import exception.TermNotMaturedException;
import pattern.AccountFactory;
import pattern.InterestFeeStrategy;

import java.time.LocalDate;

/**
 * A time deposit: one opening deposit, a fixed term in months, and no withdrawal
 * (or closing) before the maturity date.
 */
public class TimeDepositAccount extends Account {
    private final int termMonths;

    public TimeDepositAccount(String accountNumber, String customerId, double balance, LocalDate openedDate,
                              boolean open, int termMonths, InterestFeeStrategy strategy) {
        super(accountNumber, customerId, balance, openedDate, open, strategy);
        if (termMonths < 1) {
            throw new InvalidRequestException("A time deposit needs a term of at least 1 month.");
        }
        this.termMonths = termMonths;
    }

    @Override
    public String getType() {
        return AccountFactory.TIME_DEPOSIT;
    }

    @Override
    public int getTermMonths() {
        return termMonths;
    }

    public LocalDate getMaturityDate() {
        return getOpenedDate().plusMonths(termMonths);
    }

    public boolean isMatured() {
        return !LocalDate.now().isBefore(getMaturityDate());
    }

    @Override
    protected void checkWithdrawalAllowed() throws TermNotMaturedException {
        if (!isMatured()) {
            throw new TermNotMaturedException(getAccountNumber(), getMaturityDate());
        }
    }

    @Override
    protected void checkDepositAllowed() {
        throw new InvalidRequestException("Time deposit " + getAccountNumber()
                + " does not accept additional deposits.");
    }

    @Override
    public String getTermsDescription() {
        return "Time deposit: " + termMonths + " months, matures " + getMaturityDate()
                + (isMatured() ? " (matured)" : " (not yet matured)");
    }
}

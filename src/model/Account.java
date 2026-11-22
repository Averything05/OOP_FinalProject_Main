package model;

import exception.InsufficientFundsException;
import exception.InvalidRequestException;
import exception.TermNotMaturedException;
import pattern.InterestFeeStrategy;
import util.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ABSTRACTION + ENCAPSULATION: the common rules of every bank account.
 * All fields are private; the balance only changes through deposit, withdraw,
 * creditInterest, chargeFee and close, and each of them validates first.
 * Subclasses change behaviour through the small hook methods below (polymorphism),
 * and the monthly interest and fee rules come from an InterestFeeStrategy (Strategy pattern).
 */
public abstract class Account {
    private final String accountNumber;
    private final String customerId;
    private final LocalDate openedDate;
    private final InterestFeeStrategy strategy;
    private final List<Transaction> history = new ArrayList<>();
    private double balance;
    private boolean open;

    protected Account(String accountNumber, String customerId, double balance, LocalDate openedDate,
                      boolean open, InterestFeeStrategy strategy) {
        if (isBlank(accountNumber) || isBlank(customerId)) {
            throw new InvalidRequestException("Account number and customer ID are required.");
        }
        if (openedDate == null || strategy == null) {
            throw new InvalidRequestException("Opened date and interest/fee strategy are required.");
        }
        if (Double.isNaN(balance) || Double.isInfinite(balance)) {
            throw new InvalidRequestException("Balance is not a valid number.");
        }
        this.accountNumber = accountNumber.trim();
        this.customerId = customerId.trim();
        this.balance = Money.round(balance);
        this.openedDate = openedDate;
        this.open = open;
        this.strategy = strategy;
    }

    // ----- what every subclass must define -----

    /** @return the type code stored in the data file, for example SAVINGS */
    public abstract String getType();

    /** @return a one-line description of the special rules of this account type */
    public abstract String getTermsDescription();

    // ----- hooks that subclasses may override -----

    /** How low the balance may go. Savings and time deposits: 0. Checking allows an overdraft. */
    protected double getMinimumAllowedBalance() {
        return 0;
    }

    /** Throws if money may not be taken out right now. Default: always allowed. */
    protected void checkWithdrawalAllowed() throws TermNotMaturedException {
    }

    /** Throws if money may not be added right now. Default: always allowed. */
    protected void checkDepositAllowed() {
    }

    /** @return the fixed term in months, or 0 for accounts without a term */
    public int getTermMonths() {
        return 0;
    }

    // ----- operations -----

    /** Throws InvalidRequestException if this account is closed or does not accept deposits. */
    public void ensureCanDeposit() {
        requireOpen();
        checkDepositAllowed();
    }

    /** Adds money to the account. */
    public void deposit(double amount) {
        requireValidAmount(amount);
        ensureCanDeposit();
        balance = Money.round(balance + amount);
    }

    /**
     * Takes money out of the account.
     *
     * @throws InsufficientFundsException if the account cannot give that much
     * @throws TermNotMaturedException    for a time deposit that has not matured yet
     */
    public void withdraw(double amount) throws InsufficientFundsException, TermNotMaturedException {
        requireValidAmount(amount);
        requireOpen();
        checkWithdrawalAllowed();
        double available = Money.round(balance - getMinimumAllowedBalance());
        if (amount > available) {
            throw new InsufficientFundsException(accountNumber, Money.round(amount - available));
        }
        balance = Money.round(balance - amount);
    }

    /** Adds earned interest. Unlike deposit(), this is allowed on a time deposit. */
    public void creditInterest(double interest) {
        requireValidAmount(interest);
        requireOpen();
        balance = Money.round(balance + interest);
    }

    /**
     * Collects a fee, but never pushes the balance below the minimum allowed balance.
     *
     * @return the amount actually charged (may be less than the fee, or 0)
     */
    public double chargeFee(double fee) {
        requireOpen();
        double room = Math.max(0, Money.round(balance - getMinimumAllowedBalance()));
        double charged = Math.min(Money.round(fee), room);
        balance = Money.round(balance - charged);
        return charged;
    }

    /**
     * Closes the account and pays out the balance.
     *
     * @return the amount paid out
     * @throws TermNotMaturedException if this is a time deposit that has not matured
     */
    public double close() throws TermNotMaturedException {
        requireOpen();
        checkWithdrawalAllowed();
        if (balance < 0) {
            throw new InvalidRequestException("Cannot close " + accountNumber + ": it is overdrawn by PHP "
                    + Money.format(-balance) + ". Deposit that amount first.");
        }
        double payout = balance;
        balance = 0;
        open = false;
        return payout;
    }

    /** @return this month's interest, or 0 for a closed account (rule comes from the Strategy) */
    public double computeMonthlyInterest() {
        return open ? Money.round(strategy.computeInterest(balance)) : 0;
    }

    /** @return this month's fee, or 0 for a closed account (rule comes from the Strategy) */
    public double computeMonthlyFee() {
        return open ? Money.round(strategy.computeFee(balance)) : 0;
    }

    /** Adds a line to the statement history. It does not change the balance. */
    public void recordTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new InvalidRequestException("Transaction is required.");
        }
        history.add(transaction);
    }

    // ----- read-only access -----

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isOpen() {
        return open;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    /** @return a read-only view of the transaction history, oldest first */
    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ----- private helpers -----

    private void requireOpen() {
        if (!open) {
            throw new InvalidRequestException("Account " + accountNumber + " is closed.");
        }
    }

    private static void requireValidAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new InvalidRequestException("Amount must be greater than zero.");
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

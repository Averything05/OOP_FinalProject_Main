package exception;

import util.Money;

/**
 * Thrown by Account.withdraw() when the account cannot give the requested amount.
 * Checked, because the user can recover by entering a smaller amount.
 * Carries the missing amount in {@link #getShortfall()}.
 */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 1L;

    private final double shortfall;

    public InsufficientFundsException(String accountNumber, double shortfall) {
        super("Insufficient funds in " + accountNumber + ". Short by PHP " + Money.format(shortfall) + ".");
        this.shortfall = shortfall;
    }

    /** @return how much more money would be needed to complete the withdrawal */
    public double getShortfall() {
        return shortfall;
    }
}

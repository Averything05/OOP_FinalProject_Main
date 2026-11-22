package exception;

/**
 * Base class for the checked business-rule errors of the bank system.
 * Anything that extends this class is an expected, recoverable problem
 * (for example: not enough money) that the menu catches and reports
 * with getMessage(), never with a stack trace.
 */
public class BankException extends Exception {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }
}

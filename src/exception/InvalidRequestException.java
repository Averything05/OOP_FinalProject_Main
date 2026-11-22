package exception;

/**
 * Thrown by the model classes when a value or an action breaks a business rule
 * (amount not positive, closed account, invalid name, minimum opening deposit, ...).
 * Unchecked (RuntimeException): it signals a rejected argument or state, so it does not
 * have to be declared on every method. The menu still catches it and prints getMessage().
 */
public class InvalidRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}

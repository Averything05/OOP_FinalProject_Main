package exception;

/**
 * Thrown by InputHelper when the input stream ends (for example Ctrl+D or piped input runs out).
 * Unchecked, because it can happen at any prompt; MenuController catches it once, in run(),
 * saves the data, and exits cleanly instead of looping forever.
 */
public class InputClosedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("No more input is available.");
    }
}

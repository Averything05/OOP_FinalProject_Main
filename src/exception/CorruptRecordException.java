package exception;

/**
 * Thrown by DataStore when one line of a data file is malformed.
 * Checked, so the file reader is forced to catch it, print a warning, and skip the line.
 */
public class CorruptRecordException extends Exception {
    private static final long serialVersionUID = 1L;

    public CorruptRecordException(String reason) {
        super(reason);
    }
}

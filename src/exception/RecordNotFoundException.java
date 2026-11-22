package exception;

/**
 * Thrown by BankService.getAccount() and getCustomer() when an ID does not exist.
 * Checked, because a mistyped ID is a normal user error that the menu handles.
 * One class serves both cases (DRY): the message reads "Account not found: A-9999"
 * or "Customer not found: C-777", and the missing ID is available from {@link #getRecordId()}.
 */
public class RecordNotFoundException extends BankException {
    private static final long serialVersionUID = 1L;

    private final String recordType;
    private final String recordId;

    /**
     * @param recordType what was searched for, for example "Account" or "Customer"
     * @param recordId   the ID that was not found
     */
    public RecordNotFoundException(String recordType, String recordId) {
        super(recordType + " not found: " + recordId);
        this.recordType = recordType;
        this.recordId = recordId;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getRecordId() {
        return recordId;
    }
}

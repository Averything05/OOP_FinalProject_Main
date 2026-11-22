package model;

/** The kinds of entries that can appear in an account statement. */
public enum TransactionType {
    DEPOSIT(true),
    WITHDRAWAL(false),
    TRANSFER_IN(true),
    TRANSFER_OUT(false),
    INTEREST(true),
    FEE(false);

    private final boolean credit;

    TransactionType(boolean credit) {
        this.credit = credit;
    }

    /** @return true if this type adds money to the account, false if it takes money out */
    public boolean isCredit() {
        return credit;
    }
}

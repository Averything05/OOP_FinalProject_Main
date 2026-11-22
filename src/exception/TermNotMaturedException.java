package exception;

import java.time.LocalDate;

/**
 * Thrown when money is taken out of a time deposit before its maturity date.
 * Checked, because the user can try again after the maturity date.
 */
public class TermNotMaturedException extends BankException {
    private static final long serialVersionUID = 1L;

    private final LocalDate maturityDate;

    public TermNotMaturedException(String accountNumber, LocalDate maturityDate) {
        super("Time deposit " + accountNumber + " has not matured. Funds can be withdrawn on " + maturityDate + ".");
        this.maturityDate = maturityDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}

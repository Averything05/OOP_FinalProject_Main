package pattern;

import exception.InvalidRequestException;
import model.Account;
import model.CheckingAccount;
import model.SavingsAccount;
import model.TimeDepositAccount;
import util.Money;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * FACTORY pattern: builds the right Account subclass (and gives it the right strategy)
 * from a type name. The menu and the file loader only ever see the Account type, never
 * SavingsAccount, CheckingAccount or TimeDepositAccount. To add a new account type,
 * only this class (plus the new account and strategy classes) changes.
 */
public final class AccountFactory {
    public static final String SAVINGS = "SAVINGS";
    public static final String CHECKING = "CHECKING";
    public static final String TIME_DEPOSIT = "TIME_DEPOSIT";

    private AccountFactory() {
    }

    /** @return the account types the bank offers, in menu order */
    public static List<String> supportedTypes() {
        return Arrays.asList(SAVINGS, CHECKING, TIME_DEPOSIT);
    }

    /** @return true if the user must also choose a term in months for this type */
    public static boolean requiresTerm(String type) {
        return TIME_DEPOSIT.equals(normalize(type));
    }

    /** Opens a brand-new account today. Rejects an opening deposit below the type's minimum. */
    public static Account openNew(String type, String accountNumber, String customerId,
                                  double initialDeposit, int termMonths) {
        String key = normalize(type);
        double minimum = BankConfig.getInstance().getMinimumOpeningDeposit(key);
        if (initialDeposit < minimum) {
            throw new InvalidRequestException("Minimum opening deposit for a " + key + " account is PHP "
                    + Money.format(minimum) + ".");
        }
        return restore(key, accountNumber, customerId, initialDeposit, LocalDate.now(), true, termMonths);
    }

    /** Rebuilds an account with known values (used by openNew and when loading from file). */
    public static Account restore(String type, String accountNumber, String customerId, double balance,
                                  LocalDate openedDate, boolean open, int termMonths) {
        switch (normalize(type)) {
            case SAVINGS:
                return new SavingsAccount(accountNumber, customerId, balance, openedDate, open,
                        AccountRates.SAVINGS);
            case CHECKING:
                return new CheckingAccount(accountNumber, customerId, balance, openedDate, open,
                        AccountRates.CHECKING);
            case TIME_DEPOSIT:
                return new TimeDepositAccount(accountNumber, customerId, balance, openedDate, open,
                        termMonths, AccountRates.TIME_DEPOSIT);
            default:
                throw new InvalidRequestException("Unsupported account type: " + type);
        }
    }

    private static String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }
}

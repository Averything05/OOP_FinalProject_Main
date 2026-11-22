package pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * SINGLETON pattern: the one set of bank-wide settings (limits, opening deposits, data folder).
 * Two different copies would be a real mistake (the alerts and the accounts could use different
 * limits), so the constructor is private and everyone calls getInstance().
 */
public final class BankConfig {
    private static final BankConfig INSTANCE = new BankConfig();

    private final String bankName = "CvSU Bank";
    private final String dataDirectory = "data";
    private final double lowBalanceThreshold = 500.00;
    private final double largeTransactionThreshold = 50000.00;
    private final double overdraftLimit = 2000.00;
    private final Map<String, Double> minimumOpeningDeposits = new HashMap<>();

    private BankConfig() {
        minimumOpeningDeposits.put(AccountFactory.SAVINGS, 500.00);
        minimumOpeningDeposits.put(AccountFactory.CHECKING, 1000.00);
        minimumOpeningDeposits.put(AccountFactory.TIME_DEPOSIT, 5000.00);
    }

    public static BankConfig getInstance() {
        return INSTANCE;
    }

    public String getBankName() {
        return bankName;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public double getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    public double getLargeTransactionThreshold() {
        return largeTransactionThreshold;
    }

    /** How far a checking account may go below zero. */
    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    /** @return the minimum amount needed to open this type of account (0 if the type is unknown) */
    public double getMinimumOpeningDeposit(String accountType) {
        Double minimum = minimumOpeningDeposits.get(accountType);
        return minimum == null ? 0 : minimum;
    }
}

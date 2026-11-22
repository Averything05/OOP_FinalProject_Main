package pattern;

import model.Account;
import model.Transaction;
import util.Money;
import util.Ui;

/**
 * OBSERVER pattern: anything that wants to react after a transaction implements this interface.
 * BankService (the subject) only knows this interface, so adding a new alert never changes BankService.
 * Because the interface has one method, each alert is a short lambda created by a static method
 * below. A new alert is one new static method here plus one addObserver line in Main.
 */
@FunctionalInterface
public interface AccountObserver {

    /** Called by BankService after every recorded transaction. */
    void onTransaction(Account account, Transaction transaction);

    /** Prints an alert when a debit leaves an open account below the low-balance limit. */
    static AccountObserver lowBalance() {
        return (account, transaction) -> {
            double limit = BankConfig.getInstance().getLowBalanceThreshold();
            boolean isDebit = !transaction.getType().isCredit();
            if (account.isOpen() && isDebit && account.getBalance() < limit) {
                Ui.alert("Low balance: " + account.getAccountNumber() + " now has PHP "
                        + Money.format(account.getBalance()) + " (below PHP " + Money.format(limit) + ").");
            }
        };
    }

    /** Prints an alert when a single transaction is at or above the large-transaction limit. */
    static AccountObserver largeTransaction() {
        return (account, transaction) -> {
            double limit = BankConfig.getInstance().getLargeTransactionThreshold();
            if (transaction.getAmount() >= limit) {
                Ui.alert("Large transaction: " + transaction.getType() + " of PHP "
                        + Money.format(transaction.getAmount()) + " on " + account.getAccountNumber()
                        + ". Please verify.");
            }
        };
    }
}

package service;

import exception.RecordNotFoundException;
import exception.InsufficientFundsException;
import exception.InvalidRequestException;
import exception.TermNotMaturedException;
import model.Account;
import model.Customer;
import model.Transaction;
import model.TransactionType;
import pattern.AccountFactory;
import pattern.AccountObserver;
import storage.DataStore;
import util.Repository;
import util.Ui;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The business logic of the bank. It is also the SUBJECT of the Observer pattern:
 * after every recorded transaction it tells all registered AccountObservers.
 * It knows nothing about the menu or about file formats (Single Responsibility).
 */
public class BankService {
    private final Repository<Customer> customers = new Repository<>(Customer::getId);
    private final Repository<Account> accounts = new Repository<>(Account::getAccountNumber);
    private final List<AccountObserver> observers = new ArrayList<>();
    private final DataStore store;

    private int nextCustomerNo = 1;
    private int nextAccountNo = 1001;
    private int nextTransactionNo = 1;

    public BankService(DataStore store) {
        this.store = store;
    }

    // ----- Observer registration -----

    public void addObserver(AccountObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(Account account, Transaction transaction) {
        observers.forEach(observer -> observer.onTransaction(account, transaction));
    }

    // ----- customers and accounts -----

    public Customer registerCustomer(String name, String contactNumber) {
        Customer customer = new Customer(String.format("C-%03d", nextCustomerNo), name, contactNumber);
        nextCustomerNo++;
        customers.add(customer);
        return customer;
    }

    public Customer getCustomer(String customerId) throws RecordNotFoundException {
        String id = normalize(customerId);
        return customers.findById(id).orElseThrow(() -> new RecordNotFoundException("Customer", id));
    }

    public Account getAccount(String accountNumber) throws RecordNotFoundException {
        String number = normalize(accountNumber);
        return accounts.findById(number).orElseThrow(() -> new RecordNotFoundException("Account", number));
    }

    /** @return the customer's name, or "(unknown)" if the ID is not registered */
    public String getCustomerName(String customerId) {
        return customers.findById(customerId).map(Customer::getName).orElse("(unknown)");
    }

    public List<Account> listAccounts() {
        return accounts.findAll();
    }

    public int getCustomerCount() {
        return customers.size();
    }

    public int getAccountCount() {
        return accounts.size();
    }

    /** Opens an account for an existing customer and records the opening deposit. */
    public Account openAccount(String customerId, String type, double initialDeposit, int termMonths)
            throws RecordNotFoundException {
        Customer customer = getCustomer(customerId);
        String accountNumber = String.format("A-%d", nextAccountNo);
        Account account = AccountFactory.openNew(type, accountNumber, customer.getId(), initialDeposit, termMonths);
        nextAccountNo++;
        accounts.add(account);
        customer.addAccount(account);
        record(account, TransactionType.DEPOSIT, initialDeposit, "Opening deposit");
        return account;
    }

    /** Closes an account and pays out the balance. @return the amount paid out */
    public double closeAccount(String accountNumber) throws RecordNotFoundException, TermNotMaturedException {
        Account account = getAccount(accountNumber);
        double payout = account.close();
        if (payout > 0) {
            record(account, TransactionType.WITHDRAWAL, payout, "Account closed - balance paid out");
        }
        return payout;
    }

    // ----- money movements -----

    public Transaction deposit(String accountNumber, double amount) throws RecordNotFoundException {
        Account account = getAccount(accountNumber);
        account.deposit(amount);
        return record(account, TransactionType.DEPOSIT, amount, "Cash deposit");
    }

    public Transaction withdraw(String accountNumber, double amount)
            throws RecordNotFoundException, InsufficientFundsException, TermNotMaturedException {
        Account account = getAccount(accountNumber);
        account.withdraw(amount);
        return record(account, TransactionType.WITHDRAWAL, amount, "Cash withdrawal");
    }

    /**
     * Moves money between two accounts. Everything that can fail is checked before any
     * balance changes, so a failed transfer never leaves the books half-done.
     */
    public void transfer(String fromNumber, String toNumber, double amount)
            throws RecordNotFoundException, InsufficientFundsException, TermNotMaturedException {
        Account from = getAccount(fromNumber);
        Account to = getAccount(toNumber);
        if (from == to) {
            throw new InvalidRequestException("Cannot transfer to the same account.");
        }
        to.ensureCanDeposit();
        from.withdraw(amount);
        to.deposit(amount);
        record(from, TransactionType.TRANSFER_OUT, amount, "Transfer to " + to.getAccountNumber());
        record(to, TransactionType.TRANSFER_IN, amount, "Transfer from " + from.getAccountNumber());
    }

    /**
     * Applies one month of interest and fees to every open account.
     * Both are computed from the balance before this month's changes.
     *
     * @return the INTEREST and FEE transactions that were created
     */
    public List<Transaction> applyMonthlyCharges() {
        List<Transaction> applied = new ArrayList<>();
        for (Account account : accounts.findWhere(Account::isOpen)) {
            double interest = account.computeMonthlyInterest();
            double fee = account.computeMonthlyFee();
            if (interest > 0) {
                account.creditInterest(interest);
                applied.add(record(account, TransactionType.INTEREST, interest, "Monthly interest"));
            }
            if (fee > 0) {
                double charged = account.chargeFee(fee);
                if (charged > 0) {
                    applied.add(record(account, TransactionType.FEE, charged, "Monthly maintenance fee"));
                }
            }
        }
        return applied;
    }

    /** Creates the Transaction, stores it in the account's history, and notifies the observers. */
    private Transaction record(Account account, TransactionType type, double amount, String description) {
        Transaction transaction = new Transaction(String.format("T-%05d", nextTransactionNo), account.getAccountNumber(),
                type, amount, account.getBalance(), LocalDateTime.now().withNano(0), description);
        nextTransactionNo++;
        account.recordTransaction(transaction);
        notifyObservers(account, transaction);
        return transaction;
    }

    // ----- persistence -----

    public void save() throws IOException {
        store.save(customers.findAll(), accounts.findAll());
    }

    /**
     * Loads saved data. Missing files give empty data; corrupt lines are skipped with a warning
     * (DataStore); records that point to something that does not exist are skipped here.
     */
    public void load() {
        try {
            for (Customer customer : store.loadCustomers()) {
                try {
                    customers.add(customer);
                } catch (InvalidRequestException e) {
                    Ui.warning("Skipped customer record: " + e.getMessage());
                }
            }
            for (Account account : store.loadAccounts()) {
                Optional<Customer> owner = customers.findById(account.getCustomerId());
                if (!owner.isPresent()) {
                    Ui.warning("Skipped account " + account.getAccountNumber() + ": unknown customer "
                            + account.getCustomerId() + ".");
                    continue;
                }
                try {
                    accounts.add(account);
                    owner.get().addAccount(account);
                } catch (InvalidRequestException e) {
                    Ui.warning("Skipped account record: " + e.getMessage());
                }
            }
            for (Transaction transaction : store.loadTransactions()) {
                Optional<Account> owner = accounts.findById(transaction.getAccountNumber());
                if (owner.isPresent()) {
                    owner.get().recordTransaction(transaction);
                } else {
                    Ui.warning("Skipped transaction " + transaction.getId() + ": unknown account "
                            + transaction.getAccountNumber() + ".");
                }
            }
        } catch (IOException e) {
            Ui.error("Could not read saved data: " + e.getMessage() + ". Continuing with what was loaded.");
        }
        refreshCounters();
    }

    /** Makes the next IDs continue after the largest IDs that were loaded. */
    private void refreshCounters() {
        nextCustomerNo = maxSuffix(customers.findAll(), Customer::getId) + 1;
        nextAccountNo = Math.max(1000, maxSuffix(accounts.findAll(), Account::getAccountNumber)) + 1;
        int maxTransaction = 0;
        for (Account account : accounts.findAll()) {
            maxTransaction = Math.max(maxTransaction, maxSuffix(account.getHistory(), Transaction::getId));
        }
        nextTransactionNo = maxTransaction + 1;
    }

    /** Largest number found after the last '-' in the IDs, for example 1003 in "A-1003". */
    private static <T> int maxSuffix(Collection<T> items, Function<T, String> idOf) {
        int max = 0;
        for (T item : items) {
            String id = idOf.apply(item);
            try {
                max = Math.max(max, Integer.parseInt(id.substring(id.lastIndexOf('-') + 1)));
            } catch (NumberFormatException e) {
                Ui.warning("ID without a number was ignored when numbering: " + id);
            }
        }
        return max;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toUpperCase();
    }
}

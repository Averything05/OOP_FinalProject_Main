package app;

import exception.BankException;
import exception.InputClosedException;
import exception.InvalidRequestException;
import model.Account;
import model.Customer;
import model.Transaction;
import pattern.AccountFactory;
import pattern.BankConfig;
import service.BankService;
import util.InputHelper;
import util.Money;
import util.Ui;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The menu-driven console interface. It only reads input, calls BankService, and prints results.
 * Menu items are stored in a map of (number -> title + action), so adding a menu option is one
 * addItem(...) line and the loop never changes (Open/Closed Principle).
 */
public class MenuController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** One menu action. It may throw a BankException, which execute() reports to the user. */
    @FunctionalInterface
    private interface MenuAction {
        void execute() throws BankException;
    }

    /** A numbered menu entry. savesData says whether the data files must be saved after it succeeds. */
    private static final class MenuItem {
        private final String title;
        private final MenuAction action;
        private final boolean savesData;

        private MenuItem(String title, MenuAction action, boolean savesData) {
            this.title = title;
            this.action = action;
            this.savesData = savesData;
        }
    }

    private final BankService service;
    private final InputHelper input;
    private final Map<Integer, MenuItem> menu = new LinkedHashMap<>();

    public MenuController(BankService service, InputHelper input) {
        this.service = service;
        this.input = input;
        addItem("Register customer", this::registerCustomer, true);
        addItem("Open account", this::openAccount, true);
        addItem("Close account", this::closeAccount, true);
        addItem("Deposit", this::deposit, true);
        addItem("Withdraw", this::withdraw, true);
        addItem("Transfer", this::transfer, true);
        addItem("Apply monthly interest and fees", this::applyMonthlyCharges, true);
        addItem("Print account statement", this::printStatement, false);
        addItem("Customer summary", this::showCustomerSummary, false);
        addItem("List all accounts", this::listAccounts, false);
    }

    private void addItem(String title, MenuAction action, boolean savesData) {
        menu.put(menu.size() + 1, new MenuItem(title, action, savesData));
    }

    /** Loads the data, loops until the user chooses Exit, then saves. */
    public void run() {
        service.load();
        Ui.info("Loaded " + service.getCustomerCount() + " customer(s) and " + service.getAccountCount()
                + " account(s).");
        int exitChoice = menu.size() + 1;
        try {
            boolean running = true;
            while (running) {
                printMenu(exitChoice);
                int choice = input.readInt("Enter choice [1-" + exitChoice + "]: ", 1, exitChoice);
                if (choice == exitChoice) {
                    running = false;
                } else {
                    execute(menu.get(choice));
                }
            }
        } catch (InputClosedException e) {
            System.out.println();
            Ui.info("Input ended. Saving data and exiting.");
        }
        if (saveData()) {
            Ui.info("Data saved. Goodbye!");
        }
    }

    private void printMenu(int exitChoice) {
        Ui.header(BankConfig.getInstance().getBankName() + " - Account Management");
        menu.forEach((number, item) -> System.out.printf("[%d] %s%n", number, item.title));
        System.out.printf("[%d] Save and exit%n", exitChoice);
    }

    /** Runs one menu item. Business errors are shown as a short message, never as a stack trace. */
    private void execute(MenuItem item) {
        try {
            item.action.execute();
            if (item.savesData) {
                saveData();
            }
        } catch (BankException | InvalidRequestException e) {
            Ui.error(e.getMessage());
        }
    }

    private boolean saveData() {
        try {
            service.save();
            return true;
        } catch (IOException e) {
            Ui.error("Could not save data: " + e.getMessage());
            return false;
        }
    }

    // ----- menu actions -----

    private void registerCustomer() {
        String name = input.readNonEmpty("Customer name: ");
        String contact = input.readNonEmpty("Contact number: ");
        Customer customer = service.registerCustomer(name, contact);
        Ui.success("Registered " + customer.getName() + " with customer ID " + customer.getId() + ".");
    }

    private void openAccount() throws BankException {
        Customer customer = service.getCustomer(input.readNonEmpty("Customer ID: "));
        Ui.info("Customer: " + customer.getName());
        List<String> types = AccountFactory.supportedTypes();
        for (int i = 0; i < types.size(); i++) {
            System.out.printf("  [%d] %-13s minimum opening deposit PHP %s%n", i + 1, types.get(i),
                    Money.format(BankConfig.getInstance().getMinimumOpeningDeposit(types.get(i))));
        }
        String type = types.get(input.readInt("Account type [1-" + types.size() + "]: ", 1, types.size()) - 1);
        double deposit = input.readAmount("Opening deposit (PHP): ");
        int term = AccountFactory.requiresTerm(type) ? input.readInt("Term in months [1-60]: ", 1, 60) : 0;
        Account account = service.openAccount(customer.getId(), type, deposit, term);
        Ui.success("Opened " + account.getType() + " account " + account.getAccountNumber() + " for "
                + customer.getName() + " with PHP " + Money.format(account.getBalance()) + ".");
    }

    private void closeAccount() throws BankException {
        Account account = service.getAccount(readAccountNumber("Account number to close: "));
        if (!account.isOpen()) {
            Ui.error("Account " + account.getAccountNumber() + " is already closed.");
            return;
        }
        Ui.info(account.getAccountNumber() + " balance: PHP " + Money.format(account.getBalance()));
        if (!input.confirm("Close this account and pay out the balance?")) {
            Ui.info("Closing cancelled.");
            return;
        }
        double payout = service.closeAccount(account.getAccountNumber());
        Ui.success("Account " + account.getAccountNumber() + " closed. Paid out PHP " + Money.format(payout) + ".");
    }

    private void deposit() throws BankException {
        String number = readAccountNumber("Account number: ");
        double amount = input.readAmount("Amount to deposit (PHP): ");
        Transaction transaction = service.deposit(number, amount);
        Ui.success("Deposited PHP " + Money.format(amount) + " to " + number + ". New balance: PHP "
                + Money.format(transaction.getBalanceAfter()) + ".");
    }

    private void withdraw() throws BankException {
        String number = readAccountNumber("Account number: ");
        double amount = input.readAmount("Amount to withdraw (PHP): ");
        Transaction transaction = service.withdraw(number, amount);
        Ui.success("Withdrew PHP " + Money.format(amount) + " from " + number + ". New balance: PHP "
                + Money.format(transaction.getBalanceAfter()) + ".");
    }

    private void transfer() throws BankException {
        String from = readAccountNumber("From account: ");
        String to = readAccountNumber("To account: ");
        double amount = input.readAmount("Amount to transfer (PHP): ");
        service.transfer(from, to, amount);
        Ui.success("Transferred PHP " + Money.format(amount) + " from " + from.toUpperCase() + " to "
                + to.toUpperCase() + ".");
    }

    private void applyMonthlyCharges() {
        List<Transaction> applied = service.applyMonthlyCharges();
        if (applied.isEmpty()) {
            Ui.info("No interest or fees to apply.");
            return;
        }
        System.out.printf("%-8s %-10s %14s %16s%n", "Account", "Type", "Amount", "New balance");
        Ui.line(52);
        applied.forEach(t -> System.out.printf("%-8s %-10s %14s %16s%n", t.getAccountNumber(), t.getType(),
                String.format("%+,.2f", t.getSignedAmount()), Money.format(t.getBalanceAfter())));
        Ui.success("Applied " + applied.size() + " interest/fee transaction(s).");
    }

    private void printStatement() throws BankException {
        Account account = service.getAccount(readAccountNumber("Account number: "));
        Ui.header("Statement: " + account.getAccountNumber() + " (" + account.getType() + ")");
        System.out.printf("Owner : %s (%s)%n", service.getCustomerName(account.getCustomerId()),
                account.getCustomerId());
        System.out.printf("Opened: %s    Status: %s%n", account.getOpenedDate(), account.isOpen() ? "OPEN" : "CLOSED");
        System.out.println(account.getTermsDescription());
        System.out.println();
        System.out.printf("%-16s %-8s %-13s %14s %14s  %s%n", "Date", "Txn ID", "Type", "Amount", "Balance",
                "Description");
        Ui.line(88);
        if (account.getHistory().isEmpty()) {
            System.out.println("(no transactions)");
        }
        account.getHistory().forEach(t -> System.out.printf("%-16s %-8s %-13s %14s %14s  %s%n",
                t.getTimestamp().format(DATE_TIME), t.getId(), t.getType(),
                String.format("%+,.2f", t.getSignedAmount()), Money.format(t.getBalanceAfter()),
                t.getDescription()));
        Ui.line(88);
        System.out.printf("Current balance: PHP %s%n", Money.format(account.getBalance()));
    }

    private void showCustomerSummary() throws BankException {
        Customer customer = service.getCustomer(input.readNonEmpty("Customer ID: "));
        Ui.header("Customer Summary");
        System.out.printf("Customer: %s (%s)    Contact: %s%n", customer.getName(), customer.getId(),
                customer.getContactNumber());
        List<Account> sorted = new ArrayList<>(customer.getAccounts());
        sorted.sort(Comparator.comparingDouble(Account::getBalance).reversed());
        if (sorted.isEmpty()) {
            System.out.println("(no accounts)");
        } else {
            System.out.printf("%-8s %-13s %14s  %s%n", "Account", "Type", "Balance", "Status");
            Ui.line(48);
            sorted.forEach(a -> System.out.printf("%-8s %-13s %14s  %s%n", a.getAccountNumber(), a.getType(),
                    Money.format(a.getBalance()), a.isOpen() ? "OPEN" : "CLOSED"));
            Ui.line(48);
        }
        System.out.printf("Open accounts: %d    Total balance: PHP %s%n", customer.getOpenAccountCount(),
                Money.format(customer.getTotalBalance()));
    }

    private void listAccounts() {
        List<Account> all = service.listAccounts();
        if (all.isEmpty()) {
            Ui.info("There are no accounts yet.");
            return;
        }
        Ui.header("All Accounts");
        System.out.printf("%-8s %-13s %-22s %14s  %s%n", "Account", "Type", "Owner", "Balance", "Status");
        Ui.line(70);
        all.forEach(a -> System.out.printf("%-8s %-13s %-22.22s %14s  %s%n", a.getAccountNumber(), a.getType(),
                service.getCustomerName(a.getCustomerId()), Money.format(a.getBalance()),
                a.isOpen() ? "OPEN" : "CLOSED"));
    }

    private String readAccountNumber(String prompt) {
        return input.readNonEmpty(prompt).toUpperCase();
    }
}

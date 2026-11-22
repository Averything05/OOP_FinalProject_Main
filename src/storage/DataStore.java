package storage;

import exception.CorruptRecordException;
import exception.InvalidRequestException;
import model.Account;
import model.Customer;
import model.Transaction;
import model.TransactionType;
import pattern.AccountFactory;
import util.Ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Saves and loads the bank data as human-readable CSV files (customers.csv, accounts.csv,
 * transactions.csv) inside one folder. Lines that start with # are comments.
 * A missing file means "no records yet"; a corrupt line is skipped with a warning.
 * Files are written to a .tmp file first and then moved, so a crash cannot leave half a file.
 */
public class DataStore {
    private static final String CUSTOMERS_FILE = "customers.csv";
    private static final String ACCOUNTS_FILE = "accounts.csv";
    private static final String TRANSACTIONS_FILE = "transactions.csv";

    private final Path directory;

    public DataStore(String directoryName) {
        this.directory = Paths.get(directoryName);
    }

    // ----- saving -----

    /** Overwrites all three data files with the current data. */
    public void save(List<Customer> customers, List<Account> accounts) throws IOException {
        Files.createDirectories(directory);
        writeFile(CUSTOMERS_FILE, "#id,name,contact", customers,
                c -> c.getId() + "," + c.getName() + "," + c.getContactNumber());
        writeFile(ACCOUNTS_FILE, "#type,accountNumber,customerId,balance,openedDate,status,termMonths", accounts,
                a -> a.getType() + "," + a.getAccountNumber() + "," + a.getCustomerId() + ","
                        + String.format(Locale.US, "%.2f", a.getBalance()) + "," + a.getOpenedDate() + ","
                        + (a.isOpen() ? "OPEN" : "CLOSED") + "," + a.getTermMonths());
        List<Transaction> all = new ArrayList<>();
        accounts.forEach(account -> all.addAll(account.getHistory()));
        writeFile(TRANSACTIONS_FILE, "#id,accountNumber,type,amount,balanceAfter,timestamp,description", all,
                t -> t.getId() + "," + t.getAccountNumber() + "," + t.getType().name() + ","
                        + String.format(Locale.US, "%.2f", t.getAmount()) + ","
                        + String.format(Locale.US, "%.2f", t.getBalanceAfter()) + ","
                        + t.getTimestamp() + "," + t.getDescription().replace(',', ';'));
    }

    private <T> void writeFile(String fileName, String header, List<T> rows, Function<T, String> toLine)
            throws IOException {
        Path target = directory.resolve(fileName);
        Path temp = directory.resolve(fileName + ".tmp");
        try (BufferedWriter out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            out.write(header);
            out.newLine();
            for (T row : rows) {
                out.write(toLine.apply(row));
                out.newLine();
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    // ----- loading -----

    public List<Customer> loadCustomers() throws IOException {
        return readFile(CUSTOMERS_FILE, 3,
                f -> new Customer(f[0].trim(), f[1].trim(), f[2].trim()));
    }

    public List<Account> loadAccounts() throws IOException {
        return readFile(ACCOUNTS_FILE, 7, f -> {
            String status = f[5].trim().toUpperCase();
            if (!status.equals("OPEN") && !status.equals("CLOSED")) {
                throw new CorruptRecordException("status must be OPEN or CLOSED but was '" + f[5].trim() + "'");
            }
            return AccountFactory.restore(f[0].trim(), f[1].trim(), f[2].trim(),
                    Double.parseDouble(f[3].trim()), LocalDate.parse(f[4].trim()),
                    status.equals("OPEN"), Integer.parseInt(f[6].trim()));
        });
    }

    public List<Transaction> loadTransactions() throws IOException {
        return readFile(TRANSACTIONS_FILE, 7, f -> new Transaction(f[0].trim(), f[1].trim(),
                TransactionType.valueOf(f[2].trim().toUpperCase()), Double.parseDouble(f[3].trim()),
                Double.parseDouble(f[4].trim()), LocalDateTime.parse(f[5].trim()), f[6]));
    }

    /** Turns one line of a CSV file into one object. May throw if the line is bad. */
    private interface RecordParser<T> {
        T parse(String[] fields) throws CorruptRecordException;
    }

    private <T> List<T> readFile(String fileName, int expectedFields, RecordParser<T> parser)
            throws IOException {
        List<T> records = new ArrayList<>();
        Path file = directory.resolve(fileName);
        if (!Files.exists(file)) {
            Ui.info(fileName + " not found. Starting with no records.");
            return records;
        }
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    String[] fields = line.split(",", -1);
                    if (fields.length != expectedFields) {
                        throw new CorruptRecordException("expected " + expectedFields + " fields but found "
                                + fields.length);
                    }
                    records.add(parser.parse(fields));
                } catch (CorruptRecordException | InvalidRequestException e) {
                    Ui.warning("Skipped corrupt record in " + fileName + " line " + lineNumber + ": "
                            + e.getMessage());
                } catch (IllegalArgumentException | DateTimeException e) {
                    // bad number, unknown type name, or bad date/time text
                    Ui.warning("Skipped corrupt record in " + fileName + " line " + lineNumber
                            + ": invalid value (" + e.getMessage() + ")");
                }
            }
        }
        return records;
    }
}

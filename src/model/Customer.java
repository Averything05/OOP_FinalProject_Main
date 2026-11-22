package model;

import exception.InvalidRequestException;
import util.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** A bank customer. Composition: a Customer HAS accounts. Validation happens inside the class. */
public class Customer {
    private static final Pattern NAME_PATTERN = Pattern.compile("\\p{L}[\\p{L} .'-]{1,59}");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("[0-9+() -]{7,20}");

    private final String id;
    private final String name;
    private final String contactNumber;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String id, String name, String contactNumber) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidRequestException("Customer ID is required.");
        }
        if (name == null || !NAME_PATTERN.matcher(name.trim()).matches()) {
            throw new InvalidRequestException(
                    "Invalid name. Use 2 to 60 letters (spaces, . ' - allowed; no digits or commas).");
        }
        if (contactNumber == null || !CONTACT_PATTERN.matcher(contactNumber.trim()).matches()) {
            throw new InvalidRequestException(
                    "Invalid contact number. Use 7 to 20 characters: digits, +, -, ( ) or spaces.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.contactNumber = contactNumber.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    /** Links an account to this customer. The account must belong to this customer. */
    public void addAccount(Account account) {
        if (account == null || !id.equals(account.getCustomerId())) {
            throw new InvalidRequestException("That account does not belong to customer " + id + ".");
        }
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    /** @return a read-only view of the customer's accounts (open and closed) */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public int getOpenAccountCount() {
        return (int) accounts.stream().filter(Account::isOpen).count();
    }

    /** @return the sum of the balances of all open accounts */
    public double getTotalBalance() {
        return Money.round(accounts.stream().filter(Account::isOpen).mapToDouble(Account::getBalance).sum());
    }
}

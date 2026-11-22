package model;

import exception.InvalidRequestException;

import java.time.LocalDateTime;

/** One immutable line in an account's history: a deposit, withdrawal, transfer leg, interest or fee. */
public final class Transaction {
    private final String id;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(String id, String accountNumber, TransactionType type, double amount,
                       double balanceAfter, LocalDateTime timestamp, String description) {
        if (isBlank(id) || isBlank(accountNumber)) {
            throw new InvalidRequestException("Transaction ID and account number are required.");
        }
        if (type == null || timestamp == null) {
            throw new InvalidRequestException("Transaction type and time are required.");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new InvalidRequestException("Transaction amount must be greater than zero.");
        }
        if (Double.isNaN(balanceAfter) || Double.isInfinite(balanceAfter)) {
            throw new InvalidRequestException("Balance after the transaction is not a valid number.");
        }
        this.id = id.trim();
        this.accountNumber = accountNumber.trim();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.description = description == null ? "" : description.trim();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public String getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    /** @return the amount with a plus sign for money in and a minus sign for money out */
    public double getSignedAmount() {
        return type.isCredit() ? amount : -amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }
}

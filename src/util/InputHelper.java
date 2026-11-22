package util;

import exception.InputClosedException;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Reads and validates everything the user types. Every read method loops until the
 * input is valid, so text typed where a number is expected can never crash the program.
 */
public class InputHelper {
    private static final double MAX_AMOUNT = 1000000000.00;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Prints the prompt and returns the trimmed line. Throws InputClosedException at end of input. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine().trim();
    }

    /** Asks again until the user types something. */
    public String readNonEmpty(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            Ui.error("This field cannot be empty.");
        }
    }

    /** Asks again until the user types a whole number from min to max. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                Ui.error("Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                Ui.error("'" + text + "' is not a valid number. Enter a whole number from " + min + " to " + max + ".");
            }
        }
    }

    /** Asks again until the user types a positive amount with at most 2 decimals. Commas are allowed. */
    public double readAmount(String prompt) {
        while (true) {
            String text = readLine(prompt).replace(",", "");
            if (!AMOUNT_PATTERN.matcher(text).matches()) {
                Ui.error("Enter a positive amount with up to 2 decimal places, for example 1500 or 1500.50.");
                continue;
            }
            double value = Double.parseDouble(text);
            if (value <= 0) {
                Ui.error("Amount must be greater than zero.");
            } else if (value > MAX_AMOUNT) {
                Ui.error("Amount is too large. The maximum is PHP " + Money.format(MAX_AMOUNT) + ".");
            } else {
                return value;
            }
        }
    }

    /** Asks a yes/no question and returns true for yes. */
    public boolean confirm(String question) {
        while (true) {
            String text = readLine(question + " (y/n): ").toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            Ui.error("Please answer y or n.");
        }
    }
}

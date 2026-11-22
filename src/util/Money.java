package util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Small helpers so rounding and formatting of money are written once (DRY). */
public final class Money {

    private Money() {
    }

    /** Rounds to 2 decimal places (half up). */
    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Formats like 12,345.60 (no currency sign). */
    public static String format(double value) {
        return String.format("%,.2f", value);
    }

    /** One month of interest for an annual rate such as 0.025. Zero for empty or negative balances. */
    public static double monthlyInterest(double balance, double annualRate) {
        if (balance <= 0) {
            return 0;
        }
        return round(balance * annualRate / 12);
    }
}

package pattern;

/**
 * STRATEGY pattern: the monthly interest and fee rules of an account.
 * Each account type gets its own implementation, so Account never needs an
 * if-else chain on the account type. A new rule is one new class.
 */
public interface InterestFeeStrategy {

    /** @return the interest earned in one month for the given balance */
    double computeInterest(double balance);

    /** @return the fee charged in one month for the given balance */
    double computeFee(double balance);
}

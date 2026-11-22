package pattern;

import util.Money;

/**
 * STRATEGY implementations: the monthly interest and fee rules of each account type.
 * Each constant is one interchangeable strategy behind the InterestFeeStrategy interface,
 * so Account never needs an if-else chain on the account type.
 * To add a new rule, add one constant here (or any new class that implements InterestFeeStrategy)
 * and give it to the account in AccountFactory.
 */
public enum AccountRates implements InterestFeeStrategy {

    /** Savings rule: 2.5% a year, and a PHP 50 fee when the balance is below PHP 1,000. */
    SAVINGS {
        @Override
        public double computeInterest(double balance) {
            return Money.monthlyInterest(balance, SAVINGS_ANNUAL_RATE);
        }

        @Override
        public double computeFee(double balance) {
            return balance < SAVINGS_MINIMUM_BALANCE ? SAVINGS_LOW_BALANCE_FEE : 0;
        }
    },

    /** Checking rule: 0.25% a year, and a PHP 100 monthly fee that is waived at PHP 10,000 or more. */
    CHECKING {
        @Override
        public double computeInterest(double balance) {
            return Money.monthlyInterest(balance, CHECKING_ANNUAL_RATE);
        }

        @Override
        public double computeFee(double balance) {
            return balance >= CHECKING_FEE_WAIVER_BALANCE ? 0 : CHECKING_MONTHLY_FEE;
        }
    },

    /** Time deposit rule: the highest rate (5% a year) and no monthly fee. */
    TIME_DEPOSIT {
        @Override
        public double computeInterest(double balance) {
            return Money.monthlyInterest(balance, TIME_DEPOSIT_ANNUAL_RATE);
        }

        @Override
        public double computeFee(double balance) {
            return 0;
        }
    };

    private static final double SAVINGS_ANNUAL_RATE = 0.025;
    private static final double SAVINGS_MINIMUM_BALANCE = 1000.00;
    private static final double SAVINGS_LOW_BALANCE_FEE = 50.00;
    private static final double CHECKING_ANNUAL_RATE = 0.0025;
    private static final double CHECKING_FEE_WAIVER_BALANCE = 10000.00;
    private static final double CHECKING_MONTHLY_FEE = 100.00;
    private static final double TIME_DEPOSIT_ANNUAL_RATE = 0.05;
}

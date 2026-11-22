# DCIT 50 Final Project - Group 6: Bank Account Management System

A menu-driven, terminal-only Java application (no GUI, plain `javac` and `java`, Java 8 or newer).

## How to run

Run these from this folder (the program reads and writes the `data/` folder next to it).

| System | Command |
|---|---|
| Windows | `run.bat` |
| Linux / macOS | `sh run.sh` |
| Manual | `javac -d out src/app/*.java src/model/*.java src/service/*.java src/pattern/*.java src/storage/*.java src/exception/*.java src/util/*.java` then `java -cp out app.Main` |

Sample data is already in `data/` (3 customers, 5 accounts). Delete the files in `data/` to start empty.
Real terminal sessions are in `docs/sample_output/`. `docs/STUDY_GUIDE.md` has the menu-to-file traces, defense Q&A and bug-hunt practice.

## Menu

1. Register customer  2. Open account  3. Close account  4. Deposit  5. Withdraw  6. Transfer
7. Apply monthly interest and fees  8. Print account statement  9. Customer summary  10. List all accounts  11. Save and exit

Account types: `SAVINGS` (2.5% a year, PHP 50 fee under PHP 1,000), `CHECKING` (overdraft up to PHP 2,000, PHP 100 monthly fee waived at PHP 10,000), `TIME_DEPOSIT` (5% a year, no withdrawal or closing before maturity).

## Packages and classes

| Package | Classes | Job |
|---|---|---|
| `app` | `Main`, `MenuController` | Start the program; menu loop, input, output |
| `model` | `Account` (abstract), `SavingsAccount`, `CheckingAccount`, `TimeDepositAccount`, `Customer`, `Transaction`, `TransactionType` | Domain objects and their own validation |
| `service` | `BankService` | Business rules; Observer subject |
| `pattern` | `AccountFactory`, `InterestFeeStrategy` + `AccountRates` (enum: 3 strategies), `AccountObserver` (interface with 2 lambda observers), `BankConfig` | Design patterns |
| `storage` | `DataStore` | CSV save and load |
| `exception` | 7 custom exceptions (base class `BankException` + 6) | See table below |
| `util` | `Repository<T>`, `InputHelper`, `Ui`, `Money` | Generic store, input validation, messages, rounding |

## Where each requirement is met

| # | Requirement | Where |
|---|---|---|
| R1 | 8+ classes, inheritance and composition | `Account` is the parent of 3 account types (is-a); `Customer` has accounts and `Account` has transactions (has-a) |
| R2 | Encapsulation | All fields private; the balance changes only through `deposit`, `withdraw`, `creditInterest`, `chargeFee`, `close`; constructors reject bad values |
| R3 | Abstraction + polymorphism | `Account` is abstract; `Repository<Account>` and `Customer.getAccounts()` hold all three subtypes; hooks `getMinimumAllowedBalance`, `checkWithdrawalAllowed`, `checkDepositAllowed`, `getTermsDescription` are overridden per type |
| R4 | 3+ patterns | Singleton, Factory, Strategy, Observer (table below) |
| R5 | Custom exceptions thrown and caught | Table below |
| R6 | Persistence | CSV files in `data/` |
| R7 | Collections, generic class, lambda | `ArrayList` + `LinkedHashMap`; `Repository<T>`; `Comparator` sort, `forEach`, `Predicate` filter, method references, menu actions |
| R8 | UML | `docs/uml_class_diagram.png/.pdf` (classes, patterns, composition) and `docs/uml_exceptions.png/.pdf` (exception hierarchy); editable sources are the `.mmd` files |
| R10 | Console only | No GUI imports; uses `printf` for aligned tables |

## Design patterns

| Pattern | Class(es) | Problem it solves | Adding a variant |
|---|---|---|---|
| Strategy | `InterestFeeStrategy`, implemented by the three constants of the `AccountRates` enum (`SAVINGS`, `CHECKING`, `TIME_DEPOSIT`) | Interest and fee rules differ by account type; without Strategy `Account` would need an if-else on the type | One new enum constant (or one new class that implements the interface) |
| Factory | `AccountFactory` | The menu and the file loader must create the right subclass without naming it | New case in the factory + new account/strategy classes |
| Observer | `AccountObserver` with its `lowBalance()` and `largeTransaction()` lambdas; subject is `BankService` | Alerts must happen after every transaction without `BankService` knowing about them | One new static method in `AccountObserver` and one `addObserver` line in `Main` |
| Singleton | `BankConfig` | One shared set of limits; two copies could make alerts and accounts disagree | Change one constant |

## Exceptions

| Exception | Kind | Thrown in | Caught in |
|---|---|---|---|
| `InsufficientFundsException` (has `shortfall`) | checked | `Account.withdraw` | `MenuController.execute` |
| `RecordNotFoundException` (has `recordType`, `recordId`) | checked | `BankService.getAccount`, `getCustomer` | `MenuController.execute` |
| `TermNotMaturedException` (has `maturityDate`) | checked | `TimeDepositAccount.checkWithdrawalAllowed` | `MenuController.execute` |
| `InvalidRequestException` | unchecked | model classes, `AccountFactory`, `Repository` | `MenuController.execute`, `DataStore.readFile`, `BankService.load` |
| `CorruptRecordException` | checked | `DataStore` parsers | `DataStore.readFile` |
| `InputClosedException` | unchecked | `InputHelper.readLine` | `MenuController.run` |

Checked = a normal problem the user can recover from, so the compiler forces it to be handled. Unchecked = a rejected value or state that would otherwise clutter every signature.

## Persistence (CSV)

Chosen because the files are human-readable (easy to show and to debug) and need no `serialVersionUID` or class-compatibility care. Three files in `data/`: `customers.csv`, `accounts.csv`, `transactions.csv`. Lines starting with `#` are comments.

- Saved after every successful change and again on exit; files are written to `.tmp` first and then moved.
- Missing file: starts with empty data and prints an info message.
- Corrupt line (wrong field count, bad number or date, unknown type, invalid name): skipped with a warning that names the file and line. Records pointing to a missing customer or account are also skipped.

## Known limitations

- Money uses `double` rounded to 2 decimals (simple to explain); `BigDecimal` would be more exact.
- Skipped corrupt lines are not kept: the next save rewrites the files without them.
- Time deposits compare against the computer's current date.
- Interest and fees are applied manually (menu option 7), not automatically each month.
- Not included: the Word/PDF documentation (Section 7 template) and its screenshots. Screenshots must be cropped code and terminal output from your own machine so they match your final code. If you change any class, update the UML to match.

# Study Guide: Group 6 Bank Account Management System

Para sa inyong dalawa. Dahil 2 lang kayo sa group, **parehong kayo ang mabubunot** sa random debugging (5 minuto bawat isa), at ang resulta ang grade ng buong group. Kaya parehong dapat makapag-trace at makapag-ayos ng bug sa kahit anong bahagi.

Mga numero ng linya ay iniiwasan dito; hanapin ang method sa file (`Ctrl+F` o `Ctrl+T` sa VS Code), dahil nagbabago ang linya kapag may na-edit.

---

## 1. Ang system sa isang tingin

```
Main  ──►  MenuController  ──►  BankService  ──►  Account (Savings / Checking / TimeDeposit)
(start)    (menu, input,        (business rules,   Customer, Transaction
            output)              Observer subject)
                  │                   │
              InputHelper,            ├──►  Repository<T>   (lahat ng customers at accounts, hinahanap by ID)
              Ui, Money               ├──►  DataStore       (CSV save/load sa data/)
                                      ├──►  AccountFactory  (gumagawa ng tamang Account subclass)
                                      └──►  AccountObserver (Observer: low balance, large transaction; mga lambda)
Account ──► InterestFeeStrategy (mga constant ng enum AccountRates: SAVINGS / CHECKING / TIME_DEPOSIT)   BankConfig = Singleton (mga limit)
```

| Layer | Package | Trabaho | Hindi ginagawa |
|---|---|---|---|
| UI | `app` | Menu loop, tanong, print | Walang business rule |
| Logic | `service` | Mga patakaran ng bangko, ID numbering, save/load | Walang `System.out` ng menu |
| Domain | `model` | Account, Customer, Transaction; sila mismo ang nagva-validate | Walang file o menu |
| Patterns | `pattern` | Factory, Strategy, Observer, Singleton | |
| File | `storage` | CSV read/write | Walang business rule |
| Tools | `util` | Generic `Repository<T>`, `InputHelper`, `Ui`, `Money` | |

---

## 2. Trace ng bawat menu option (menu → file)

Lahat ng nasa `MenuController` ay dumadaan sa `execute()`: tinatawag ang action, at kung walang error at `savesData` ang option, `saveData()` ang susunod (`BankService.save()` → `DataStore.save()`). Ang mga error na `BankException` o `InvalidRequestException` ay nahuhuli doon at naipi-print gamit ang `e.getMessage()`.

| # | Option | `MenuController` | `BankService` | Model / pattern | Pwedeng mag-error | Sine-save? | Alerts |
|---|---|---|---|---|---|---|---|
| 1 | Register customer | `registerCustomer` | `registerCustomer` (gumagawa ng `C-001`...) | `new Customer(...)` (regex sa name at contact) → `Repository.add` | `InvalidRequestException` | Oo | |
| 2 | Open account | `openAccount` | `getCustomer` → `openAccount` → `record(DEPOSIT)` | `AccountFactory.openNew` (minimum deposit, `BankConfig`) → `restore` → bagong Savings/Checking/TimeDeposit + Strategy | `RecordNotFoundException`, `InvalidRequestException` | Oo | Large (≥ 50,000) |
| 3 | Close account | `closeAccount` (may `confirm`) | `closeAccount` → `record(WITHDRAWAL)` | `Account.close()` | `RecordNotFoundException`, `TermNotMaturedException`, `InvalidRequestException` (overdrawn) | Oo | |
| 4 | Deposit | `deposit` (`readAmount`) | `deposit` → `record(DEPOSIT)` | `Account.deposit()` → `ensureCanDeposit()` → `checkDepositAllowed()` (Time Deposit ay laging tumatanggi) | `RecordNotFoundException`, `InvalidRequestException` | Oo | Large |
| 5 | Withdraw | `withdraw` | `withdraw` → `record(WITHDRAWAL)` | `Account.withdraw()` → `checkWithdrawalAllowed()` (Time Deposit) → `available = balance - getMinimumAllowedBalance()` → `amount > available`? | `InsufficientFundsException`, `TermNotMaturedException`, `RecordNotFoundException` | Oo | Low (< 500) at Large |
| 6 | Transfer | `transfer` | `transfer`: (1) pareho ang account? (2) `to.ensureCanDeposit()` (3) `from.withdraw` (4) `to.deposit` (5) dalawang `record` | | pareho sa 4 at 5 | Oo | Low sa source, Large |
| 7 | Monthly interest at fees | `applyMonthlyCharges` | `applyMonthlyCharges`: para sa bawat bukas na account, interest at fee mula sa **parehong lumang balance** | `computeMonthlyInterest` at `computeMonthlyFee` (Strategy) → `creditInterest`, `chargeFee` (hindi bababa sa minimum) | | Oo | Low |
| 8 | Statement | `printStatement` | `getAccount` | `getHistory()` | `RecordNotFoundException` | Hindi | |
| 9 | Customer summary | `showCustomerSummary` (`Comparator` lambda, pinakamalaking balance muna) | `getCustomer` | `Customer.getTotalBalance` | `RecordNotFoundException` | Hindi | |
| 10 | List accounts | `listAccounts` (`forEach`) | `listAccounts` | | | Hindi | |
| 11 | Save and exit | tapos ng loop sa `run()` → `saveData()` | | | | Oo | |

**Pagsisimula:** `Main.main` → `new BankService(new DataStore("data"))` → `addObserver(AccountObserver.lowBalance())` at `addObserver(AccountObserver.largeTransaction())` → `MenuController.run()` → `BankService.load()` → `DataStore.loadCustomers/Accounts/Transactions` → `refreshCounters()` (para ang susunod na ID ay magpatuloy: `A-1006`, `C-004`, `T-00011`).

**Pag-save:** `DataStore.save` → `writeFile` ay sumusulat muna sa `.tmp` file, tapos `Files.move(..., REPLACE_EXISTING)`. Kaya hindi mag-iiwan ng kalahating file kapag nag-crash.

**Pag-load:** `readFile`: kung wala ang file → `[INFO]` at walang laman na list. Linyang nagsisimula sa `#` o blangko → laktawan. Mali ang bilang ng field, masamang numero o petsa, hindi kilalang type → `[WARNING] Skipped corrupt record ... line N`. Account na walang customer → `[WARNING] Skipped account ...`.

---

## 3. Mga patakaran at numero (para hindi mag-isip sa demo)

| Bagay | Patakaran | Nasaan |
|---|---|---|
| Savings | 2.5% kada taon (buwanang interes = balance × rate ÷ 12); fee na 50 kapag balance < 1,000 | `AccountRates.SAVINGS` |
| Checking | 0.25% kada taon; fee na 100 maliban kung balance ≥ 10,000; puwedeng mag-overdraft hanggang 2,000 | `AccountRates.CHECKING`, `CheckingAccount.getMinimumAllowedBalance`, `BankConfig` |
| Time deposit | 5% kada taon, walang fee; walang dagdag na deposit; walang withdraw o close bago ang maturity date | `AccountRates.TIME_DEPOSIT`, `TimeDepositAccount` |
| Minimum na unang deposit | Savings 500, Checking 1,000, Time deposit 5,000 | `BankConfig` |
| Low-balance alert | kapag debit at balance < 500 | `AccountObserver.lowBalance` |
| Large-transaction alert | kapag halaga ≥ 50,000 | `AccountObserver.largeTransaction` |
| Sample data | `A-1003` ay mature na (2026-06-01); `A-1005` ay mature sa 2027-09-01; `A-1004` ay 450.00 (mababa sa 500) | `data/accounts.csv` |

---

## 4. Hanggang saan ang 5-minutong paliwanag (outline)

1. **Ano ang system:** terminal na bank app; customers, tatlong uri ng account, deposit, withdraw, transfer, buwanang interes at fee, statement, summary, CSV save at load.
2. **Class design:** `Account` ay abstract; tatlong subclass (is-a); `Customer` may mga account at `Account` may mga transaction (has-a). Ang validation ay nasa loob ng class (hindi puwedeng maging negative ang halaga, hindi puwedeng mag-withdraw ang saradong account).
3. **Patterns (bakit):** Strategy, Factory, Observer, Singleton (tingnan ang seksyon 6).
4. **Persistence:** CSV sa `data/`; nawawalang file ay walang crash; sirang linya ay lalaktawan na may babala.
5. **Exceptions:** 7 custom; checked para sa mga bagay na maaayos ng user, unchecked para sa maling halaga o estado.

---

## 5. Saan hinuhuli ang bawat exception

| Exception | Uri | Itinatapon sa | Nahuhuli sa |
|---|---|---|---|
| `InsufficientFundsException` (may `shortfall`) | checked | `Account.withdraw` | `MenuController.execute` |
| `TermNotMaturedException` (may `maturityDate`) | checked | `TimeDepositAccount.checkWithdrawalAllowed` (tinatawag ng `withdraw` at `close`) | `MenuController.execute` |
| `RecordNotFoundException` (may `recordType`, `recordId`) | checked | `BankService.getAccount` at `getCustomer` | `MenuController.execute` |
| `InvalidRequestException` | unchecked | `Account` (halaga, saradong account), `TimeDepositAccount.checkDepositAllowed`, `Customer`, `AccountFactory.openNew`, `Repository.add` | `MenuController.execute`; `DataStore.readFile` at `BankService.load` kapag nagloload |
| `CorruptRecordException` | checked | `DataStore` (maling bilang ng field, maling status) | `DataStore.readFile` |
| `InputClosedException` | unchecked | `InputHelper.readLine` (wala nang input) | `MenuController.run` |

`BankException` ay base class ng lahat ng checked business error, kaya isang `catch` lang sa `execute()`.

---

## 6. Mga tanong na malamang itanong at ang maikling sagot

**"Bakit Strategy at hindi simpleng if-else?"**
Magkaiba ang interes at fee ng Savings, Checking at Time Deposit. Kung if-else sa `Account`, kailangan itong baguhin sa bawat bagong uri. Ngayon, may `InterestFeeStrategy` at isang bagong constant lang sa `AccountRates` (o isang bagong klase na nag-i-implement ng interface) ang kailangan. `Account` ay may interface lang (`strategy`), hindi ang konkretong klase.

**"Bakit Factory?"**
Para hindi alam ng menu at ng file loader ang `SavingsAccount`, `CheckingAccount` o `TimeDepositAccount`. `AccountFactory.openNew` ay gumagawa ng bagong account at `restore` ay ginagamit din ng `DataStore` sa pag-load.

**"Bakit Observer?"**
Ang alert ay dapat mangyari pagkatapos ng bawat transaction, pero hindi dapat alam ng `BankService` kung anong mga alert meron. Ang `BankService` ay may listahan ng `AccountObserver` lang. Dagdag na alert = isang static method sa `AccountObserver` at isang `addObserver` sa `Main`.

**"Bakit Singleton ang `BankConfig`?"**
Dalawang kopya ng limits ay totoong problema: puwedeng magkaiba ang limit ng alert at ng account. Kaya iisa lang ang instance (`getInstance()`, private ang constructor).

**"Anong babaguhin mo para magdagdag ng bagong uri ng account (halimbawa Student)?"**
1. `model/StudentAccount extends Account` (`getType`, `getTermsDescription`).
2. `pattern/AccountRates`: bagong constant na `STUDENT` na may sariling `computeInterest` at `computeFee`.
3. `AccountFactory`: bagong constant, idagdag sa `supportedTypes()`, at bagong `case` sa `restore()`.
4. `BankConfig`: minimum na unang deposit.
Walang kailangang baguhin sa `MenuController`, `BankService` o `DataStore`. Ang listahan ng uri sa menu ay galing sa `supportedTypes()`.

**"Anong babaguhin para magdagdag ng bagong menu option?"**
Gumawa ng method sa `MenuController` at magdagdag ng isang `addItem("...", this::method, true/false)` na linya sa constructor. Ang numero ng Exit ay awtomatikong nag-a-adjust.

**"Anong nangyayari kapag nawawala o sira ang data file?"**
Nawawala: `[INFO] ... not found. Starting with no records.` Sira: `[WARNING] Skipped corrupt record in <file> line N: ...` at nagpapatuloy ang program (`DataStore.readFile`). Hindi nagka-crash.

**"Bakit checked at unchecked?"**
Checked ang mga problemang puwedeng ayusin ng user at gusto nating pilitin ng compiler na i-handle (kulang na pera, hindi mature, ID na wala). Unchecked ang maling halaga o estado na ipapasa na lang pataas para hindi punuin ang bawat signature ng `throws`.

**"Bakit CSV?"**
Nababasa ng tao, madaling ipakita at i-debug, at walang `serialVersionUID` o ClassNotFound na isyu. Kapalit: manual na parsing at kailangang hawakan ang sirang linya, na ginagawa ng `DataStore`.

**"Nasaan ang polymorphism, abstraction, encapsulation, inheritance, composition?"**
- **Abstraction:** `Account` (abstract), `InterestFeeStrategy` at `AccountObserver` (interface).
- **Inheritance:** `SavingsAccount`, `CheckingAccount` at `TimeDepositAccount` extends `Account` (true is-a).
- **Polymorphism:** `Repository<Account>` at `Customer.getAccounts()` ay may iba't ibang subtype; tumatawag ng `account.withdraw()` at ang hook (`getMinimumAllowedBalance`, `checkWithdrawalAllowed`) ang nag-iiba ayon sa tunay na uri.
- **Encapsulation:** private ang lahat ng field; ang balance ay nagbabago lang sa pamamagitan ng `deposit`, `withdraw`, `creditInterest`, `chargeFee`, `close`.
- **Composition:** `Customer` has accounts; `Account` has transactions at strategy.

**"Nasaan ang generic class, lambda at collections?"**
Generic: `Repository<T>`. Lambda: `AccountObserver.lowBalance` at `largeTransaction` (dalawang observer), `Comparator` sa customer summary, `forEach` sa statement at list, `orElseThrow(() -> ...)`, mga menu action (`this::registerCustomer`). Collections: `ArrayList` (history, accounts ng customer), `LinkedHashMap` (loob ng `Repository`, hinahanap by ID habang nananatili ang pagkakasunod), `HashMap` (minimum deposits sa `BankConfig`).

---

## 7. Bug-hunt na pagsasanay (lahat ay sinubukan na ang sintomas)

**Paano gamitin:** Si A ay maglalagay ng **isang** bug sa isang kopya ng project ni B (o kabaligtaran) at ibibigay lang ang sintomas. Si B ay may 5 minuto para i-reproduce, hanapin ang sanhi, ipaliwanag, ayusin, at patakbuhing muli. Palitan ang gumagawa ng bug. Mga sample data ang gamit ng lahat ng test dito (burahin ang `data/` na binago at ibalik mula sa orihinal bago ang susunod).

| ID | File / method | Binago | Sintomas (ito ang makikita) | Paano hanapin | Ayos |
|---|---|---|---|---|---|
| B1 | `model/Account.java`, `withdraw` | `amount > available` → `amount >= available` | Option 5 sa `A-1004`, 450 → `[ERROR] Insufficient funds in A-1004. Short by PHP 0.00.` (hindi puwedeng i-withdraw ang buong balance) | Ang "Short by 0.00" ay hindi makatwiran, kaya kondisyon ng paghahambing | ibalik sa `>` |
| B2 | `service/BankService.java`, `refreshCounters` | tinanggal ang `+ 1` sa `nextAccountNo` | Pagkatapos mag-restart, option 2 → `[ERROR] Duplicate ID: A-1005` | Duplicate ID galing sa `Repository.add`; tingnan kung saan binibigyan ng numero pagka-load | ibalik ang `+ 1` |
| B3 | `service/BankService.java`, `transfer` | tinanggal ang `to.ensureCanDeposit();` | Option 6 mula `A-1001` papuntang `A-1005`, 100 → may `[ERROR] ... does not accept additional deposits` PERO bumaba ang `A-1001` sa 14,900.00 (nawalang pera) | Ihambing ang balance bago at pagkatapos; tingnan ang pagkakasunod ng check at pagbabago sa `transfer` | ibalik ang check bago ang `from.withdraw` |
| B4 | `util/InputHelper.java`, `readInt` | `value <= max` → `value < max` | Hindi matanggap ang `11` (Exit): `[ERROR] Please enter a number from 1 to 11.` | Ang pinakamataas na numero lang ang tinatanggihan → boundary | ibalik ang `<=` |
| B5 | `storage/DataStore.java`, `readFile` | na-disable ang `fields.length != expectedFields` na check | Kung may linyang kulang ang field sa `accounts.csv`, **nagka-crash** ang startup: `ArrayIndexOutOfBoundsException` | Basahin ang stack trace: pinakamataas na linya ng sarili nating code (`loadAccounts` → `readFile`) | ibalik ang check na nagtatapon ng `CorruptRecordException` |
| B6 | `pattern/AccountRates.java`, `SAVINGS` → `computeFee` | `<` → `>` | Option 7: ang `A-1001` (15,000) ay may `FEE -50.00`, at ang `A-1004` (450) ay wala | Fee sa mayaman at wala sa mahirap ay baliktad na kondisyon | ibalik ang `<` |
| B7 | `util/Money.java`, `monthlyInterest` | tinanggal ang `/ 12` | Option 7: `A-1001` ay `+375.00` sa halip na `+31.25` (12 beses) | Ihambing sa 15,000 × 2.5% ÷ 12 = 31.25 | ibalik ang `/ 12` |
| B8 | `pattern/AccountObserver.java`, `lowBalance` | `<` → `>` | Option 5 sa `A-1001`, 100 → `[ALERT] Low balance: A-1001 now has PHP 14,900.00` (mali) | Alert ay lumalabas sa mataas na balance | ibalik ang `<` |
| B9 | `model/CheckingAccount.java`, `getMinimumAllowedBalance` | tinanggal ang minus (`-limit` → `limit`) | Option 5 sa `A-1002`, 9,000 → `[ERROR] Insufficient funds ... Short by PHP 2,800.00` (dapat tinanggap dahil may overdraft na 2,000) | Overdraft na hindi gumagana; sign ng minimum | `return -BankConfig...getOverdraftLimit();` |
| B10 | `storage/DataStore.java`, `save` | binaliktad ang `"OPEN" : "CLOSED"` | Isara ang `A-1004`, lumabas, buksan ulit → option 10: lahat ay `CLOSED` at ang sinara ay `OPEN` | Tama bago mag-restart pero mali pagkatapos → file save o load | ibalik ang pagkakasunod |
| B11 | `model/Transaction.java`, `getSignedAmount` | binaliktad ang `-amount` | Option 8: ang `DEPOSIT` ay `-10,000.00` at ang `TRANSFER_OUT` ay `+2,000.00`, pero tama ang balance | Balance tama pero tanda ng halaga mali → statement lang ang apektado | ibalik ang `isCredit() ? amount : -amount` |

**Paraan para sa 5 minuto (sundin ito sa totoong demo):**
1. **I-reproduce:** patakbuhin at gawin ang eksaktong hakbang na sinabi. Basahin ang `[ERROR]` o stack trace.
2. **Hanapin sa trace table** (seksyon 2) kung anong method ang dinadaanan ng option na iyon.
3. **Basahin ang kondisyon at numero:** `>` o `>=`, `<` o `<=`, sign (`+` o `-`), `+ 1`, pagkakasunod ng mga hakbang, `OPEN` o `CLOSED`.
4. **Ipaliwanag sa salita bago mag-ayos:** "Ang sanhi ay ___ kasi ___."
5. **I-ayos, i-compile ulit, patakbuhin ulit** at ipakita ang tamang output.

**Iba pang karaniwang bug ng instructor (pag-isipan din):** off-by-one sa loop, kulang na validation, maling file mode (`APPEND` sa halip na overwrite), exception na hindi nahuhuli, maling method ang tinatawag.

---

## 8. Checklist bago ang demo

- [ ] Parehong makapag-compile at makapag-run mula sa malinis na folder (`run.bat`).
- [ ] Parehong nakapag-trace ng bawat option sa seksyon 2 nang hindi tumitingin.
- [ ] Parehong nakapag-solve ng hindi bababa sa 5 sa mga bug sa seksyon 7.
- [ ] Alam kung saan nasa `data/` ang sample data at kung paano ito ibalik.
- [ ] Alam sagutin ang lahat ng tanong sa seksyon 6.
- [ ] Parehong alam gamitin ang debugger o print statements ng VS Code.
- [ ] Tugma ang UML sa code (kung may binago, i-update).

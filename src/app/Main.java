package app;

import pattern.AccountObserver;
import pattern.BankConfig;
import service.BankService;
import storage.DataStore;
import util.InputHelper;

import java.util.Locale;
import java.util.Scanner;

/** Starts the Bank Account Management System. All real work happens in other classes. */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        BankService service = new BankService(new DataStore(BankConfig.getInstance().getDataDirectory()));
        service.addObserver(AccountObserver.lowBalance());
        service.addObserver(AccountObserver.largeTransaction());
        try (Scanner scanner = new Scanner(System.in)) {
            new MenuController(service, new InputHelper(scanner)).run();
        }
    }
}

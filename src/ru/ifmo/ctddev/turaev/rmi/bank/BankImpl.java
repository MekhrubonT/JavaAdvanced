package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.util.*;
import java.rmi.server.*;
import java.rmi.*;

public class BankImpl implements Bank {
    private final Map<Person, HashMap<String, Account>> accounts = new HashMap<>();
    private final int port;

    public BankImpl(final int port) {
        this.port = port;
    }

    public Account createAccount(Person person, String accountNumber) throws RemoteException {
        Account account = new AccountImpl(person, accountNumber);
        accounts.put(person, new HashMap<>());
        accounts.get(person).put(accountNumber, account);
        UnicastRemoteObject.exportObject(account, port);
        return account;
    }


    public Account getAccount(Person person, String serialNumber) {
        return accounts.get(person).get(serialNumber);
    }
}

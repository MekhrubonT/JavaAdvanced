package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.bank.Account;
import ru.ifmo.ctddev.turaev.rmi.bank.AccountImpl;
import ru.ifmo.ctddev.turaev.rmi.bank.Bank;
import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.util.*;
import java.rmi.server.*;
import java.rmi.*;

public class BankImpl implements Bank {
    private final Map<String, Account> accounts = new HashMap<>();
    private final int port;

    public BankImpl(final int port) {
        this.port = port;
    }

    // Создает счет
    public Account createAccount(Person id, String serialNumber) throws RemoteException {
        System.out.println(id.getId());
        Account account = new AccountImpl(id, serialNumber);
        accounts.putIfAbsent(id.getId(), account);
        UnicastRemoteObject.exportObject(account, port);
        return account;
    }

    // Возвращает счет
    public Account getAccount(Person id, String serialNumber) throws RemoteException {
        System.out.println("Get account " + id.hashCode() + " " + id.toString());
        return accounts.get(id.getId());
    }
}

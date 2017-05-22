package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.util.*;
import java.rmi.server.*;
import java.rmi.*;

public class BankImpl implements Bank {
    private final Map<String, Map<String, Account>> accounts = new HashMap<>();
    private final int port;

    public BankImpl(final int port) {
        this.port = port;
    }

    public Account createAccount(Person id, String serialNumber) throws RemoteException {
        Account account = new AccountImpl(id, serialNumber);
        accounts.putIfAbsent(id.getString(), new HashMap<>());
        accounts.get(id.getString()).putIfAbsent(serialNumber, account);
        UnicastRemoteObject.exportObject(account, port);
        return account;
    }

    public Account getAccount(Person id, String serialNumber) throws RemoteException {
        Map<String, Account> stringAccountMap = accounts.get(id.getString());
        return stringAccountMap == null ? null : stringAccountMap.get(serialNumber);
    }
}

package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.rmi.*;

public interface Bank extends Remote {
    // Создает счет
    public Account createAccount(Person id, String serialNumber)
            throws RemoteException;
    // Возвращает счет
    public Account getAccount(Person id, String serialNumber)
            throws RemoteException;
}

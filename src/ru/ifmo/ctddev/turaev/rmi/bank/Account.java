package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.rmi.*;

public interface Account extends Remote {
    public Person getPerson()
        throws RemoteException;

    public int getAmount()
        throws RemoteException;

    public void setAmount(int amount)
        throws RemoteException;
}
package ru.ifmo.ctddev.turaev.rmi.bank;

import java.rmi.*;

public interface Account extends Remote {
    public String getPerson()
        throws RemoteException;

    public int getAmount()
        throws RemoteException;

    public void setAmount(int amount)
        throws RemoteException;
}
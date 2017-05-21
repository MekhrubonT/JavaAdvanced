package ru.ifmo.ctddev.turaev.rmi.person;

import java.rmi.RemoteException;

/**
 * Created by mekhrubon on 10.05.2017.
 */
public interface Person {
    public String getSurname() throws RemoteException;

    public String getId() throws RemoteException;

    public String getName() throws RemoteException;
}

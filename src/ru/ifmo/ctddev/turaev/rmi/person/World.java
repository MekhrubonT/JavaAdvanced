package ru.ifmo.ctddev.turaev.rmi.person;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by mekhrubon on 22.05.2017.
 */
public interface World extends Remote {
    public void tryNewPerson(String name, String surname, String id) throws RemoteException;
    public RemotePerson getRemotePerson(String id) throws RemoteException;
    public LocalPersonImpl getLocalPerson(String id) throws RemoteException;

}

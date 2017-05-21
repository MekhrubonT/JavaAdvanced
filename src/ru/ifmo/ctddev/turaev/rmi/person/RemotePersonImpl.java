package ru.ifmo.ctddev.turaev.rmi.person;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by mekhrubon on 10.05.2017.
 */
public class RemotePersonImpl extends UnicastRemoteObject implements RemotePerson {
    private final String name;
    private final String surname;
    private final String id;
    public RemotePersonImpl(String name, String surname, String id) throws RemoteException {
        this.name = name;
        this.surname = surname;
        this.id = id;
    }

    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }
}

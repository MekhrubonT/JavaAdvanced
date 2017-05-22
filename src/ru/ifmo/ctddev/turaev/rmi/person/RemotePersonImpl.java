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
    private final String stringValue;

    public RemotePersonImpl(String name, String surname, String id) throws RemoteException {
        this.name = name;
        this.surname = surname;
        this.id = id;
        stringValue = name + surname + id;
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

    @Override
    public String toString() {
        return stringValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LocalPersonImpl))
            return false;
        return hashCode() == obj.hashCode() && toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }
}
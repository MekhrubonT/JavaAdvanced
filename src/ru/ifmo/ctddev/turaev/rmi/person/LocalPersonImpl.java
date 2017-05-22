package ru.ifmo.ctddev.turaev.rmi.person;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by mekhrubon on 10.05.2017.
 */
public class LocalPersonImpl implements LocalPerson {
    private final String name;
    private final String surname;
    private final String id;
    private final String stringValue;

    public LocalPersonImpl(String name, String surname, String id) {
        this.name = name;
        this.surname = surname;
        this.id = id;
        stringValue = name + surname + id;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
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
    public String getString() throws RemoteException {
        return stringValue;
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }
}

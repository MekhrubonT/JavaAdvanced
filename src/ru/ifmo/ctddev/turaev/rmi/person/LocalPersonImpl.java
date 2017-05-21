package ru.ifmo.ctddev.turaev.rmi.person;

import java.io.Serializable;
import java.rmi.Remote;

/**
 * Created by mekhrubon on 10.05.2017.
 */
public class LocalPersonImpl implements LocalPerson {
    private final String name;
    private final String surname;
    private final String id;

    public LocalPersonImpl(String name, String surname, String id) {
        this.name = name;
        this.surname = surname;
        this.id = id;
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
}

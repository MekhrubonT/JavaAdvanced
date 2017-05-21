package ru.ifmo.ctddev.turaev.rmi.person;

import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mekhrubon on 22.05.2017.
 */
public class WorldImpl implements World {
    private Map<String, Pair<String, String>> population = new HashMap<>();
    int country;

    public WorldImpl (int country) {
        this.country = country;
    }

    public void tryNewPerson(String name, String surname, String id) {
        population.putIfAbsent(id, new Pair<>(name, surname));
    }
    public RemotePerson getRemotePerson(String id) throws RemoteException {
        Pair<String, String> personData = population.get(id);
        return new RemotePersonImpl(personData.getKey(), personData.getValue(), id);
    }
    public LocalPersonImpl getLocalPerson(String id) {
        Pair<String, String> personData = population.get(id);
        return new LocalPersonImpl(personData.getKey(), personData.getValue(), id);
    }
}

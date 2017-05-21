package ru.ifmo.ctddev.turaev.rmi;

import ru.ifmo.ctddev.turaev.rmi.bank.Bank;
import ru.ifmo.ctddev.turaev.rmi.bank.BankImpl;
import ru.ifmo.ctddev.turaev.rmi.person.World;
import ru.ifmo.ctddev.turaev.rmi.person.WorldImpl;

import java.rmi.*;
import java.rmi.server.*;
import java.net.*;

public class Server {

    private final static int PORT = 8888;
    public static final String MEKHRUBON_WORLD = "MekhrubonWorld";
    public static final String LOCALHOST_BANK = "//localhost/bank";

    public static void main(String[] args) {
        Bank bank = new BankImpl(PORT);
        World world = new WorldImpl(PORT);
        try {
            UnicastRemoteObject.exportObject(bank, PORT);
            UnicastRemoteObject.exportObject(world, PORT);
            Naming.rebind(LOCALHOST_BANK, bank);
            Naming.rebind(MEKHRUBON_WORLD, world);
        } catch (RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
        System.out.println("Server started and world created");
    }
}

package ru.ifmo.ctddev.turaev.rmi;

import ru.ifmo.ctddev.turaev.rmi.bank.Bank;
import ru.ifmo.ctddev.turaev.rmi.bank.BankImpl;
import ru.ifmo.ctddev.turaev.rmi.person.World;
import ru.ifmo.ctddev.turaev.rmi.person.WorldImpl;

import java.rmi.*;
import java.rmi.server.*;
import java.net.*;

public class Server implements AutoCloseable {
    private final int port;
    public static final String LOCALHOST_BANK = "//localhost/bank";
    public static final String MEKH_WORLD = "mekh_world";

    public Server() {
        this(8888);
    }
    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        try {
            new Server(Integer.parseInt(args[0])).start();
        } catch (NumberFormatException e) {
            System.out.println("The first argument should be an integer");
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Should give port number as an argument");
            e.printStackTrace();
        }  catch (RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }

    public void start() throws MalformedURLException, RemoteException {
        Bank bank = new BankImpl(port);
        World world = new WorldImpl(port);
        System.out.println(LOCALHOST_BANK);
        UnicastRemoteObject.exportObject(bank, port);
        UnicastRemoteObject.exportObject(world, port);
        Naming.rebind(LOCALHOST_BANK, bank);
        Naming.rebind(MEKH_WORLD, world);
        System.out.println("Server started");
    }

    @Override
    public void close() throws Exception {
        Naming.unbind(LOCALHOST_BANK);
        Naming.unbind(MEKH_WORLD);
    }
}

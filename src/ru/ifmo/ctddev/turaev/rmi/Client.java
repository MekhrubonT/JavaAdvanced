package ru.ifmo.ctddev.turaev.rmi;

import ru.ifmo.ctddev.turaev.rmi.bank.Account;
import ru.ifmo.ctddev.turaev.rmi.bank.Bank;
import ru.ifmo.ctddev.turaev.rmi.person.LocalPersonImpl;
import ru.ifmo.ctddev.turaev.rmi.person.Person;
import ru.ifmo.ctddev.turaev.rmi.person.World;

import java.rmi.*;
import java.net.*;
import java.util.Random;

import static ru.ifmo.ctddev.turaev.rmi.Server.LOCALHOST_BANK;


public class Client {

    public static final String LOCALHOST_BANK = "//localhost/bank";
    public static final String MEKH_WORLD = "mekh_world";
    private final Bank bank;
    private final World world;

    public Client() throws RemoteException, NotBoundException, MalformedURLException {
        bank = (Bank) Naming.lookup(LOCALHOST_BANK);
        world = (World) Naming.lookup(MEKH_WORLD);
    }

    public static void main(String[] args) throws RemoteException {
        try {
            System.out.println("Money: " + new Client().run(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]), PersonType.Local));
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
        }
    }

    public int run(String name, String surname, String id, String accountSer, int damount, PersonType type) throws RemoteException, MalformedURLException, NotBoundException {
        world.tryNewPerson(name, surname, id);
        Person person = type == PersonType.Local ? world.getLocalPerson(id) : world.getRemotePerson(id);
        Account account = bank.getAccount(person, accountSer);
        if (account == null) {
            account = bank.createAccount(person, accountSer);
        }
        account.setAmount(account.getAmount() + damount);
        return account.getAmount();
    }
}

package ru.ifmo.ctddev.turaev.rmi;

import ru.ifmo.ctddev.turaev.rmi.bank.Account;
import ru.ifmo.ctddev.turaev.rmi.bank.Bank;
import ru.ifmo.ctddev.turaev.rmi.person.Person;
import ru.ifmo.ctddev.turaev.rmi.person.World;

import java.rmi.*;
import java.net.*;

import static ru.ifmo.ctddev.turaev.rmi.Server.LOCALHOST_BANK;
import static ru.ifmo.ctddev.turaev.rmi.Server.MEKHRUBON_WORLD;

public class Client {
    public static void main(String[] args) throws RemoteException {
        Bank bank;
        World world;
        try {
            bank = (Bank) Naming.lookup(LOCALHOST_BANK);
            world = (World) Naming.lookup(MEKHRUBON_WORLD);
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        world.tryNewPerson(args[0], args[1], args[2]);
        Person person = world.getLocalPerson(args[2]);

        Account account;
        try {
             account = bank.getAccount(person, args[3]);
        } catch (NullPointerException e) {
            System.out.println("bank is not working");
            return;
        }
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(person, args[3]);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + Integer.parseInt(args[4]));
        System.out.println("Money: " + account.getAmount());
    }
}

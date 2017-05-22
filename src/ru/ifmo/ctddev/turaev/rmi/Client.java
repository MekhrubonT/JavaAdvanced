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
    public static void main(String[] args) throws RemoteException {
        Bank bank;
        World world;
        try {

            System.out.println(LOCALHOST_BANK);
            System.out.println("//localhost/bank");
            bank = (Bank) Naming.lookup("//localhost/bank");
            world = (World) Naming.lookup("mekh_world");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }
        world.tryNewPerson(args[0], args[1], args[2]);
        Person person;
        if (new Random().nextBoolean()) {
            person = world.getLocalPerson(args[2]);
        } else {
            person = world.getRemotePerson(args[2]);
        }
        System.out.println(person.getId() + " " + person.getSurname() + " " + person.getName());
        System.out.println(person + " " + person.hashCode());
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

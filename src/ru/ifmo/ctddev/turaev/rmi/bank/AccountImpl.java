package ru.ifmo.ctddev.turaev.rmi.bank;

import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.rmi.*;

public class AccountImpl implements Account {
    private final Person person;
    private final String accountNumber;

    private int amount;

    public AccountImpl(Person person, String accountNumber) {
        this.person = person;
        this.accountNumber = accountNumber;
        amount = 0;
    }

    @Override
    public int hashCode() {
        return (person.hashCode() << 9) ^ accountNumber.hashCode();
    }

    public Person getPerson() throws RemoteException {
        return person;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}

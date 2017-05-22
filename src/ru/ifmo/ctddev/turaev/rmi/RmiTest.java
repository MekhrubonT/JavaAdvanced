package ru.ifmo.ctddev.turaev.rmi;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import ru.ifmo.ctddev.turaev.rmi.bank.Account;
import ru.ifmo.ctddev.turaev.rmi.person.LocalPersonImpl;
import ru.ifmo.ctddev.turaev.rmi.person.Person;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Created by mekhrubon on 22.05.2017.
 */
public class RmiTest {
    private Map<String, Person> myLittleWorld = new HashMap<>();
    private Map<Person, Map<String, Account>> bank = new HashMap<>();
    Random random = new Random(System.nanoTime());



    private void addPerson(Person person) {
        try {
            myLittleWorld.putIfAbsent(person.getId(), person);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void addAccount(Person person, String accountSerial) {
        bank.putIfAbsent(person, new HashMap<>());
        bank.get(person).putIfAbsent(accountSerial, new Account() {
            int amount = 0;
            @Override
            public Person getPerson() throws RemoteException {
                return person;
            }

            @Override
            public int getAmount() throws RemoteException {
                return amount;
            }

            @Override
            public void setAmount(int amount) throws RemoteException {
                this.amount = amount;
            }
        });
    }
    private int addMoney(Person person, String accountSerial, int damount) throws RemoteException {
        Account account = bank.get(person).get(accountSerial);
        int amount = account.getAmount() + damount;
        account.setAmount(amount);
        return amount;
    }

    void baseTest(int operationsAmount, int peopleAmount, List<Integer> s) throws Exception {
        List<Person> people = new ArrayList<>();
        List<List<String>> accounts = new ArrayList<>();

        for (int i = 0; i < peopleAmount; ++i) {
            people.add(new Person() {
                String name = RandomStringUtils.randomAlphabetic(random.nextInt(10));
                String surname = RandomStringUtils.randomAlphabetic(random.nextInt(10));
                String id = RandomStringUtils.randomAlphabetic(random.nextInt(10));
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
                public String getString() throws RemoteException {
                    return surname + name + id;
                }
            });
            accounts.add(new ArrayList<>());
            addPerson(people.get(i));
            for (int j = 0; j < s.get(i); ++j) {
                accounts.get(i).add(RandomStringUtils.randomAlphabetic(10));
                addAccount(people.get(i), accounts.get(i).get(j));
            }
        }

        Client client = new Client();
        try (Server server = new Server()) {
            server.start();
            for (int operations = 0; operations < operationsAmount; operations++) {
                int personNumber = random.nextInt(peopleAmount);
                int accountNumber = random.nextInt(s.get(personNumber));
                int dx = random.nextInt(2001) - 1000;
                Person person = people.get(personNumber);
                String accountSerial = accounts.get(personNumber).get(accountNumber);
                PersonType type = random.nextBoolean() ? PersonType.Local : PersonType.Remote;

                int expected = addMoney(person, accountSerial, dx);
                assertEquals(expected,
                        client.run(person.getName(), person.getSurname(), person.getId(), accountSerial, dx, type));
            }
        }
    }

    @Test
    public void singlePersonSingleAccount() throws Exception {
        baseTest(100, 1, Collections.singletonList(1));
    }

    @Test
    public void singlePersonMultipleAccounts() throws Exception {
        baseTest(100, 1, Collections.singletonList(random.nextInt(100) + 1));
    }

    @Test
    public void multiplePersonsSingleAccount() throws Exception {
        baseTest(2000, 100, random.ints(100, 1, 2).boxed()
                .collect(Collectors.toList()));
    }

    @Test
    public void multiplePersonsMultipleAccounts() throws Exception {
        baseTest(2000, 100, random.ints(100, 1, 101).boxed()
                .collect(Collectors.toList()));

    }
}

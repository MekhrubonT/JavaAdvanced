package ru.ifmo.ctddev.turaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.min;

public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper = null;
    public IterativeParallelism() {}
    public IterativeParallelism(ParallelMapper  mapper) {
        this.mapper = mapper;
    }

    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        return baseFunc(i, list,
                l -> l.map(Object::toString).collect(Collectors.joining()),
                l -> l.map(Object::toString).collect(Collectors.joining())
                );
    }

    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return baseFunc(
                i,
                list,
                l -> l.filter(predicate).collect(Collectors.toList()),
                l1 -> l1.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }


    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return baseFunc(i, list,
                l -> l.map(function).collect(Collectors.toList()),
                l -> l.flatMap(Collection::stream).collect(Collectors.toList()));
    }


    private <T, R> R baseFunc(int pThreads, final List<? extends T> list,
                              final Function<Stream<? extends T>, R> threadHandle,
                              final Function<? super Stream<R>, R> resultCollector) throws InterruptedException {

        int len = list.size() / pThreads;
        int g = list.size() % pThreads;

        List<List<? extends T>> temp = new ArrayList<>();
        for (int cur = 0; cur < list.size(); g--) {
            int prev = cur;
            cur += len + (g > 0 ? 1 : 0);
            temp.add(list.subList(prev, cur));
        }

        List<R> result;
        if (mapper != null) {
            result = mapper.map(l -> threadHandle.apply(l.stream()), temp);
        } else {
            result = new ArrayList<>();
            List<Thread> myThreads = new ArrayList<>();
            for (List<? extends T> sublist : temp) {
                result.add(null);
                Thread thread = new Thread(() -> result.set(result.size(), threadHandle.apply(sublist.stream())));thread.start();
                myThreads.add(thread);
            }
            for (Thread myThread : myThreads) {
                myThread.join();
            }
        }

        return resultCollector.apply(result.stream());
    }


    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> temp = l -> l.max(comparator).get();
        return baseFunc(min(i, list.size()), list, temp, temp);
    }

    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(i, list, comparator.reversed());
    }

    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return baseFunc(i, list, l -> l.allMatch(predicate), x -> x.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return !all(i, list, t -> !predicate.test(t));
    }
}



/*
java -ea -classpath "D:\java\JavaAdvanced\out\production\HW1;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\IterativeParallelismTest.jar" info.kgeorgiy.java.advanced.concurrent.Tester list ru.ifmo.ctddev.turaev.concurrent.IterativeParallelism

 */

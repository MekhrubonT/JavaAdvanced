package ru.ifmo.ctddev.turaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Integer.min;

public class IterativeParallelism implements ListIP {
    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        Function<List<?>, String> joiner = l -> l.stream().map(Object::toString).collect(Collectors.joining());
        return baseFunc(i, list, joiner, joiner);
    }

    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        Function<List<? extends T>, List<T>> threadHandler =
                l -> l.stream().filter(predicate).collect(Collectors.toList());
        Function<List<? extends List<T>>, List<T>> answerCollector =
                l -> l.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return baseFunc(i, list, threadHandler, answerCollector);
    }

    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        Function<List<? extends T>, List<U>> threadHandler =
                l -> l.stream().map(function).collect(Collectors.toList());
        Function<List<? extends List<U>>, List<U>> answerCollector =
                l -> l.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return baseFunc(i, list, threadHandler, answerCollector);
    }
    private <T, R> R baseFunc(int pThreads, final List<? extends T> list, final Function<List<? extends T>, R> threadHandle,
                              final Function<? super List<R>, R> resultCollector) throws InterruptedException {

        ArrayList<R> result = new ArrayList<>();
        ArrayList<Thread> myThreads = new ArrayList<>();
        int len = (list.size() + pThreads - 1) / pThreads;

        for (int cur = 0; cur < list.size(); cur += len) {
            final int start = cur;
            final int end = min(list.size(), cur + len);
            final int position = result.size();
            result.add(null);
            myThreads.add(new Thread(() -> result.set(position, threadHandle.apply(list.subList(start, end)))));
            myThreads.get(myThreads.size() - 1).start();
        }

        for (Thread myThread : myThreads) {
            myThread.join();
        }
        return resultCollector.apply(result);
    }


    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> reduce = l -> Collections.max(l, comparator);
        return baseFunc(min(i, list.size()), list, reduce, reduce);
    }

    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(i, list, comparator.reversed());
    }

    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        Function<List<? extends T>, Boolean> allMatchForThreads = l -> l.stream().allMatch(predicate);
        return baseFunc(i, list, allMatchForThreads, x -> x.stream().allMatch(y -> y.equals(true)));
    }

    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return !all(i, list, t -> !predicate.test(t));
    }
}



/*
java -ea -classpath "D:\java\JavaAdvanced\out\production\HW1;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\ArraySetTest.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\JarImplementorTest.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\WalkTest.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\IterativeParallelismTest.jar" info.kgeorgiy.java.advanced.concurrent.Tester list ru.ifmo.ctddev.turaev.concurrent.IterativeParallelism
 */
package ru.ifmo.ctddev.turaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.min;

public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper = null;

    public IterativeParallelism() {
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String join(int threads, List<?> list) throws InterruptedException {
        Function<Stream<?>, String> joiner =
                inputStream -> inputStream.map(Object::toString).collect(Collectors.joining());
        return baseFunc(threads, list, joiner, joiner);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> list,
                              Predicate<? super T> predicate) throws InterruptedException {
        return baseFunc(
                threads,
                list,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }


    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list,
                              Function<? super T, ? extends U> function) throws InterruptedException {
        return baseFunc(threads, list,
                stream -> stream.map(function).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }


    private <T, R> R baseFunc(int pThreads, final List<? extends T> list,
                              final Function<Stream<? extends T>, R> threadHandle,
                              final Function<? super Stream<R>, R> resultCollector) throws InterruptedException {

        int len = list.size() / pThreads;
        int g = list.size() % pThreads;
        List<Stream<? extends T>> temp = new ArrayList<>();
        for (int cur = 0; cur < list.size(); g--) {
            int prev = cur;
            cur += len + (g > 0 ? 1 : 0);
            temp.add(list.subList(prev, cur).stream());
        }

        List<R> result;
        if (mapper != null) {
            result = mapper.map(threadHandle, temp);
        } else {
            result = new ArrayList<>();
            List<Thread> myThreads = new ArrayList<>();
            for (Stream<? extends T> sublist : temp) {
                result.add(null);
                Thread thread = new Thread(() -> result.set(result.size(), threadHandle.apply(sublist)));
                thread.start();
                myThreads.add(thread);
            }
            for (Thread myThread : myThreads) {
                myThread.join();
            }
        }

        return resultCollector.apply(result.stream());
    }


    @Override
    public <T> T maximum(int threads, List<? extends T> list,
                         Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> temp = stream -> stream.max(comparator).get();
        return baseFunc(min(threads, list.size()), list, temp, temp);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> list,
                         Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, list, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> list,
                           Predicate<? super T> predicate) throws InterruptedException {
        return baseFunc(threads, list,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> list,
                           Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, list, t -> !predicate.test(t));
    }
}



/*
java -ea -classpath "D:\java\JavaAdvanced\out\production\HW1;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\IterativeParallelismTest.jar" info.kgeorgiy.java.advanced.concurrent.Tester list ru.ifmo.ctddev.turaev.concurrent.IterativeParallelism

 */

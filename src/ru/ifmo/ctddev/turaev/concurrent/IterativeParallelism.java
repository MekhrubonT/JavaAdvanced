package ru.ifmo.ctddev.turaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
        List<Stream<? extends T>> subStreams = new ArrayList<>();
        for (int cur = 0; cur < list.size(); g--) {
            final int prev = cur;
            cur += len + (g > 0 ? 1 : 0);
            subStreams.add(list.subList(prev, cur).stream());
        }

        List<R> result;
        if (mapper != null) {
            result = mapper.map(threadHandle, subStreams);
        } else {
            result = new ArrayList<>(Collections.nCopies(subStreams.size(), null));
            List<Thread> myThreads = new ArrayList<>();
            IntStream.range(0, subStreams.size()).forEach(position -> Utils.addAndStart(myThreads,
                    new Thread(() -> result.set(position, threadHandle.apply(subStreams.get(position))))));
            Utils.threadJoiner(myThreads);
        }
        return resultCollector.apply(result.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> list,
                         Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> temp = stream -> stream.max(comparator).get();

//        System.out.println("threads=" + threads);
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

//"C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\bin\java" -ea -classpath "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\cldrdata.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\dnsns.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\jaccess.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\jfxrt.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\localedata.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\nashorn.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\sunec.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\sunjce_provider.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\sunmscapi.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\sunpkcs11.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\ext\zipfs.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\jce.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\jfxswt.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\jsse.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\management-agent.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\resources.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\jre\jre\lib\rt.jar;D:\java\JavaAdvanced\out\production\JavaAdvanced;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\ParallelMapperTest.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2016.3.4\lib\idea_rt.jar" info.kgeorgiy.java.advanced.mapper.Tester list ru.ifmo.ctddev.turaev.concurrent.ParallelMapperImpl,ru.ifmo.ctddev.turaev.concurrent.IterativeParallelism
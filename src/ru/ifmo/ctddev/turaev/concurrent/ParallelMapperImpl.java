package ru.ifmo.ctddev.turaev.concurrent;


import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> order = new ArrayDeque<>();
    private final List<Thread> workerThreads = new ArrayList<>();

    public ParallelMapperImpl(int threads) {
        IntStream.range(0, threads).forEach(ignoredIndex ->
            Utils.addAndStart(workerThreads, new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable fromQueue;
                        synchronized (order) {
                            while (order.isEmpty()) {
                                order.wait();
                            }
                            fromQueue = order.remove();
                        }
                        fromQueue.run();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            })));
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function,
                              List<? extends T> list) throws InterruptedException {
        System.out.println("mapper " + list.toString());
        ResultCollector<R> data = new ResultCollector<>(list.size());
            IntStream.range(0, list.size()).forEach(current -> {
                        synchronized (order) {
                            order.add(() -> data.setResult(current, function.apply(list.get(current))));
                            order.notify();
                        }
                    }
            );
        return data.getResult();
    }

    @Override
    public void close() throws InterruptedException {
        workerThreads.forEach(Thread::interrupt);
        Utils.threadJoiner(workerThreads);
    }

    private class ResultCollector<R> {
        List<R> data;
        int left;

        ResultCollector(int size) {
            left = size;
            data = new ArrayList<>(Collections.nCopies(size, null));
        }

        void setResult(int pos, R res) {
            data.set(pos, res);
            synchronized (this) {
                left--;
                if (left == 0) {
                    this.notify();
                }
            }
        }

        synchronized List<R> getResult() throws InterruptedException {
            while (left > 0) {
                this.wait();
            }
            return data;
        }
    }
}

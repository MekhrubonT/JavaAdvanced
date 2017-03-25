package ru.ifmo.ctddev.turaev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> order = new ArrayDeque<>();
    private final List<Thread> workerThreads = new ArrayList<>();

    public ParallelMapperImpl(int threads) {
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
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
                }
            });
            t.start();
            workerThreads.add(t);
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        ResultCollector<R> data = new ResultCollector<>(list.size());
        synchronized (order) {
            for (int i = 0; i < list.size(); i++) {
                final int current = i;
                order.add(() -> data.setResult(current, function.apply(list.get(current))));
                order.notify();
            }
        }
        return data.getResult();
    }

    @Override
    public void close() throws InterruptedException {
        workerThreads.forEach(Thread::interrupt);
        for (Thread workerThread : workerThreads) {
            workerThread.join();
        }
    }

    private class ResultCollector<R>  {
        ArrayList<R> data;
        int left;

        ResultCollector(int size) {
            left = size;
            data = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                data.add(null);
            }
        }

        synchronized void setResult(int pos, R res) {
            data.set(pos, res);
            left--;
            if (left == 0) {
                this.notify();
            }
        }

        synchronized List<R> getResult() throws InterruptedException {
            if (left > 0) {
                this.wait();
            }
            return data;
        }
    }
}

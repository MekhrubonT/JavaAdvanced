package ru.ifmo.ctddev.turaev.concurrent;

import java.util.List;

class Utils {
    static void threadJoiner(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }
    static void addAndStart(List<Thread> threads, Thread newThread) {
        threads.add(newThread);
        newThread.start();
    }
}

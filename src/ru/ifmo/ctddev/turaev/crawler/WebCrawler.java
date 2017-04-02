package ru.ifmo.ctddev.turaev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractorPool;
    private final LinkHandler handler;

    class Waiter {
        int unfinishedThreads = 0;

        synchronized void inc() {
            unfinishedThreads++;
//            System.out.println("+1\t" + unfinishedThreads);
        }
        synchronized void dec() {
//            System.out.println("-1\t" + (unfinishedThreads - 1));
            if (--unfinishedThreads == 0) {
                this.notify();
            }
        }
        synchronized void waitAll() {
            while (unfinishedThreads != 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) throws FileNotFoundException {
        System.out.println(downloaders + " " + extractors + " " + perHost);
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
        handler = new LinkHandler(perHost);
        this.downloader = downloader;
    }

    @Override
    public Result download(String url, int depth) {
        System.out.println(url + " " + depth);
        final Waiter waiter = new Waiter();
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();

        myDownload(waiter, downloaded, errors, url, depth);

        waiter.waitAll();
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void myDownload(Waiter waiter, Set<String> downloaded,
                            Map<String, IOException> errors, String url, int depth) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return;
        }
        if (depth >= 1 && downloaded.add(url)) {
            waiter.inc();
            handler.add(host, () -> {
                try {
                    final Document document = downloader.download(url);
                    if (depth == 1) {
                        return;
                    }
                    waiter.inc();
                    extractorPool.submit(() -> {
                        try {
                            document.extractLinks().forEach(s ->
                                    myDownload(waiter, downloaded, errors, s, depth - 1));
                        } catch (IOException e) {
                            errors.put(url, e);
                        } finally {
                            waiter.dec();
                            System.out.println("finish");
                        }
                    });
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    handler.finish(host);
                    waiter.dec();
                    System.out.println("finish");
                }
            });
        }
    }


    @Override
    public void close() {
        downloaderPool.shutdownNow();
        extractorPool.shutdownNow();
    }

    class LinkHandler {
        Map<String, Queue<Runnable>> linksByHost = new ConcurrentHashMap<>();
        Map<String, Integer> currentlyRan = new ConcurrentHashMap<>();
        final int maxPerHost;

        LinkHandler(int maxPerHost) {
            this.maxPerHost = maxPerHost;
        }

        void add(String host, Runnable url) {
            currentlyRan.putIfAbsent(host, 0);
            if (currentlyRan.get(host) == maxPerHost) {
                linksByHost.putIfAbsent(host, new LinkedBlockingQueue<>());
                linksByHost.get(host).add(url);
            } else {
                currentlyRan.compute(host, (hostName, num) -> num + 1);
                downloaderPool.submit(url);
            }
        }
        void finish(String host) {
            if (linksByHost.containsKey(host)) {
                final Queue<Runnable> temp = linksByHost.get(host);
                synchronized (temp) {
                    if (!temp.isEmpty()) {
                        downloaderPool.submit(temp.poll());
                    }
                    if (temp.isEmpty()) {
                        linksByHost.remove(host);
                    }
                }
            } else {
                currentlyRan.compute(host, (hostName, num) -> num - 1);
            }

        }
    }
}

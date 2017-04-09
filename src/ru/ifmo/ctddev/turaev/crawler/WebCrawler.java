package ru.ifmo.ctddev.turaev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import javafx.util.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractorPool;
    private final LinkHandler handler;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
        handler = new LinkHandler(perHost);
        this.downloader = downloader;
    }

    // url depth downloaders extractors perhost
    public static void main(String[] args) {
        try (Crawler crawler = new WebCrawler(new CachingDownloader(),
                Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]))) {
            crawler.download(args[0], Integer.parseInt(args[1]));
        } catch (IOException e) {
            System.out.println("Error while creating instance of CachingDownloader: " + e);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Not enough command line arguments" + e);
        } catch (NumberFormatException e) {
            System.out.println("Wrong format of arguments" + e);
        }
    }

    @Override
    public Result download(String url, int depth) {
        Phaser waiter = new Phaser(1);
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        myDownload(waiter, downloaded, errors, url, depth);
        waiter.arriveAndAwaitAdvance();
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void errorCatcherWrapper(Phaser waiter, Map<String, IOException> errors, String url, Callable<?> callable) {
        try {
            callable.call();
        } catch (IOException e) {
            errors.put(url, e);
        } catch (Exception e) {
            System.out.println(e.getMessage() + " " + e.getCause());
            e.printStackTrace();
        } finally {
            waiter.arrive();
        }
    }

    private void myDownload(Phaser waiter, Set<String> downloaded,
                            Map<String, IOException> errors, String url, int depth) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return;
        }
        if (depth >= 1 && downloaded.add(url)) {
            waiter.register();
            handler.add(host, () -> errorCatcherWrapper(waiter, errors, url,
                    () -> {
                        try {
                            final Document document = downloader.download(url);
                            if (depth != 1) {
                                waiter.register();
                                extractorPool.submit(() -> errorCatcherWrapper(waiter, errors, url,
                                        () -> {
                                            document.extractLinks().forEach(s -> myDownload(waiter, downloaded, errors, s, depth - 1));
                                            return null;
                                        }));
                            }
                        } finally {
                            handler.finish(host);
                        }
                        return null;
                    }));
        }
    }


    @Override
    public void close() {
        downloaderPool.shutdown();
        extractorPool.shutdown();
    }

    private class LinkHandler {
        private final int maxPerHost;
        private final Map<String, Pair<Integer, Queue<Runnable>>> currentlyRan = new ConcurrentHashMap<>();

        LinkHandler(int maxPerHost) {
            this.maxPerHost = maxPerHost;
        }

        void add(String host, Runnable url) {
            currentlyRan.putIfAbsent(host, new Pair<>(0, null));
            currentlyRan.compute(host, (key, oldVal) -> {
                if (oldVal.getKey() == maxPerHost) {
                    Queue<Runnable> q = new ConcurrentLinkedQueue<>(oldVal.getValue() == null ? Collections.emptyList() : oldVal.getValue());
                    q.add(url);
                    return new Pair<>(oldVal.getKey(), q);
                } else {
                    downloaderPool.submit(url);
                    return new Pair<>(oldVal.getKey() + 1, oldVal.getValue());
                }
            });
        }

        void finish(String host) {
            currentlyRan.compute(host, (key, oldVal) -> {
                    if (oldVal.getValue() != null) {
                        Queue<Runnable> q = new ConcurrentLinkedQueue<>(oldVal.getValue() == null ? Collections.emptyList() : oldVal.getValue());
                        downloaderPool.submit(q.poll());
                        if (q.isEmpty()) {
                            q = null;
                        }
                        return new Pair<>(oldVal.getKey(), q);
                    } else {
                        return new Pair<>(oldVal.getKey() - 1, null);
                    }
            });
        }
    }
}

//java -ea -classpath "D:\java\JavaAdvanced\out\production\JavaAdvanced;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\WebCrawlerTest.jar" info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.turaev.crawler.WebCrawler

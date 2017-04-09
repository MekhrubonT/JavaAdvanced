package ru.ifmo.ctddev.turaev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractorPool;
    private final ExecutorService perHostLimitHandlerPool;

    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
        perHostLimitHandlerPool = Executors.newCachedThreadPool();
        this.downloader = downloader;
        this.perHost = perHost;
    }

    // url depth downloaders extractors perhost
    public static void main(String[] args) {
        try (Crawler crawler = new WebCrawler(new CachingDownloader(),
                Integer.parseInt(args[2]), Integer. parseInt(args[3]), Integer.parseInt(args[4]))) {
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
        Map<String, Semaphore> permission = new ConcurrentHashMap<>();
        Phaser waiter = new Phaser(1);
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        myDownload(waiter, downloaded, errors, permission, url, depth);
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
                            Map<String, IOException> errors, Map<String, Semaphore> permission, String url, int depth) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return;
        }
        if (depth >= 1 && downloaded.add(url)) {
            waiter.register();
            perHostLimitHandlerPool.submit(() -> {
                try {
                    permission.putIfAbsent(host, new Semaphore(perHost));
                    Semaphore cur = permission.get(host);
                    cur.acquire();
                    downloaderPool.submit(() -> errorCatcherWrapper(waiter, errors, url,
                            () -> {
                                try {
                                    final Document document = downloader.download(url);
                                    if (depth != 1) {
                                        waiter.register();
                                        extractorPool.submit(() -> errorCatcherWrapper(waiter, errors, url,
                                                () -> {
                                                    document.extractLinks().forEach(s -> myDownload(waiter, downloaded, errors, permission, s, depth - 1));
                                                    return null;
                                                }));
                                    }
                                } finally {
                                    cur.release();
                                }
                                return null;
                            }));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    @Override
    public void close() {
        downloaderPool.shutdown();
        extractorPool.shutdown();
        perHostLimitHandlerPool.shutdown();
    }
}

//java -ea -classpath "D:\java\JavaAdvanced\out\production\JavaAdvanced;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\WebCrawlerTest.jar" info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.turaev.crawler.WebCrawler

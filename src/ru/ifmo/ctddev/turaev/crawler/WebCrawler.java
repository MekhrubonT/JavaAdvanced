package ru.ifmo.ctddev.turaev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

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
        private final Map<String, Queue<Runnable>> linksByHost = new ConcurrentHashMap<>();
        private final Map<String, Integer> currentlyRan = new ConcurrentHashMap<>();

        LinkHandler(int maxPerHost) {
            this.maxPerHost = maxPerHost;
        }


        void add(String host, Runnable url) {
            currentlyRan.putIfAbsent(host, 0);
            currentlyRan.compute(host, (key, oldVal) -> {
                    if (oldVal == maxPerHost) {
                        linksByHost.putIfAbsent(host, new ConcurrentLinkedQueue<>());
                        linksByHost.get(host).add(url);
                        return oldVal;
                    } else {
                        downloaderPool.submit(url);
                        return oldVal + 1;
                    }
            });
        }

        void finish(String host) {
            currentlyRan.compute(host, (key, oldVal) -> {
                    final Queue<Runnable> temp = linksByHost.get(host);
                    if (temp != null && !temp.isEmpty()) {
                        downloaderPool.submit(temp.poll());
                        if (temp.isEmpty()) {
                            //
                            linksByHost.remove(host);
                        }
                        return oldVal;
                    } else {
                        return oldVal - 1;
                    }
            });
        }
    }
}

//java -ea -classpath "D:\java\JavaAdvanced\out\production\JavaAdvanced;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\WebCrawlerTest.jar" info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.turaev.crawler.WebCrawler

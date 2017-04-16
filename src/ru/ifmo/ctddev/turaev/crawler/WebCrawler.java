package ru.ifmo.ctddev.turaev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation class for Crawler.
 * Accepts parallelism threshold for downloads, extractions and connection to same host can be specified.
 * Downloads pages and objects from given URL. For remote addresses network connection or internet is required.
 */
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractorPool;
    private final LinkHandler handler;

    /**
     * Creates parallel Crawler, that uses Downloader downloader to download object
     * by link and extracts all links in it. Other arguments set properties of parallelism.
     *
     * @param downloader  the download object, used to download link
     * @param downloaders upper bound of parallel downloads simultaneously.
     * @param extractors  upper bound of parallel extractors simultaneously.
     * @param perHost     upper bound of parallel downloadings from the same host simultaneously.
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
        handler = new LinkHandler(perHost);
        this.downloader = downloader;
    }

    /**
     * Creates {@link WebCrawler} and downloads pages and files from given url. Uses CachingDownloader
     * as Downloader object.
     *
     * @param args contains {@link java.net.URL} URL to download from and next integers: maximal depth,
     *             downloaders, extractors and connection to single host amount in appropriate order.
     * @see WebCrawler#download(String, int)
     */
    public static void main(String[] args) {
        try (Crawler crawler = new WebCrawler(new CachingDownloader(),
                Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]))) {
            crawler.download(args[0], Integer.parseInt(args[1]));
        } catch (IOException e) {
            System.out.println("Cannot create instance of CachingDownloader: " + e);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Not enough command line arguments" + e);
        } catch (NumberFormatException e) {
            System.out.println("The integer number expected in the argument: " + e);
        }
    }

    /**
     * Recursively walks the pages starting from given URL to given depth, downloads them and return the list of
     * downloaded pages and file. If an error occured, the page is added to list of errors.
     *
     * @param url   given {@link java.net.URL}
     * @param depth maximal depth of recursion
     * @return The list ob succesfully downloaded objects and list of objects. The object is placed in error list if an
     * error occured while downloading
     */
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
        if (depth >= 1 && downloaded.add(url)) {
            final String host;
            try {
                host = URLUtils.getHost(url);
            } catch (MalformedURLException e) {
                errors.put(url, e);
                return;
            }
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

    /**
     * Terminates all threads and stopы downloading.
     */
    @Override
    public void close() {
        downloaderPool.shutdown();
        extractorPool.shutdown();
    }

    private class LinkHandler {
        private final int maxPerHost;
        private final Map<String, HostData> countersAndQueue = new ConcurrentHashMap<>();
        LinkHandler(int maxPerHost) {
            this.maxPerHost = maxPerHost;
        }

        void add(String host, Runnable url) {
            HostData hostData = countersAndQueue.putIfAbsent(host, new HostData());
            if (hostData == null) {
                hostData = countersAndQueue.get(host);
            }
            hostData.locker.lock();
            if (hostData.connections == maxPerHost) {
                hostData.q.add(url);
            } else {
                downloaderPool.submit(url);
                hostData.connections++;
            }
            hostData.locker.unlock();
        }

        void finish(String host) {
            HostData hostData = countersAndQueue.get(host);
            hostData.locker.lock();
            if (!hostData.q.isEmpty()) {
                downloaderPool.submit(hostData.q.poll());
            } else {
                hostData.connections--;
            }
            hostData.locker.unlock();
        }

        private class HostData {
            final Queue<Runnable> q;
            final ReentrantLock locker;
            int connections;

            HostData() {
                connections = 0;
                this.q = new LinkedBlockingQueue<>();
                this.locker = new ReentrantLock();
            }
        }
    }
}

//java -ea -classpath "D:\java\JavaAdvanced\out\production\JavaAdvanced;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\WebCrawlerTest.jar" info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.turaev.crawler.WebCrawler
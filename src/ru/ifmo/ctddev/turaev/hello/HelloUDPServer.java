package ru.ifmo.ctddev.turaev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.stream.Stream;


/**
 * This class represents implementaion of HelloServer. For getting more information about server
 * read {@link HelloUDPServer#start(int, int)} )}
 * Created by mekhrubon on 16.04.2017.
 */
public class HelloUDPServer implements HelloServer, AutoCloseable {
    private ServerInstance server = null;

    /**
     * Constructs an UDP Server, which accept request and response next: "Hello, " + request.
     */
    public HelloUDPServer() {
    }

    /**
     * Construct HelloUDPServer and starts it, using arguments in args[]
     *
     * @param args contains arguments for {@link HelloUDPServer#start(int, int)} in next order:
     *             port, threads
     *             {@link HelloUDPServer#start(int, int)}
     */
    public static void main(String[] args) {
        int data[] = Utils.checkArguments(new int[]{0, 1}, args, 2);
        new HelloUDPServer().start(data[0], data[1]);
    }


    /**
     * Simultaneously accepts request using specified number of ports.
     * The responsing data is byte[] presentation of String "Hello, " + request.
     *
     * @param port    port number of server
     * @param threads number of threads, used to handle requests
     */
    @Override
    public void start(int port, int threads) {
        try {
            server = new ServerInstance(port, threads);
        } catch (SocketException e) {
            System.err.println("The socket could not be opened, or the socket could not bind to the specified local port: " + e.getMessage());
        }
    }

    /**
     * Shutdowns the server and close socket
     */
    @Override
    public void close() {
        server.close();
    }

    private class ServerInstance implements AutoCloseable {
        private final Thread[] serverWorkingThreads;
        private final DatagramSocket socket;

        ServerInstance(int port, int threads) throws SocketException {
//            serverWorkingThreads = new Thread[threads];
            socket = new DatagramSocket(port);
//            Utils.TimeManager timeManager = new Utils.TimeManager(socket);
            serverWorkingThreads = Stream.generate(() -> new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
//                        System.out.println("here");
                        DatagramPacket p = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                        socket.receive(p);
                        String response = "Hello, " + new String(p.getData(), p.getOffset(), p.getLength(), Util.CHARSET);
                        p.setData(response.getBytes(Util.CHARSET), 0, response.getBytes().length);
                        socket.send(p);
//                        timeManager.update(1);
//                        System.out.println(response);
                    }
                } catch (PortUnreachableException e) {
                    System.out.println("Socket is connected to a currently unreachable destination: " + e.getMessage());
//                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("An I/O error occured: " + e.getMessage());
//                    e.printStackTrace();
                }

            })).limit(threads).toArray(Thread[]::new);
            Arrays.stream(serverWorkingThreads).forEach(Thread::start);
        }

        @Override
        public void close() {
            for (Thread serverWorkingThread : serverWorkingThreads) {
                serverWorkingThread.interrupt();
            }
            for (Thread serverWorkingThread : serverWorkingThreads) {
                while (!serverWorkingThread.isInterrupted()) {
                    try {
                        serverWorkingThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            socket.close();
        }
    }
}


//java -ea -classpath "D:\JavaAdvanced\out\production\JavaAdvanced;D:\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\JavaAdvanced\java-advanced-2017\artifacts\HelloUDPTest.jar" ru.ifmo.ctddev.turaev.hello.HelloUDPServer 12345 1
//java -ea -classpath "D:\JavaAdvanced\out\production\JavaAdvanced;D:\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\JavaAdvanced\java-advanced-2017\artifacts\HelloUDPTest.jar" info.kgeorgiy.java.advanced.hello.Tester server ru.ifmo.ctddev.turaev.hello.HelloUDPServer
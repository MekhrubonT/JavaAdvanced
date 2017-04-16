package ru.ifmo.ctddev.turaev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.Util;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer  {
    private ServerInstance server = null;

    @Override
    public void start(int port, int threads) {
        try {
            server = new ServerInstance(port, threads);
        } catch (SocketException e) {
            System.out.println("Can't create DatagramSocket " + e.getMessage());
        }
    }

    @Override
    public void close() {
        server.close();
    }

    class ServerInstance implements AutoCloseable {
        private final ExecutorService serverPools;
        private final DatagramSocket socket;

        ServerInstance(int port, int threads) throws SocketException {
            socket = new DatagramSocket(port);
            serverPools = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                serverPools.submit(() -> {
                    try {
                        while (!Thread.interrupted()) {
                            DatagramPacket p = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                            socket.receive(p);
                            String request = "Hello, " + new String(p.getData(), p.getOffset(), p.getLength(), Util.CHARSET);
                            p.setData(request.getBytes(Util.CHARSET), 0, request.getBytes().length);
                            socket.send(p);
                        }
                    } catch (IOException e) {
                    }
                });
            }
        }

        @Override
        public void close() {
            serverPools.shutdownNow();
            socket.close();
        }
    }
}
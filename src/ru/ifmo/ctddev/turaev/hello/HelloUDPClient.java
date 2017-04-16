package ru.ifmo.ctddev.turaev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.Util;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import org.omg.CORBA.TIMEOUT;
import ru.ifmo.ctddev.turaev.concurrent.ParallelMapperImpl;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by mekhrubon on 16.04.2017.
 */
public class HelloUDPClient implements HelloClient {

    private static final int CONNECTIONTIMEOUT = 1000;

    private String requestForm(String prefix, int threadInd, int requestInd) {
        return prefix + threadInd + "_" + requestInd;
    }
    private String expectedResponse(String prefix, int threadInd, int requestInd) {
        return "Hello, " + requestForm(prefix, threadInd, requestInd);
    }


    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        System.out.println(host + " " + port + " " + prefix + " " + requests + " " + threads);
        try(ParallelMapper mapper = new ParallelMapperImpl(threads)){
            InetAddress address = InetAddress.getByName(host);
            mapper.map((index) -> {
                System.out.println(index);
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(CONNECTIONTIMEOUT);
                    DatagramPacket response = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                    DatagramPacket request = new DatagramPacket(new byte[1], 1, address, port);
                    for (int reqNumber = 0; reqNumber < requests; ++reqNumber) {
                        byte[] requestByteArray = requestForm(prefix, index, reqNumber).getBytes(Util.CHARSET);
                        String expectedResponseString = expectedResponse(prefix, index, reqNumber);

                        request.setData(requestByteArray, 0, requestByteArray.length);

                        while (true) {
                            try {
                                socket.send(request);
                                socket.receive(response);
                                String responseStr = new String(response.getData(), response.getOffset(), response.getLength(), Util.CHARSET);
                                if (expectedResponseString.equals(responseStr)) {
                                    System.out.println(responseStr);
                                    break;
                                }
                            } catch (IOException e) {
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                return null;
            }, IntStream.range(0, threads).boxed().collect(Collectors.toList()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}

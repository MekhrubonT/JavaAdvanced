package ru.ifmo.ctddev.turaev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.Util;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import ru.ifmo.ctddev.turaev.concurrent.ParallelMapperImpl;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents implementaion of HelloClient.java. For getting more information of client
 * read {@link HelloUDPClient#run(String, int, String, int, int)}
 * Created by mekhrubon on 16.04.2017.
 * //
 */
public class HelloUDPClient implements HelloClient {
    private static final List<String> HELLO_ON_DIFFERENT_LANGUAGES = Arrays.asList("%s Hello", "%s ආයුබෝවන්",
            "Բարեւ, %s", "مرحبا %s", "Салом %s", "Здраво %s", "Здравейте %s", "Прывітанне %s", "Привіт %s", "Привет, %s",
            "Поздрав %s", "سلام به %s", "שלום %s", "Γεια σας %s", "העלא %s", "ہیل%s٪ ے", "Bonjou %s",
            "Bonjour %s", "Bună ziua %s", "Ciao %s", "Dia duit %s", "Dobrý deň %s", "Dobrý den, %s", "Habari %s", "Halló %s", "Hallo %s", "Halo %s", "Hei %s", "Hej %s", "Hello  %s", "Hello %s", "Hello %s", "Helo %s", "Hola %s", "Kaixo %s", "Kamusta %s", "Merhaba %s", "Olá %s", "Ola %s", "Përshëndetje %s", "Pozdrav %s", "Pozdravljeni %s", "Salom %s", "Sawubona %s", "Sveiki %s", "Tere %s", "Witaj %s", "Xin chào %s", "ສະບາຍດີ %s", "สวัสดี %s", "ഹലോ %s", "ಹಲೋ %s", "హలో %s", "हॅलो %s", "नमस्कार%sको", "হ্যালো %s", "ਹੈਲੋ %s", "હેલો %s", "வணக்கம் %s", "ကို %s မင်္ဂလာပါ", "გამარჯობა %s", "ជំរាបសួរ %s បាន", "こんにちは%s", "你好%s", "안녕하세요  %s");

    /**
     * Constructs an UDP Client, which sends request until getting response of form "Hello, " + request.
     */
    public HelloUDPClient() {
    }

    /**
     * Construct HelloUDPClient and starts it, using arguments in args[]
     *
     * @param args contains arguments for {@link HelloUDPClient#run(String, int, String, int, int)} in next order:
     *             <p>
     *             host, port, prefix of requests, threads amount, request per thread.
     */
    public static void main(String[] args) {
        int data[] = Utils.checkArguments(new int[]{1, 3, 4}, args, 5);
        new HelloUDPClient().run(args[0], data[0], args[2], data[1], data[2]);
    }

    private String requestForm(String prefix, int threadInd, int requestInd) {
        return prefix + threadInd + "_" + requestInd;
    }

    private String expectedResponse(String prefix, int threadInd, int requestInd) {
        return "Hello, " + requestForm(prefix, threadInd, requestInd);
    }

    /**
     * Simultaneously send request to the given server using specified number of ports. Requests are formed
     * next way: prefix + threadIndexNumber + "_" + requestIndexNumber.
     * The client expects the response to be next form: "Hello, " + request.
     *
     * @param host     name or IP address of machine with server
     * @param port     port number for requests
     * @param prefix   prefix of request
     * @param requests numbers of requests per thread
     * @param threads  number of threads, used to send requests
     */
    @Override
    public void run(String host, int port, String prefix, int requests, int threads) {
//        System.out.println(prefix);
//        ExecutorService executors = Executors.newFixedThreadPool(threads);
        //System.err.println(host + " " + port + " " + prefix + " " + requests + " " + threads);
        try (ParallelMapper mapper = new ParallelMapperImpl(threads)) {
            InetAddress address = InetAddress.getByName(host);

//            System.out.println(prefix);
            mapper.map(index -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    Utils.TimeManager manager = new Utils.TimeManager(socket);
                    DatagramPacket response = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                    DatagramPacket request = new DatagramPacket(new byte[1], 1, address, port);
                    for (int reqNumber = 0; reqNumber < requests; reqNumber++) {
                        String requestStr = requestForm(prefix, index, reqNumber);
                        byte[] requestByteArray = requestStr.getBytes(StandardCharsets.UTF_8);
                        String expectedResponseString = expectedResponse(prefix, index, reqNumber);

                        request.setData(requestByteArray, 0, requestByteArray.length);

                        while (true) {
                            try {
                                socket.send(request);
                                socket.receive(response);
//                                System.out.println(new String(request.getData()));
//                                System.out.println(new String(response.getData()));
                                String responseStr = new String(response.getData(), response.getOffset(), response.getLength(), Util.CHARSET);
                                if (isExpectedResponce(requestStr, responseStr)) {
                                    System.out.println(requestStr + ": " + responseStr);
                                    break;
                                }
                                manager.update(1);
                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout has expired: " + e.getMessage());
                                manager.update(-1);
//                                e.printStackTrace();
                            } catch (PortUnreachableException e) {
                                System.out.println("Socket is connected to a currently unreachable destination: " + e.getMessage());
//                                e.printStackTrace();
                            } catch (IOException e) {
                                System.err.println("An I/O error occured: " + e.getMessage());
//                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.err.println("The socket could not be opened, is already closed, the socket could " +
                            "not bind to the specified local port or some low-level error occurred: " + e.getMessage());
//                    e.printStackTrace();
                }

                return null;
            }, IntStream.range(0, threads).boxed().collect(Collectors.toList()));
        } catch (InterruptedException e) {
            System.err.println("Threads were interrupted previously: " + e.getMessage());
//            e.printStackTrace();
        } catch (UnknownHostException e) {
            System.err.println("No IP address for the host could be found, or if a scope_id was specified for a global IPv6 address: " + e.getMessage());
//            e.printStackTrace();
        }
    }

    private boolean isExpectedResponce(String request, String responseStr) {
        for (String s : HELLO_ON_DIFFERENT_LANGUAGES) {
            s = String.format(s, request);
            if (s.equals(responseStr)) {
                return true;
            }
        }
        return false;
    }
}

//java -ea -classpath "D:\JavaAdvanced\out\production\JavaAdvanced;D:\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\JavaAdvanced\java-advanced-2017\artifacts\HelloUDPTest.jar" ru.ifmo.ctddev.turaev.hello.HelloUDPClient localhost 12345 SonyaDura 10 10
//java -ea -classpath "D:\JavaAdvanced\out\production\JavaAdvanced;D:\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\JavaAdvanced\java-advanced-2017\artifacts\HelloUDPTest.jar" info.kgeorgiy.java.advanced.hello.Tester client ru.ifmo.ctddev.turaev.hello.HelloUDPClient

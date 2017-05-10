package ru.ifmo.ctddev.turaev.hello;


import java.net.DatagramSocket;
import java.net.SocketException;

import static java.lang.Integer.max;
import static java.lang.Math.min;
import static java.lang.System.exit;

/**
 * Helper utilities for internal work
 */
class Utils {
    /**
     * Creates int[] and fills with numbers, parsed f
     * rom given String[]
     *
     * @param indexes        contains indexes of numbers in String[] being parsed
     * @param args           array, that contains data. Only indexes from <code>indexes</code> are
     * @param expectedLength expected length of arguments
     * @return int[] of <code>indexes.length</code> numbers, containing number parsed from <code>args</code>
     */
    static int[] checkArguments(int[] indexes, String[] args, int expectedLength) {
        if (args.length < expectedLength) {
            System.err.println("Not enough arguments for running client");
            exit(1);
        }
        int res[] = new int[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            try {
                res[i] = Integer.parseInt(args[index]);
            } catch (NumberFormatException e) {
                System.err.println("Argument #" + index + ": " + args[index] + " expected to be Integer");
                exit(1);
            }
        }
        return res;
    }

    static class TimeManager {
        private static final int MINIMAL_CONNECTION_TIMEOUT = 100;
        private static final int MAXIMAL_CONNECTION_TIMEOUT = 1_000_000;
        private static final int BORDER_VALUE = 5;

        final DatagramSocket socket;
        int successCounter;

        TimeManager(DatagramSocket socket) throws SocketException {
            this.socket = socket;
            successCounter = 0;
            socket.setSoTimeout(10 * MINIMAL_CONNECTION_TIMEOUT);
        }

        void update(int delta) throws SocketException {
            int soTimeout = socket.getSoTimeout();
            if (delta == 0 || (delta == 1 && soTimeout <= MINIMAL_CONNECTION_TIMEOUT) || (delta == -1 && soTimeout >= MAXIMAL_CONNECTION_TIMEOUT)) {
                return;
            }
            successCounter = delta < 0 ? min(delta, successCounter + delta) : max(delta, successCounter + delta);
//            System.out.println(successCounter);
            if (successCounter >= BORDER_VALUE && soTimeout > MINIMAL_CONNECTION_TIMEOUT) {
                successCounter = 0;
                soTimeout >>= 1;
                socket.setSoTimeout(soTimeout);
                System.out.println("New timeout : " + soTimeout);
            } else if (successCounter <= -BORDER_VALUE && soTimeout < MAXIMAL_CONNECTION_TIMEOUT) {
                successCounter = 0;
                soTimeout <<= 1;
                socket.setSoTimeout(soTimeout);
                System.out.println("New timeout : " + soTimeout);
            }
        }
    }
}

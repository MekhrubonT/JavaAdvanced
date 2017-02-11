package ru.ifmo.ctddev.turaev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    public static void main(String[] args) {
        try {
            FileInputStream fileInputStream = new FileInputStream(args[0]);
            FileOutputStream fileOutputStream = new FileOutputStream(args[1]);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8))) {
                new RecursiveWalk().run(in, out);
            }
        } catch (Exception ignored) {
        }

    }

    private void run(BufferedReader in, PrintWriter out) throws IOException {
        String name;
        MyFileVisitor visitor = new MyFileVisitor(out);
        while ((name = in.readLine()) != null) {
            Path path = Paths.get(name);
            Files.walkFileTree(path, visitor);
        }
    }
}

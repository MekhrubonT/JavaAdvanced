package ru.ifmo.ctddev.turaev.walk;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("Слишком много аргументов командной строки: " + args.length);
        }
            try {
            FileInputStream fileInputStream = new FileInputStream(args[0]);

            try {
                Path path = Paths.get(args[1]);
                if (Files.notExists(path)) {
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }
                    Files.createFile(path);
                }
            } catch (IOException e) {
                throw new IOException("Не могу создать выходной файл");
            }

            FileOutputStream fileOutputStream = new FileOutputStream(args[1]);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8))) {
                new RecursiveWalk().run(in, out);
                if (out.checkError()) {
                    System.out.println("Произошла ошибка при использовании PrintWritter-а");
                }
            }
        } catch (IOException | InvalidPathException e) {
            System.out.println(e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Недостаточно аргументов командной строки: " + args.length);
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

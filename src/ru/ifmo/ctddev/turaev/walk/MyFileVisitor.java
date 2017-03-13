package ru.ifmo.ctddev.turaev.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class MyFileVisitor extends SimpleFileVisitor<Path> {
    private static final long FNV_MOD = (1L << 32);
    private static final long FNV_INIT = 2166136261L;
    private static final long FNV_PRIME = 16777619;
    private static final int NEEDED_BITS = (1 << 8) - 1;
    private static final int BUFFER_LENGTH = 4096;
    private PrintWriter out;

    MyFileVisitor(PrintWriter out) {
        this.out = out;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
        try {
            long hashSum = FNV_INIT;
            InputStream input = Files.newInputStream(path);
            byte[] b = new byte[BUFFER_LENGTH];
            int sz;
            while ((sz = input.read(b, 0, b.length)) >= 0) {
                for (int i = 0; i < sz; i++) {
                    hashSum = (hashSum * FNV_PRIME) % FNV_MOD ^ (b[i] & 0xff);
                }
            }
            out.format("%08x %s\n", hashSum, path);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage() + " (Не удается найти указанный файл)");
        } catch (IOException e) {
            System.out.println(e.getMessage() + " (Ошибка чтения из файла)");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        out.format("%08x %s\n", 0, path.toString());
        System.out.println(e.getMessage() + " (Не удается найти указанный путь)");
        return FileVisitResult.CONTINUE;
    }
}

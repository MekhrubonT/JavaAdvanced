package ru.ifmo.ctddev.turaev.walk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Created by Lenovo on 11.02.2017.
 */
public class MyFileVisitor extends SimpleFileVisitor<Path> {
    private final long FNV_MOD = (1L << 32);
    private final long FNV_INIT = 2166136261L;
    private final long FNV_PRIME = 16777619;
    private final int NEEDED_BITS = (1 << 8) - 1;
    private final int BUFFER_LENGTH = 4096;
    private PrintWriter out;

    MyFileVisitor(PrintWriter out) {
        this.out = out;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        long hashSum = FNV_INIT;
        InputStream input = new FileInputStream(path.toFile());
        byte[] b = new byte[BUFFER_LENGTH];
        int sz = 0;
        while ((sz = input.read(b, 0, b.length)) >= 0) {
            for (int i = 0; i < sz; i++) {
                hashSum = (hashSum * FNV_PRIME) % FNV_MOD ^ ((long) b[i] & NEEDED_BITS);
            }
        }

        out.format("%08x %s\n", hashSum, path.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        out.format("%08x %s\n", 0, path.getFileName());
        return FileVisitResult.CONTINUE;
    }
}

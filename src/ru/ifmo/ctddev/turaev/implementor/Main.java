package ru.ifmo.ctddev.turaev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.InterfaceImplementorTest;

import java.nio.file.Path;
import java.nio.file.Paths;

//import static info.kgeorgiy.java.advanced.implementor.InterfaceImplementorTest.check;
//import static info.kgeorgiy.java.advanced.implementor.InterfaceImplementorTest.getClassLoader;

//import static info.kgeorgiy.java.advanced.implementor.InterfaceImplementorTest.getClassLoader;

public class Main {
    public static void main(String[] args) throws ImplerException {


        Class c = ru.ifmo.ctddev.turaev.implementor.Implementor.class;
        String path = "test15_encoding\\info.kgeorgiy.java.advanced.implementor.examples.Arabic.jr";
        new Implementor().implementJar(c, Paths.get(path));
        final Path jarFile = Paths.get(path).resolve(c.getName() + ".class");
        System.out.println(jarFile);
//        check(getClassLoader(jarFile), c);
//                Implementor.main(new String[]{"-jar", "javax.management.relation.RelationNotFoundException", "test10_utilityClasses"});

    }


}

/*
D:\java\JavaAdvanced\test03_standardInterfaces\javax\annotation\GeneratedImpl.java:3: error: GeneratedImpl is not abstract and does not override abstract method annotationType() in Annotation
public class GeneratedImpl implements Generated {
       ^
1 error
*/
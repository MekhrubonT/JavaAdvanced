package ru.ifmo.ctddev.turaev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;


/**
 * Implenting classs for interfaces {@link Impler} and {@link JarImpler}
 */

public class Implementor implements Impler, JarImpler {
    /**
     * indent for generated classes
     */
    private final static String tab = "\t";

    /**
     * Template for methods and constructors arguments
     */
    private final static String varNames = "var";

    /**
     * Expansion for java sources files
     */
    private final static String DOTJAVA = ".java";

    /**
     * Expansion for java compilled files
     */
    private final static String DOTCLASS = ".class";

    /**
     * The name of generated class
     */
    private String outputClassName;

    /**
     * The class or interface to be implemented
     */
    private Class<?> aClass;

    /**
     * The writer used to print the class into destination file
     */
    private Writer pw;


    /**
     * Runs Class-Implementor (method implement) or Creates Jar(method jarImplement)
     *
     * @param args The array of arguments for Implementor.
     *             There are 2 variants of running the main:
     *             <ul>
     *             <li> -jar fullClassName generatedFilesLocation </li>
     *             <li> fullClassName generatedFilesLocation </li>
     *             </ul>
     */
    public static void main(String[] args) {
        try {
            if (args[0].equals("-jar")) {
                new Implementor().implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (ImplerException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("No such class: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Not enough main() arguments");
        }
    }


    /**
     * This method gets the existing Class/Interface and implements/expands it.
     * The generated class will have suffix Impl.
     *
     * @param aClass - Class or Interface to implement
     * @param path   - The location of generated class
     * @throws ImplerException {@link ImplerException} if the given class cannot be generated.
     *                         <ul>
     *                         <li> If the given aClass is not class of interface </li>
     *                         <li> The aClass is final </li>
     *                         <li> The process is not allowed to create files or directories </li>
     *                         <li> Interface contains only private constructors </li>
     *                         <li> The problems with I/O </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (aClass.isPrimitive() || aClass.isArray() || aClass == Enum.class) {
            throw new ImplerException("The given Class<> should be class or interface");
        }
        if (Modifier.isFinal(aClass.getModifiers())) {
            throw new ImplerException("Cannot implement final class");
        }
        this.aClass = aClass;
        outputClassName = aClass.getSimpleName() + "Impl";
        try (PrintWriter temp = new PrintWriter(new File(
                createDirectory(path, aClass, DOTJAVA).toString()))) {
            pw = temp;
            printPackage();
            printTitle();
            printConstructors();
            printMethods();
            pw.write("}\n");
        } catch (IOException e) {
            throw new ImplerException(e);
        }

    }


    /**
     * Generates the string <tt>"return x;"</tt>, where x is 0, false, null or empty string.
     * Chooses the variant to fit returning type of method.
     *
     * @param c The returning type of method.
     * @return Generated string
     */
    private String returnImpl(Class<?> c) {
        StringBuilder t = new StringBuilder(tab + tab + "return");
        if (!c.isPrimitive()) {
            t.append(" null");
        } else if (c.equals(boolean.class)) {
            t.append(" false");
        } else if (!c.equals(void.class)) {
            t.append(" 0");
        }
        return t.append(";").toString();
    }

    /**
     * Implements all methods from class or interface aClass and public and protected methods of its superclasses
     * @throws IOException if an error occured while writing to the destination file via {@link Writer}
     */
    private void printMethods() throws IOException {
        Set<MethodWrapper> methods = new HashSet<>();
        for (Method method : aClass.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                methods.add(new MethodWrapper(method));
            }
        }

        while (aClass != null) {
            for (Method method : aClass.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    methods.add(new MethodWrapper(method));
                }
            }
            aClass = aClass.getSuperclass();
        }

        for (MethodWrapper method : methods) {
            pw.write(method + "\n");
        }
    }

    /**
     * Generates the string, denoting the argument-list of method or constructor
     * Arguments are named varName + i, where i is ordinal number of argument.
     *
     * @param argList The array of types of arguments
     * @return The string, denoting arguments list.
     */
    private String printArgList(Class<?> argList[]) {
        StringBuilder t = new StringBuilder("(");
        for (int i = 0; i < argList.length; i++) {
            t.append(argList[i].getTypeName()).append(" ").append(varNames).append(i);
            if (i != argList.length - 1) {
                t.append(", ");
            }
        }
        t.append(") ");
        return t.toString();
    }

    /**
     * Implements all constructors of the class/interface aClass.
     *
     * @throws ImplerException If aClass is class with no public constructors.
     * @throws IOException if an error occured while writing to the destination file via {@link Writer}
     */
    private void printConstructors() throws ImplerException, IOException {
        boolean flag = true;
        for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                flag = false;
                pw.write(tab + printModifier(aClass.getModifiers()));
                pw.write(outputClassName);
                pw.write(printArgList(constructor.getParameterTypes()));
                pw.write(exceptionsList(constructor.getExceptionTypes()));

                pw.write("{\n" + tab + tab + "super(");
                for (int i = 0; i < constructor.getParameterCount(); i++) {
                    pw.write(varNames + i);
                    if (i != constructor.getParameterCount() - 1) {
                        pw.write(", ");
                    }
                }
                pw.write(");\n" + tab + "}\n\n");
            }
        }
        if (!aClass.isInterface() && flag) {
            throw new ImplerException("Only private constructors");
        }
    }


    /**
     * * Generates the string of next pattern:
     * <tt> throws a, b, c, ... </tt> where a, b, c are some types.
     * This method generates exception for constructors by its exceptions array.
     *
     * @param exceptionTypes - The array of type of exceptions, thrown by constructor
     * @return Generated String
     */
    private String exceptionsList(Class<?>[] exceptionTypes) {
        if (exceptionTypes.length == 0) {
            return "";
        }
        StringBuilder t = new StringBuilder("throws ");
        for (int i = 0; i < exceptionTypes.length; i++) {
            t.append(exceptionTypes[i].getTypeName());
            if (i != exceptionTypes.length - 1) {
                t.append(",");
            }
            t.append(" ");
        }
        return t.toString();
    }

    /**
     * By given Class object creates directory for its java source or object file.
     * It means that directories for packages are also created. If
     * directories exist, nothing happens. Then returns the relative path
     * to sought-for file.
     *
     * @param path   The location, where packages directories are created.
     * @param c      The class object containing its packages data
     * @param suffix File expansion. java-source or java-object file.
     * @return The relative path to given class file.
     * @throws IOException Directories are created using {@link Files#createDirectories(Path, FileAttribute[])}
     *                     and all its exceptions are thrown by this method
     */
    private Path createDirectory(Path path, Class<?> c, String suffix) throws IOException {
        if (c.getPackage() != null) {
            path = path.resolve(c.getPackage().getName().replace(".", "\\"));
        }
        Files.createDirectories(path);
        return path.resolve(outputClassName + suffix);
    }

    /**
     * If the class aClass isn't located in default package, prints concatanation of string "package " and given class package name.
     * @throws IOException if an error occured while writing to the destination file via {@link Writer}
     */
    private void printPackage() throws IOException {
        if (aClass.getPackage() != null) {
            pw.write("package " + aClass.getPackage().getName() + ";\n\n");
        }
    }


    /**
     * Prints header of generated aClass.
     * @throws IOException if an error occured while writing to the destination file via {@link Writer}
     */
    private void printTitle() throws IOException {
        pw.write(printModifier(aClass.getModifiers()));
        pw.write("class ");
        pw.write(outputClassName + " ");
        pw.write(aClass.isInterface() ? "implements " : "extends ");
        pw.write(aClass.getSimpleName() + " {\n");
    }

    /**
     * Generate string containig modifiers divided with space from the given int except {@link Modifier#ABSTRACT},
     * {@link Modifier#TRANSIENT}, {@link Modifier#INTERFACE}. Uses {@link Modifier} to convert to String.
     * The mask values representing the modifiers is described there.
     *
     * @param modifiers the byte mask of the modifiers
     * @return The string generated from given int.
     */
    private String printModifier(int modifiers) {
        return Modifier.toString(modifiers & ~(Modifier.ABSTRACT | Modifier.TRANSIENT |
                Modifier.INTERFACE)) + " ";
    }


    /**
     * Implements the given class and creates Jar-Archive with resulting class.
     * @param cls the given class
     * @param path destination of Jar-Archive
     * @throws ImplerException <ul>
     *     <li> I/O Exceptions while creating, reading or deleting files and directories </li>
     *     <li> Exceptions thrown by {@link Implementor#implement(Class, Path)}</li>
     * </ul>
     * @throws NullPointerException {@link JavaCompiler#run} If JRE used instead of JDK
     * @see Implementor#implement(Class, Path)
     */
    public void implementJar(Class<?> cls, Path path) throws ImplerException {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Path temporaryDir = Paths.get(System.getProperty("user.dir")).resolve("tmp");
            Path filePath = temporaryDir.relativize(createAndCompile(cls, temporaryDir));
            createJarFile(temporaryDir, filePath, path);
            clean(temporaryDir);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Remove the given file/directory and everything in it
     * @param root the path to the given file/directory
     * @throws IOException If error occurs while deleting files.
     * @throws SecurityException  If the security manager denies access to the starting file. In the case of the
     * default provider, the checkRead method is invoked to check read access to the directory.
     */
    private void clean(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path createAndCompile(Class<?> aClass, Path path) throws ImplerException, IOException {
        implement(aClass, path);
        JavaCompiler t = ToolProvider.getSystemJavaCompiler();

        if (t.run(null, null, null,
                createDirectory(path, aClass, DOTJAVA).toString(), "-cp",
                aClass.getPackage().getName() + File.pathSeparator
                        + System.getProperty("java.class.path")) != 0) {
            throw new ImplerException("Can't compile the given class");
        }
        return createDirectory(path, aClass, DOTCLASS);
    }

    private void createJarFile(Path dirs, Path pathFile, Path path) throws ImplerException, IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            out.putNextEntry(new ZipEntry(pathFile.toString().replace(File.separator, "/")));
            Files.copy(dirs.resolve(pathFile), out);
        }
    }

    class MethodWrapper {
        private final Method method;
        private int hash;

        MethodWrapper(Method method) {
            this.method = method;
            hash = (method.getName() + printArgList(method.getParameterTypes())).hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof MethodWrapper)) {
                return false;
            }
            MethodWrapper temp = (MethodWrapper) o;
            return temp.method.getName().equals(method.getName()) &&
                    Arrays.equals(temp.method.getParameterTypes(), method.getParameterTypes());
        }


        @Override
        public String toString() {
            return tab + printModifier(method.getModifiers())
                    + method.getReturnType().getTypeName() + " "
                    + method.getName()
                    + printArgList(method.getParameterTypes()) + "{\n"
                    + returnImpl(method.getReturnType()) + "\n"
                    + tab + "}\n";
        }
    }
}


//"C:\Program Files\Java\jdk1.8.0_60\bin\java" -ea -Dfile.encoding=UTF-8 -cp "D:\java\JavaAdvanced\out\production\HW1;D:\java\JavaAdvanced\java-advanced-2017\lib\hamcrest-core-1.3.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\jsoup-1.8.1.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\junit-4.11.jar;D:\java\JavaAdvanced\java-advanced-2017\lib\quickcheck-0.6.jar;D:\java\JavaAdvanced\java-advanced-2017\artifacts\JarImplementorTest.jar" info.kgeorgiy.java.advanced.implementor.Tester jar-class ru.ifmo.ctddev.turaev.implementor.Implementor
//D:\java\JavaAdvanced\java-advanced-2017\artifacts\JarImplementorTest.jar
//D:\java\JavaAdvanced\java-advanced-2017\artifacts\JarImplementorTest.jar
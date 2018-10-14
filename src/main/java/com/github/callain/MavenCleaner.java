package com.github.callain;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class MavenCleaner {

    private static final boolean VERBOSE_MODE = false;

    private static Set<Path> corruptedDependencies = new HashSet<>();
    private static Set<Path> withoutSignatureDependencies = new HashSet<>();

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            System.out.println("Usage: java -jar mavencleaner.jar PATH_TO_YOUR_MAVEN_REPOSITORY");
        }

        Path dir = Paths.get(args[0]);

        recursiveFind(dir);

        if (corruptedDependencies.size() != 0) {
            System.out.println("Corrupted dependencies :");
            corruptedDependencies.forEach(p -> System.out.println("\t - " + p));
        } else {
            System.out.println("No corrupted dependencies found");
        }

        if( withoutSignatureDependencies.size() != 0 ) {
            System.out.println("Dependencies without signatures :");
            corruptedDependencies.forEach(p -> System.out.println("\t - " + p));
        } else {
            System.out.println("No dependencies without signatures found");
        }
    }

    /**
     * Browse a directory recursively
     * @param dir the directory
     */
    public static void recursiveFind(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (VERBOSE_MODE) {
                    System.out.println(path);
                }

                boolean isDirectory = Files.isDirectory(path);
                boolean isJAR = path.toFile().getName().endsWith(".jar");
                boolean isPOM = path.toFile().getName().endsWith(".pom");

                if (isDirectory) {
                    recursiveFind(path);
                } else if (isJAR || isPOM) {
                    boolean hasSHA1 = checkSHA1(path);
                    boolean hasMD5 = checkMD5(path);
                    if (!hasSHA1 && !hasMD5) {
                        if( VERBOSE_MODE ) {
                            System.out.println("Path " + path + " has no signature files");
                        }

                        withoutSignatureDependencies.add(path.getParent());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the first word of the file which sould be the SHA1 checksum
     *
     * @param file a .sha1 file
     * @return the SHA1 checksum
     * @throws FileNotFoundException if the file is not found
     */
    private static String readSHA1(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file);
        String sha1 = sc.next();
        sc.close();
        return sha1;
    }

    /**
     * Generate a SHA1
     * @param path path of the file
     * @return the SHA1
     */
    public static String generateSHA1(Path path) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(path.toFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] dataBytes = new byte[1024];

        int nread = 0;

        try {
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ;

        byte[] mdbytes = md.digest();

        // Convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        String sha1 = sb.toString();
        return sha1;
    }

    /**
     * Check the SHA1 of a file
     * @param path path of a dependency
     * @return true if the dependency has a SHA1 file, false otherwise
     */
    public static boolean checkSHA1(Path path) {
        String readedSHA1 = null;
        String generatedSHA1 = null;

        // Get dependency SHA1 file
        File jarSha1File = new File(path.toString() + ".sha1");

        // Read SHA1
        try {
            readedSHA1 = readSHA1(jarSha1File);
        } catch (FileNotFoundException e) {
            return false;
        }

        // Generate SHA1
        generatedSHA1 = generateSHA1(path);

        // Compare SHA1
        if (readedSHA1 != null && generatedSHA1 != null) {
            if (!readedSHA1.equals(generatedSHA1)) {
                if( VERBOSE_MODE ) {
                    System.out.println(path + " : " + path);
                    System.out.println("SHA1 readed    = " + readedSHA1);
                    System.out.println("SHA1 generated = " + generatedSHA1);
                    System.out.println("KO");
                }

                corruptedDependencies.add(path.getParent());
            }
        }

        return true;
    }

    /**
     * Check the MD5 of a file
     * @param path path of a dependency
     * @return true if the dependency has a MD5 file, false otherwise
     */
    public static boolean checkMD5(Path path) {
        String readedMD5 = null;
        String generatedMD5 = null;

        // Get dependency MD5 file
        File jarMD5File = new File(path.toString() + ".md5");

        // Read MD5
        try {
            readedMD5 = readMD5(jarMD5File);
        } catch (FileNotFoundException e) {
            return false;
        }

        // Generate MD5
        generatedMD5 = generateMD5(path);

        // Compare MD5
        if (readedMD5 != null && generatedMD5 != null) {
            boolean equals = readedMD5.equals(generatedMD5);
            if (!equals) {
                if( VERBOSE_MODE ) {
                    System.out.println("Path : " + path);
                    System.out.println("MD5 readed    = " + readedMD5);
                    System.out.println("MD5 generated = " + generatedMD5);
                    System.out.println("KO");
                }

                corruptedDependencies.add(path.getParent());
            }
        }

        return true;
    }

    /**
     * Generate a MD5
     * @param path path of the file
     * @return the MD5
     */
    private static String generateMD5(Path path) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(path.toFile()), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * Read a file containing a MD5
     * @param file the file to read
     * @return the MD5 readed
     * @throws FileNotFoundException if the file is not found
     */
    private static String readMD5(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file);
        String sha1 = sc.next();
        sc.close();
        return sha1;
    }
}
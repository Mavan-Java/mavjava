package com.github.callain;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) {

		Path dir = Paths.get("/Users/dalc/.m2/repository");

		recursiveFind(dir);
	}

	public static void recursiveFind(Path dir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
				boolean isDirectory = Files.isDirectory(path);
				boolean isJAR = path.toFile().getName().endsWith(".jar");
				boolean isPOM = path.toFile().getName().endsWith(".pom");

				if (isDirectory) {
					recursiveFind(path);
				} else if (isJAR || isPOM ) {
					checkSHA1(path);
					checkMD5(path);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read the first word of the file which sould be the SHA1 checksum
	 * @param file
	 * @return the SHA1 checksum
	 * @throws FileNotFoundException
	 */
	private static String readSHA1(File file) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		String sha1 = sc.next();
		sc.close();
		return sha1;
	}

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
	 * 
	 * @param path
	 */
	public static void checkSHA1(Path path) {
		String readedSHA1 = null;
		String generatedSHA1 = null;

		File jarSha1File = new File(path.toString() + ".sha1");
		
		try {
			readedSHA1 = readSHA1(jarSha1File);
		} catch (FileNotFoundException e) {
			//System.out.println("[WARN] Missing SHA1 for " + path);
		}
		generatedSHA1 = generateSHA1(path);

		if (readedSHA1 != null && generatedSHA1 != null) {
			boolean equals = readedSHA1.equals(generatedSHA1);
			if (!equals) {
				System.out.println(path + " : \nSHA1 readed    = " + readedSHA1 + " \nSHA1 generated = " + generatedSHA1 + " \n" + (equals ? "OK" : "KO"));
			}
		}
	}
	
	public static void checkMD5(Path path) {
		String readedMD5 = null;
		String generatedMD5 = null;

		File jarSha1File = new File(path.toString() + ".sha1");
		
		try {
			readedMD5 = readMD5(jarSha1File);
		} catch (FileNotFoundException e) {
			//System.out.println("[WARN] Missing SHA1 for " + path);
		}
		generatedMD5 = generateMD5(path);

		if (readedMD5 != null && generatedMD5 != null) {
			boolean equals = readedMD5.equals(generatedMD5);
			if (!equals) {
				System.out.println(path + " : \nSHA1 readed    = " + readedMD5 + " \nSHA1 generated = " + generatedMD5 + " \n" + (equals ? "OK" : "KO"));
			}
		}
	}

	private static String generateMD5(Path path) {
		// TODO Auto-generated method stub
		return null;
	}

	private static String readMD5(File file) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		String sha1 = sc.next();
		sc.close();
		return sha1;
	}
}
package com.example.demo;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileMatchMaker {

    public static void areFilesIdentical(String file1Path, String file2Path){
        File file1 = new File(file1Path);
        File file2 = new File(file2Path);

        try {
            if (areFilesIdenticalTwins(file1, file2)) {
                celebrateMatch(file1, file2);
            } else {
                annouceMismatch(file1, file2);
            }
        } catch (Exception e) {
            System.out.println("Oops! Something went wrong: " + e.getMessage());
        }
    }

    public static boolean areFilesIdenticalTwins(File file1, File file2) throws IOException {
        // Quick size check
        if (file1.length() != file2.length()) {
            return false;
        }

        // Cryptographic hash comparison for fun and efficiency
        try {
            String hash1 = calculateFileChecksum(file1);
            String hash2 = calculateFileChecksum(file2);
            return hash1.equals(hash2);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to byte-by-byte comparison
            return compareFileContentsManually(file1, file2);
        }
    }

    private static String calculateFileChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();

        // Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private static boolean compareFileContentsManually(File file1, File file2) throws IOException {
        try (
                BufferedReader reader1 = new BufferedReader(new FileReader(file1));
                BufferedReader reader2 = new BufferedReader(new FileReader(file2))
        ) {
            String line1, line2;
            int lineNumber = 0;

            while ((line1 = reader1.readLine()) != null &&
                    (line2 = reader2.readLine()) != null) {
                lineNumber++;

                if (!line1.equals(line2)) {
                    System.out.println("🕵️ Mismatch detected at line " + lineNumber + "!");
                    return false;
                }
            }

            // Check if one file has more lines than the other
            return reader1.readLine() == null && reader2.readLine() == null;
        }
    }

    private static void celebrateMatch(File file1, File file2) {
        System.out.println("🎉 EUREKA! Files are IDENTICAL TWINS! 🎊");
        System.out.println("🔍 Forensic Details:");
        System.out.println("   File 1: " + file1.getName() + " (Size: " + file1.length() + " bytes)");
        System.out.println("   File 2: " + file2.getName() + " (Size: " + file2.length() + " bytes)");
        System.out.println("🕺 Let's party! The files are exactly the same! 💃");
    }

    private static void annouceMismatch(File file1, File file2) {
        System.out.println("❌ FILES ARE NOT IDENTICAL! ❌");
        System.out.println("🕵️ Detective Report:");
        System.out.println("   File 1: " + file1.getName() + " (Size: " + file1.length() + " bytes)");
        System.out.println("   File 2: " + file2.getName() + " (Size: " + file2.length() + " bytes)");
        System.out.println("🤔 These files are more like distant cousins!");
    }
}

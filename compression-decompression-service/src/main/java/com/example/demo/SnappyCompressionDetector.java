// Copyright (c) 2025 Cloudera, Inc. All rights reserved.
package com.example.demo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnappyCompressionDetector {
  //private static final Logger LOG = LoggerFactory.getLogger(SnappyCompressionDetector.class);

  // Standard Snappy framing format magic bytes
  private static final byte[] SNAPPY_MAGIC = {(byte) 0x82, 'S', 'N', 'A', 'P', 'P', 'Y'};

  // Hadoop Snappy codec may use this header
  private static final byte[] HADOOP_SNAPPY_MAGIC = {(byte) 0x73, (byte) 0x4E, (byte) 0x61, (byte) 0x50, (byte) 0x70, (byte) 0x59};

  // Potential magic bytes for �SNAPPY header (� is Unicode replacement character 0xEF 0xBF 0xBD)
  private static final byte[] EFBFBD_SNAPPY_MAGIC = {(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, 'S', 'N', 'A', 'P', 'P', 'Y'};

  // ASCII SNAPPY without any special prefix (as seen in some files)
  private static final byte[] ASCII_SNAPPY = {'S', 'N', 'A', 'P', 'P', 'Y'};

  // Other potential BOM markers followed by SNAPPY
  private static final byte[] UTF8_BOM_SNAPPY = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'S', 'N', 'A', 'P', 'P', 'Y'};
  private static final byte[] UTF16LE_BOM_SNAPPY = {(byte) 0xFF, (byte) 0xFE, 'S', 'N', 'A', 'P', 'P', 'Y'};
  private static final byte[] UTF16BE_BOM_SNAPPY = {(byte) 0xFE, (byte) 0xFF, 'S', 'N', 'A', 'P', 'P', 'Y'};

  /**
   * Detects if a file is compressed with Snappy, handling multiple Snappy formats
   * including standard Snappy, Hadoop Snappy, and various custom headers
   */
  public static boolean isSnappyCompressed(File file) {
    if (!file.exists() || file.length() < 3) {
      System.out.println("File doesn't exist or is too small to be Snappy compressed");
      return false;
    }

    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
      bis.mark(24); // Mark stream position to allow multiple format checks

      // First check standard Snappy framing format
      if (checkStandardSnappyFormat(bis)) {
        System.out.println("File detected as standard Snappy format");
        return true;
      }

      // Reset stream position and try Hadoop Snappy format
      bis.reset();
      if (checkHadoopSnappyFormat(bis)) {
        System.out.println("File detected as Hadoop Snappy format");
        return true;
      }

      // Reset stream position and try �SNAPPY format (Unicode replacement char)
      bis.reset();
      if (checkEfbfbdSnappyFormat(bis)) {
        System.out.println("File detected as �SNAPPY format with Unicode replacement character");
        return true;
      }

      // Reset stream and check for UTF-8 BOM + SNAPPY
      bis.reset();
      if (checkUtf8BomSnappyFormat(bis)) {
        System.out.println("File detected as SNAPPY with UTF-8 BOM");
        return true;
      }

      // Reset stream and check for UTF-16 LE BOM + SNAPPY
      bis.reset();
      if (checkUtf16LeBomSnappyFormat(bis)) {
        System.out.println("File detected as SNAPPY with UTF-16 LE BOM");
        return true;
      }

      // Reset stream and check for UTF-16 BE BOM + SNAPPY
      bis.reset();
      if (checkUtf16BeBomSnappyFormat(bis)) {
        System.out.println("File detected as SNAPPY with UTF-16 BE BOM");
        return true;
      }

      // Reset stream and check for plain ASCII SNAPPY
      bis.reset();
      if (checkAsciiSnappyFormat(bis)) {
        System.out.println("File detected as plain ASCII SNAPPY format");
        return true;
      }

      // If none of the standard headers match, try the heuristic approach
      bis.reset();
      if (checkSnappyHeuristic(bis)) {
        System.out.println("File likely Snappy compressed based on heuristic check");
        return true;
      }

      System.out.println("File does not appear to be Snappy compressed");
      return false;
    } catch (IOException e) {
      System.out.println("IOException occurred while checking if file is Snappy compressed " + e);
      return false;
    }
  }

  /**
   * Checks for standard Snappy framing format with magic header
   */
  private static boolean checkStandardSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[SNAPPY_MAGIC.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == SNAPPY_MAGIC.length && Arrays.equals(header, SNAPPY_MAGIC);
  }

  /**
   * Checks for Hadoop-specific Snappy format
   */
  private static boolean checkHadoopSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[HADOOP_SNAPPY_MAGIC.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == HADOOP_SNAPPY_MAGIC.length && Arrays.equals(header, HADOOP_SNAPPY_MAGIC);
  }

  /**
   * Checks for �SNAPPY format where � is the Unicode replacement character (0xEFBFBD)
   */
  private static boolean checkEfbfbdSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[EFBFBD_SNAPPY_MAGIC.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == EFBFBD_SNAPPY_MAGIC.length && Arrays.equals(header, EFBFBD_SNAPPY_MAGIC);
  }

  /**
   * Checks for UTF-8 BOM followed by SNAPPY
   */
  private static boolean checkUtf8BomSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[UTF8_BOM_SNAPPY.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == UTF8_BOM_SNAPPY.length && Arrays.equals(header, UTF8_BOM_SNAPPY);
  }

  /**
   * Checks for UTF-16 LE BOM followed by SNAPPY
   */
  private static boolean checkUtf16LeBomSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[UTF16LE_BOM_SNAPPY.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == UTF16LE_BOM_SNAPPY.length && Arrays.equals(header, UTF16LE_BOM_SNAPPY);
  }

  /**
   * Checks for UTF-16 BE BOM followed by SNAPPY
   * */
  private static boolean checkUtf16BeBomSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[UTF16BE_BOM_SNAPPY.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == UTF16BE_BOM_SNAPPY.length && Arrays.equals(header, UTF16BE_BOM_SNAPPY);
  }

  /**
   * Checks for plain ASCII SNAPPY without special prefix
   */
  private static boolean checkAsciiSnappyFormat(BufferedInputStream bis) throws IOException {
    byte[] header = new byte[ASCII_SNAPPY.length];
    int bytesRead = bis.read(header, 0, header.length);

    return bytesRead == ASCII_SNAPPY.length && Arrays.equals(header, ASCII_SNAPPY);
  }

  /**
   * Uses heuristics to detect Snappy compression when standard headers aren't present
   * This is needed because Hadoop Snappy implementation doesn't always use consistent headers
   */
  private static boolean checkSnappyHeuristic(BufferedInputStream bis) throws IOException {
    // Read first few bytes
    byte[] buffer = new byte[16];
    int bytesRead = bis.read(buffer, 0, buffer.length);
    if (bytesRead < 8) {
      return false;
    }

    // Check for patterns that indicate Snappy:

    // 1. Look for compressed chunk size pattern
    // Typically first 4 bytes contain a length that should be reasonable
    int chunkSize = ((buffer[0] & 0xFF)) |
        ((buffer[1] & 0xFF) << 8) |
        ((buffer[2] & 0xFF) << 16) |
        ((buffer[3] & 0xFF) << 24);

    // Reasonable chunk size for Snappy (typically < 1MB)
    boolean reasonableChunkSize = chunkSize > 0 && chunkSize < 1024 * 1024;

    // 2. Check for specific bit patterns common in Snappy format
    // This is a simplified approach and may have false positives
    boolean hasSnappyPatterns =
        // Check for some common Snappy block type indicators
        (buffer[4] <= 3 && buffer[4] >= 0) ||
            // Check for compressed chunk type markers
            (buffer[0] == 0 && buffer[1] == 0 && buffer[2] != 0);

    // 3. Search for "SNAPPY" string anywhere in the first bytes
    boolean containsSnappyString = false;
    for (int i = 0; i < buffer.length - 5; i++) {
      if (buffer[i] == 'S' && buffer[i+1] == 'N' && buffer[i+2] == 'A' &&
          buffer[i+3] == 'P' && buffer[i+4] == 'P' && buffer[i+5] == 'Y') {
        containsSnappyString = true;
        break;
      }
    }

    // Combine heuristics
    return reasonableChunkSize && (hasSnappyPatterns || containsSnappyString);
  }
}

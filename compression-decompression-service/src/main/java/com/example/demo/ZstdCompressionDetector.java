// Copyright (c) 2025 Cloudera, Inc. All rights reserved.
package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ZstdCompressionDetector {

  //private static final Logger LOG = LoggerFactory.getLogger(ZstdCompressionDetector.class);

  /**
   * Detects if a file is compressed with Zstd, handling standard format
   */
  public static boolean isZstdCompressedFile(File file) {
    int[] zstdMagicBytes = {0x28, 0xB5, 0x2F, 0xFD}; // Using int to handle unsigned values
    //int[] zstdCustomizedBytes = {0x28, 0xEF, 0xBF, 0xBD};

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] fileMagicBytes = new byte[4];
      int bytesRead = fis.read(fileMagicBytes);

      if (bytesRead == 4) {
        for (int i = 0; i < 4; i++) {
          //System.out.println("fileMagicByte & 0xFF = " + (fileMagicBytes[i] & 0xFF) + " fileMagicByte = " + fileMagicBytes[i] + " zstdMagicByte = " + zstdMagicBytes[i] + " zstdCustomizedByte = " + zstdCustomizedBytes[i]);
          //System.out.println("i = " + i + " Condition-1 : " + ((fileMagicBytes[i] & 0xFF) != zstdMagicBytes[i]) + " Condition-2 : " + ((fileMagicBytes[i] & 0xFF) != zstdCustomizedBytes[i]));
          if ((fileMagicBytes[i] & 0xFF) != zstdMagicBytes[i] ) { //&& (fileMagicBytes[i] & 0xFF) != zstdCustomizedBytes[i]
            return false;
          }
        }
        System.out.println("zstd standard magic bytes matched");
        return true;
      } else {
        return false; // Not enough bytes to check
      }

    } catch (IOException e) {
      System.out.println("Error checking file: "+ e.getMessage());
      return false;
    }
  }

  public static boolean isZstdCompressed(final File file) {
    int[] zstdMagicBytes = {0x28, 0xB5, 0x2F, 0xFD}; // Using int to handle unsigned values
    boolean fileZstdCompressed = false;

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] fileMagicBytes = new byte[4];
      int bytesRead = fis.read(fileMagicBytes);

      if (bytesRead == 4 &&
              (fileMagicBytes[0] & 0xFF) == zstdMagicBytes[0] &&
              (fileMagicBytes[1] & 0xFF) == zstdMagicBytes[1] &&
              (fileMagicBytes[2] & 0xFF) == zstdMagicBytes[2] &&
              (fileMagicBytes[3] & 0xFF) == zstdMagicBytes[3]) {
        System.out.println("zstd standard magic bytes matched");
        fileZstdCompressed = true;
      }
    } catch (IOException e) {
      System.out.println("Error checking file: "+ e.getMessage());
    }
    return fileZstdCompressed;
  }
}

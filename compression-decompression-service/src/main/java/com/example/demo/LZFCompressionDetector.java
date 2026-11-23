// Copyright (c) 2025 Cloudera, Inc. All rights reserved.
package com.example.demo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LZFCompressionDetector {
  //private static final Logger LOG = LoggerFactory.getLogger(LZFCompressionDetector.class);

  /**
   * Detects if a file is compressed with LZF, suitable for Spark event logs
   */
    public static boolean isLZFCompressed(final File file) {
      int[] lzfStandardMagicBytes = {0x5a, 0x56}; //"ZV" in ASCII, Using int to handle unsigned values
      boolean fileLzfCompressed = false;

      try (FileInputStream fis = new FileInputStream(file)) {
        byte[] fileMagicBytes = new byte[2];
        int bytesRead = fis.read(fileMagicBytes);

        if (bytesRead == 2 &&
            (fileMagicBytes[0] & 0xFF) == lzfStandardMagicBytes[0] &&
            (fileMagicBytes[1] & 0xFF) == lzfStandardMagicBytes[1] ) {
          System.out.println("lzf standars frame magic bytes matched!");
          fileLzfCompressed = true;
        }
      } catch (IOException e) {
        System.out.println("Error checking file: "+ e.getMessage());
      }
      return fileLzfCompressed;
    }
}

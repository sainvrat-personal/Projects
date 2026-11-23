package com.example.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
//import net.jpountz.lz4.LZ4FrameInputStream;
import com.ning.compress.lzf.LZFInputStream;
import com.github.luben.zstd.ZstdInputStream;
import org.xerial.snappy.SnappyInputStream;
import net.jpountz.lz4.LZ4BlockInputStream;

public class Decompression {

    public static byte[] decompressZstd(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                ZstdInputStream zis = new ZstdInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            return bos.toByteArray();
        }
    }

    public static byte[] decompressLZ4(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                LZ4BlockInputStream lis = new LZ4BlockInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = lis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            return bos.toByteArray();
        }
    }

    public static byte[] decompressSnappy(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                SnappyInputStream zis = new SnappyInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            return bos.toByteArray();
        }
    }

    public static byte[] decompressLZF(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                LZFInputStream zis = new LZFInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            return bos.toByteArray();
        }
    }

    public static void decompressAndWriteIntoFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            byte[] compressedData = bos.toByteArray();

            byte[] decompressedData;
            if (isZstdCompressedFile(new File(filePath))) {
                System.out.println("file = " + filePath + "is dectected as zstd compressed file");
                decompressedData = decompressZstd(compressedData);
            } else if ((compressedData[0] == 0x4c) && (compressedData[1] == 0x5a) && (compressedData[2] == 0x34) && (compressedData[3] == 0x42)) {
                System.out.println("file = " + filePath + "is dectected as lz4 compressed file");
                decompressedData = decompressLZ4(compressedData);
            }else if (SnappyCompressionDetector.isSnappyCompressed(new File(filePath))) {
                System.out.println("file = " + filePath + "is dectected as snappy compressed file");
                decompressedData = decompressSnappy(compressedData);
            } else if (LZFCompressionDetector.isLZFCompressed(new File(filePath))) { //(compressedData[0] == 0x5a) && (compressedData[1] == 0x56) && (compressedData[2] == 0x00) && (compressedData[3] == 0x00)
                System.out.println("file = " + filePath + "is dectected as lzf compressed file");
                decompressedData = decompressLZF(compressedData);
            } else {
                System.out.println("Unknown compression format.");
                return;
            }
            int idxCompression = filePath.lastIndexOf('/');
            String outputFileName = filePath.substring(idxCompression + 1) + ".txt";
            System.out.println("outfileName = {}" + outputFileName);
            writeByteArrayToStringFile(decompressedData, outputFileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isZstdCompressedFile(File file) {
        boolean isFileZstdCompressed = false;
        try {
            boolean isZstd = ZstdCompressionDetector.isZstdCompressed(file);
            if (isZstd) {
                System.out.println(file.getName() + " is Zstd compressed");
                isFileZstdCompressed = isZstd;
            } else {
                System.out.println(file.getName() + " is not Zstandard (Zstd) compressed");
            }
        } catch (Exception exception) {
            System.out.println("Exception occured while checking file is Zstd compressed, exception = " + exception.getStackTrace().toString());
        }
        return isFileZstdCompressed;
    }

    public static void writeByteArrayToStringFile(byte[] byteArray, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            String stringData = new String(byteArray); // Convert byte array to String
            writer.write(stringData);
            System.out.println("Successfully wrote data to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
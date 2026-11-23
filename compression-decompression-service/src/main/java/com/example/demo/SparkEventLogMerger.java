package com.example.demo;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
//import org.apache.commons.compress.compressors.lzf.LZFInputStream;
//import org.apache.commons.compress.compressors.lzf.LZFOutputStream;
//import org.apache.hadoop.io.compress.SnappyCodec;
//import org.apache.hadoop.io.compress.ZstdCodec;

/*
Key features of this implementation:

        1. Dependencies required:
        ```xml
        <dependencies>
       <!-- LZ4 Compression -->
       <dependency>
           <groupId>org.lz4</groupId>
           <artifactId>lz4-java</artifactId>
           <version>1.8.0</version>
       </dependency>

       <!-- LZF Compression -->
       <dependency>
           <groupId>org.apache.commons</groupId>
           <artifactId>commons-compress</artifactId>
           <version>1.21</version>
       </dependency>

       <!-- Hadoop Compression Codecs -->
       <dependency>
           <groupId>org.apache.hadoop</groupId>
           <artifactId>hadoop-common</artifactId>
           <version>3.3.4</version>
       </dependency>
   </dependencies>
        ```

        2. Supports multiple compression codecs:
        - LZ4
   - LZF
   - Snappy
   - Zstd

3. Key methods:
        - `mergeCompressedEventLogs()`: Main method to merge multiple compressed files
   - `mergeCompressedFile()`: Merges individual compressed files
   - `createCompressedOutputStream()`: Creates compressed output stream
   - `getDecompressedInputStream()`: Decompresses input streams

4. Features:
        - Reads compressed files
   - Decompresses each file
   - Writes to a new compressed output file
   - Optional file deletion after merging
   - Supports different input and output codecs

Example usage in the `main()` method shows how to:
        - Specify input directory
- List event log files to merge
- Specify output file and codec

Notes:
        - Ensure you have the necessary dependencies
- Modify paths and file names as per your specific use case
        - Handle exceptions appropriately

 */
public class SparkEventLogMerger {
    // Codec constants
    private static final String CODEC_LZ4 = "lz4";
    private static final String CODEC_LZF = "lzf";
    private static final String CODEC_SNAPPY = "snappy";
    private static final String CODEC_ZSTD = "zstd";

    /**
     * Merge compressed Spark event log files into a single compressed file
     *
     * @param localEventLogDirectory Directory containing event log files
     * @param outputFile Merged output compressed file
     * @param eventLogFileNames List of event log file names to merge
     * @throws IOException If there's an error during merging
     */
    public static void mergeCompressedEventLogs(
            File localEventLogDirectory,
            File outputFile,
            List<String> eventLogFileNames) throws IOException {

        String inputCodec = getCompressionCodecName(eventLogFileNames.get(0));
        if(inputCodec == null){
            inputCodec = getCompressionCodecName(eventLogFileNames.get(1));
        }

        // Determine output stream based on codec
        try (OutputStream outputStream = createCompressedOutputStream(outputFile, inputCodec)) {
            // Iterate through input files and merge
            for (String fileName : eventLogFileNames) {
                File currentFile = new File(localEventLogDirectory, fileName);
                //inputCodec = getCompressionCodecName(fileName);

                // Merge individual file
                mergeCompressedFile(currentFile, outputStream, inputCodec);

                // Optional: Delete source file after merging
               // System.out.println("File " + currentFile + " deleted post merge :: " + currentFile.delete());
            }
        }
    }

    /**
     * Merge a single compressed file into the output stream
     */
    private static void mergeCompressedFile(
            File currentFile,
            OutputStream outputStream,
            String codecName) throws IOException {

        try (InputStream inputStream = new FileInputStream(currentFile)) {
            // Get decompressed input stream based on codec
            InputStream decompressedStream = getDecompressedInputStream(inputStream, codecName);

            // Buffer for reading
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Read from compressed input and write to output
            while ((bytesRead = decompressedStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }
    }

    /**
     * Create compressed output stream based on codec
     */
    private static OutputStream createCompressedOutputStream(
            File outputFile,
            String codecName) throws IOException {

        FileOutputStream fos = new FileOutputStream(outputFile);

        switch (codecName) {
            case CODEC_LZ4:
                return new LZ4BlockOutputStream(fos);
            case CODEC_LZF:
                return new LZFOutputStream(fos);
            case CODEC_SNAPPY:
                return new SnappyOutputStream(fos); //SnappyCodec().createOutputStream(fos);
            case CODEC_ZSTD:
                return new ZstdOutputStream(fos); //ZstdCodec().createOutputStream(fos);
            default:
                throw new IllegalArgumentException("Unsupported codec: " + codecName);
        }
    }

    /**
     * Get decompressed input stream based on codec
     */
    private static InputStream getDecompressedInputStream(
            InputStream is,
            String codecName) throws IOException {

        switch (codecName) {
            case CODEC_LZ4:
                return new LZ4BlockInputStream(is);
            case CODEC_LZF:
                return new LZFInputStream(is);
            case CODEC_SNAPPY:
                return new SnappyInputStream(is); // SnappyCodec().createInputStream(is);
            case CODEC_ZSTD:
                return new ZstdInputStream(is); // ZstdCodec().createInputStream(is);
            default:
                throw new IllegalArgumentException("Unsupported codec: " + codecName);
        }
    }

    /**
     * Determine compression codec from filename
     */
    private static String getCompressionCodecName(String fileName) {
        if (fileName.endsWith(".lz4")) return CODEC_LZ4;
        if (fileName.endsWith(".lzf")) return CODEC_LZF;
        if (fileName.endsWith(".snappy")) return CODEC_SNAPPY;
        if (fileName.endsWith(".zstd")) return CODEC_ZSTD;
        throw new IllegalArgumentException("Unknown compression codec for file: " + fileName);
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        try {
            File inputDirectory = new File("/path/to/event/logs");
            File outputFile = new File("/path/to/merged/eventlog.lz4");

            List<String> eventLogFiles = Arrays.asList(
                    "eventlog1.lz4",
                    "eventlog2.lz4",
                    "eventlog3.lz4"
            );

            mergeCompressedEventLogs(
                    inputDirectory,
                    outputFile,
                    eventLogFiles
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


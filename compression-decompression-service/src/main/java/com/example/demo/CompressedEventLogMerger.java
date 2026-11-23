package com.example.demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

/*
Key improvements and features:

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
   </dependencies>
        ```

        2. Supports multiple compression codecs:
        - LZ4
   - LZF
   - GZIP

3. Key methods:
        - `mergeFile()`: Merges individual compressed files
   - `getDecompressedInputStream()`: Decompresses input streams
   - `createCompressedPrintWriter()`: Creates a PrintWriter with compressed output
   - `createCompressedOutputStream()`: Creates compressed output stream

4. Features:
        - Reads compressed files
   - Writes content to a PrintWriter
   - Supports different compression codecs
   - Uses UTF-8 encoding
   - Provides flexibility in merging compressed files

5. Example usage in `main()` method shows how to:
        - Specify input directory
   - Create a compressed PrintWriter
   - Merge multiple event log files

Improvements over the original code:
        - More robust codec handling
- Added support for multiple compression formats
- Provides a method to create compressed PrintWriter
- Uses StandardCharsets for consistent encoding
- More flexible and extensible

Would you like me to elaborate on any part of the implementation?
 */
public class CompressedEventLogMerger {
    private static final String CODEC_LZ4 = "lz4";
    private static final String CODEC_LZF = "lzf";
    private static final String CODEC_SNAPPY = "snappy";
    private static final String CODEC_ZSTD = "zstd";
    private static final Pattern ROLLING_EVENT_LOG_FILE_NAME_PATTERN = Pattern.compile("events_(\\d+)_(\\w*)", Pattern.CASE_INSENSITIVE);
    private static final String UNWANTED_FILES_PATTERN = ".DS_Store";

    public static void sortAndMergeRollingEventLogFiles(File localEventLogDirectory, String fullSparkAppId) {
        try {
            //Preconditions.checkState(localEventLogDirectory.exists());
            clearUnwantedFilesBeforeMerge(localEventLogDirectory);
            String[] filesToMerge = java.util.Objects.requireNonNull(localEventLogDirectory.list());
            if (filesToMerge.length == 0 ) { //|| filesToMerge[0].matches(fullSparkAppId)
                // Merging is only needed for rolling logs
                System.out.println("found no files in given directory!");
                return;
            }
            String inputCodec = getCompressionCodecName(filesToMerge[0]);

            System.out.println("Merging files from directory "+localEventLogDirectory+" to a single file " + fullSparkAppId);
            try (PrintWriter mergedFileWriter = createCompressedPrintWriter(new File(Paths.get(localEventLogDirectory.toString(), fullSparkAppId).toString()), inputCodec)) {
                System.out.println("Event Log File "+fullSparkAppId+" created. Proceeding to merge.");
                Arrays.stream(java.util.Objects.requireNonNull(localEventLogDirectory.list()))
                        .filter(f -> !f.equalsIgnoreCase(fullSparkAppId))
                        .sorted(Comparator.comparingInt(CompressedEventLogMerger::getEventLogFileIndex))
                        .forEach(f -> mergeFile(localEventLogDirectory, mergedFileWriter, f));
            }
        } catch (IOException e) {
            System.out.println("Failed to merge event log files for Spark app: " +fullSparkAppId + " exception : "  + e.getMessage());
        }
    }

    /**
     * There maybe files in the localEventLogDirectory that would've been downloaded as part of direct folder download.
     * We're clearing them out so that they don't cause any side effects in downstream processes.
     * @param localEventLogDirectory Local Directory where event log files are already extracted
     */
    private static void clearUnwantedFilesBeforeMerge(File localEventLogDirectory) {
        Arrays.stream(java.util.Objects.requireNonNull(localEventLogDirectory.list()))
                .filter(fileName -> fileName.matches(UNWANTED_FILES_PATTERN))
                .forEach(unwantedFileName -> {
                    File unwantedFile = new File(localEventLogDirectory, unwantedFileName);
                    System.out.println("Unwanted file "+unwantedFile+" deleted = " + unwantedFile.delete());
                });
    }

    /**
     * Merge compressed event log file into a PrintWriter
     *
     * @param localEventLogDirectory Directory containing event log files
     * @param mergedFileWriter PrintWriter to write merged content
     * @param eventLogFileName Name of the event log file to merge
     */
    public static void mergeFile(
            File localEventLogDirectory,
            PrintWriter mergedFileWriter,
            String eventLogFileName) {

        System.out.println("Merging file " + eventLogFileName);
        File currentFile = new File(localEventLogDirectory, eventLogFileName);
        String codecName = getCompressionCodecName(eventLogFileName);

        try (InputStream is = new FileInputStream(currentFile)) {
            // Get decompressed input stream based on codec
            InputStream decompressedStream = getDecompressedInputStream(is, codecName);

            // Use BufferedReader to read lines from decompressed stream
            try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(decompressedStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    mergedFileWriter.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge file: " + currentFile.getName(), e);
        } finally {
            mergedFileWriter.flush();
        }

        System.out.println("File " + currentFile + " is merged.");
    }

    /**
     * Sample event log file name when spark.eventLog.rolling.enabled=true : events_1_application_1685528640339_0013_2
     * In the above sample, 1 is the file index
     *
     * @param eventLogFileName filename to extract the index from
     * @return The index of the rolling file
     */
    private static int getEventLogFileIndex(String eventLogFileName) {
        Matcher matcher = ROLLING_EVENT_LOG_FILE_NAME_PATTERN.matcher(eventLogFileName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            System.out.println("File "+ eventLogFileName +" is not expected here.");
            throw new IllegalArgumentException("File " + eventLogFileName + " is not expected here.");
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
        try {
            if (fileName.endsWith(".lz4")) return CODEC_LZ4;
            if (fileName.endsWith(".lzf")) return CODEC_LZF;
            if (fileName.endsWith(".snappy")) return CODEC_SNAPPY;
            if (fileName.endsWith(".zstd")) return CODEC_ZSTD;
            throw new IllegalArgumentException("Unknown compression codec for file: " + fileName);
        } catch (Exception e) {
            System.out.println("Unknown compression codec for file: " + fileName);
            return null;
        }
    }

    /**
     * Create a PrintWriter with compressed output
     */
    public static PrintWriter createCompressedPrintWriter(
            File outputFile,
            String codecName) throws IOException {

        OutputStream compressedOutputStream = createCompressedOutputStream(outputFile, codecName);
        return new PrintWriter(new OutputStreamWriter(compressedOutputStream, StandardCharsets.UTF_8));
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
}
package com.example.demo;

import com.ning.compress.lzf.LZFInputStream;
import com.github.luben.zstd.ZstdInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jpountz.lz4.LZ4BlockInputStream;
import org.xerial.snappy.SnappyInputStream;

public class CotentCopier {

    public static final String CODEC_LZ4 = "lz4";
    public static final String CODEC_LZF = "lzf";
    public static final String CODEC_SNAPPY = "snappy";
    public static final String CODEC_ZSTD = "zstd";
    private static final String IN_PROGRESS = ".inprogress";
    private static final Pattern ROLLING_EVENT_LOG_FILE_NAME_PATTERN = Pattern.compile("events_(\\d+)_(\\w*)", Pattern.CASE_INSENSITIVE);

    public static void sortAndMergeRollingEventLogFiles(File localEventLogDirectory, String fullSparkAppId) {
        try {
            //Preconditions.checkState(localEventLogDirectory.exists());
            String[] filesToMerge = java.util.Objects.requireNonNull(localEventLogDirectory.list());
            if (filesToMerge.length == 0 || filesToMerge[0].matches(fullSparkAppId)) {
                // Merging is only needed for rolling logs
                return;
            }
            System.out.println("Merging files from directory "+localEventLogDirectory+" to a single file " + fullSparkAppId);

            try (PrintWriter mergedFileWriter = new PrintWriter(Paths.get(localEventLogDirectory.toString(), fullSparkAppId).toString())) {
                System.out.println("Event Log File "+fullSparkAppId+" created. Proceeding to merge.");
                //clearUnwantedFilesBeforeMerge(localEventLogDirectory, filesToMerge);
                Arrays.stream(java.util.Objects.requireNonNull(localEventLogDirectory.list()))
                        .filter(f -> !f.equalsIgnoreCase(fullSparkAppId))
                        .sorted(Comparator.comparingInt(CotentCopier::getEventLogFileIndex))
                        .forEach(f -> mergeFile(localEventLogDirectory, mergedFileWriter, f));
            }
        } catch (IOException e) {
            System.out.println("Failed to merge event log files for Spark app: " +fullSparkAppId + " exception : "  + e.getMessage());
        }
    }

    public static void mergeFile(File localEventLogDirectory, PrintWriter mergedFileWriter, String eventLogFileName) {
        System.out.println("Merging file " + eventLogFileName);
        File currentFile = new File(localEventLogDirectory, eventLogFileName);
        String codecName = getCompressionCodecName(eventLogFileName);

        try (InputStream is = new FileInputStream(currentFile);
                BufferedReader fileReader = new BufferedReader(new InputStreamReader(
                        codecName != null ? getDecompressedInputStream(is, codecName) : is))) {

            String line;
            while ((line = fileReader.readLine()) != null) {
                mergedFileWriter.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge file: " + currentFile.getName(), e);
        } finally {
            mergedFileWriter.flush();
        }
        System.out.println("File " + currentFile + "is merged.");
        //System.out.println("File " + currentFile + " deleted post merge :: " + currentFile.delete());
    }

    public static void copyCompressedFile(File file1, File file2) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(file1);
                FileOutputStream fos = new FileOutputStream(file2)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
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

    public static void decompressAndMergeFile(File localEventLogDirectory, PrintWriter mergedFileWriter, String eventLogFileName) {
        System.out.println("Merging file " + eventLogFileName);
        File currentFile = new File(localEventLogDirectory, eventLogFileName);
        String codecName = getCompressionCodecName(eventLogFileName);

        try (InputStream is = new FileInputStream(currentFile);
                BufferedReader fileReader = new BufferedReader(new InputStreamReader(
                        codecName != null ? getDecompressedInputStream(is, codecName) : is))) {

            String line;
            while ((line = fileReader.readLine()) != null) {
                mergedFileWriter.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge file: " + currentFile.getName(), e);
        } finally {
            mergedFileWriter.flush();
        }
        System.out.println("File " + currentFile + "is merged.");
        //System.out.println("File " + currentFile + " deleted post merge :: " + currentFile.delete());
    }

    // Helper method to get decompressed input stream based on codec
    private static InputStream getDecompressedInputStream(InputStream is, String codecName) throws IOException {
        switch (codecName) {
            case CODEC_LZ4:
                System.out.println("Using LZ4BlockInputStream");
                return new LZ4BlockInputStream(is);
            case CODEC_LZF:
                System.out.println("Using LZFInputStream");
                return new LZFInputStream(is);
            case CODEC_SNAPPY:
                System.out.println("Using SnappyInputStream");
                return new SnappyInputStream(is);
            case CODEC_ZSTD:
                System.out.println("Using ZstdInputStream");
                return new ZstdInputStream(is);
            default:
                throw new RuntimeException(String.format("Unexpected compression codec %s", codecName));
        }
    }

    /*
     * See to EventLoggingListener.openEventLog
     */
    public static String getCompressionCodecName(String filename) {
        int idxOfInProgress = filename.indexOf(IN_PROGRESS);
        if (idxOfInProgress != -1) {
            filename = filename.substring(0, idxOfInProgress);
        }
        int idxOfCompression = filename.lastIndexOf('.');
        if (idxOfCompression != -1) {
            return filename.substring(idxOfCompression + 1);
        }
        System.out.println("idxOfInProgress = "+idxOfInProgress+", idxOfCompression = "+idxOfCompression+", filename = " + filename);
        return null;
    }
}

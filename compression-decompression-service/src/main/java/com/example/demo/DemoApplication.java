package com.example.demo;

import java.io.File;
import org.springframework.boot.SpringApplication;
import com.example.demo.Decompression;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
		System.out.println("Decompression Application Started...");

		String value = "";

		// Replace with your file path
		//String lz4CompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/application_1742381818143_0007_1/application_1742381818143_0007_1";
		//String lzfCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/application_1742381818143_0008_1/application_1742381818143_0008_1";
		//String snappyCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/application_1742381818143_0006_1/application_1742381818143_0006_1";
		//String zstdCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/application_1742381818143_0009_1/application_1742381818143_0009_1";

		String lz4CompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/lz4/application_1742381818143_0007_1.lz4";
		String lzfCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/application_1743141975732_0015_1/application_1743141975732_0015_1"; //lzf/application_1742381818143_0008_1.lzf
		String snappyCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/snappy/application_1742381818143_0006_1.snappy";
		String zstdCompressedFilePath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/zstd/application_1742381818143_0009_1.zstd";

		System.out.println("Decompressing lzf compressed file ...");
		Decompression.decompressAndWriteIntoFile(lzfCompressedFilePath);

		/*System.out.println("Decompressing snappy compressed file ...");
		Decompression.decompressAndWriteIntoFile(snappyCompressedFilePath);

		System.out.println("Decompressing zstd compressed file ...");
		Decompression.decompressAndWriteIntoFile(zstdCompressedFilePath);*/

/*		File localLz4EventLogDirectory = new File("/Users/sainvratm/Downloads/5543-Tp-Payloads/lz4");
		String fullLz4SparkAppId = "application_1742381818143_0007_1";
		System.out.println("Writing lz4 compressed files into another file...");
		CompressedEventLogMerger.sortAndMergeRollingEventLogFiles(localLz4EventLogDirectory, fullLz4SparkAppId);
		String newLz4FileCreatedPath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/lz4/application_1742381818143_0007_1";*/

		//System.out.println("Decompressing lz4 compressed file ...");
		//Decompression.decompressAndWriteIntoFile(lz4CompressedFilePath);

		//System.out.println("Decompressing lz4 compressed file ...");
		//Decompression.decompressAndWriteIntoFile(newLz4FileCreatedPath);

		//FileMatchMaker.areFilesIdentical("application_1742381818143_0007_1.lz4.txt", "application_1742381818143_0007_1.txt");

/*		File localLzfEventLogDirectory = new File("/Users/sainvratm/Downloads/5543-Tp-Payloads/lzf");
		String fullLzfSparkAppId = "application_1742381818143_0008_1";
		System.out.println("Writing lzf compressed file into another file...");
		CotentCopier.sortAndMergeRollingEventLogFiles(localLzfEventLogDirectory, fullLzfSparkAppId);
		String newLzfFileCreatedPath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/lzf/application_1742381818143_0008_1";
		FileMatchMaker.areFilesIdentical(lzfCompressedFilePath, newLzfFileCreatedPath);

		File localSnappyEventLogDirectory = new File("/Users/sainvratm/Downloads/5543-Tp-Payloads/snappy");
		String fullSnappySparkAppId = "application_1742381818143_0006_1";
		System.out.println("Writing snappy compressed file into another file...");
		CotentCopier.sortAndMergeRollingEventLogFiles(localSnappyEventLogDirectory, fullSnappySparkAppId);
		String newSnappyFileCreatedPath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/snappy/application_1742381818143_0006_1";
		FileMatchMaker.areFilesIdentical(snappyCompressedFilePath, newSnappyFileCreatedPath);

		File localZstdEventLogDirectory = new File("/Users/sainvratm/Downloads/5543-Tp-Payloads/zstd");
		String fullZstdSparkAppId = "application_1742381818143_0009_1";
		System.out.println("Writing zstd compressed file into another file...");
		CotentCopier.sortAndMergeRollingEventLogFiles(localZstdEventLogDirectory, fullZstdSparkAppId);
		String newZstdFileCreatedPath = "/Users/sainvratm/Downloads/5543-Tp-Payloads/zstd/application_1742381818143_0009_1";
		FileMatchMaker.areFilesIdentical(zstdCompressedFilePath, newZstdFileCreatedPath);*/
	}

}

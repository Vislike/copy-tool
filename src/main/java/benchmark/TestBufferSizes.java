package benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import copy.RobustCopy;
import copy.meta.FileRecord;
import copy.meta.Settings;
import utils.Utils;

public class TestBufferSizes {

	public static final long SIZE = 1024 * 1024 * 512;

	public static void main(String[] args) throws IOException {
		System.out.println("= = = = Copy Tool Buffer Test = = = =");
		System.out.println();

		if (args.length == 0) {
			System.out.println("Usage: ct-buffer-test *path-to-test-file*");
			return;
		}

		Path file = Paths.get(args[0]);
		Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "ct-buffer-test-temp-file");
		System.out.println("Test file for reading: " + file);
		System.out.println("Temp file for writing: " + tempFile);
		System.out.println("Copy limit: " + Utils.size(SIZE));
		System.out.println();

		long size = Files.size(file);
		if (size < SIZE * 2) {
			System.out.println("Test file must be at least " + Utils.size(SIZE * 2) + " large");
			return;
		}

		Random random = new Random();

		int numBytes = 512;
		// 512 B to 16 MiB
		for (int i = 0; i < 16; i++) {
			// Open files

			long posistion = random.nextLong(size - SIZE);

			testCopy(new FileRecord(file, posistion, SIZE), FileRecord.targetFile(tempFile), numBytes);
			System.out.println(" (Pos: " + Utils.size(posistion) + ")");

			numBytes *= 2;
		}

	}

	private static void testCopy(FileRecord source, FileRecord target, int numBytes) throws IOException {
		long startTime = System.nanoTime();

		RobustCopy robustCopy = new RobustCopy(new Settings(numBytes, 0));
		robustCopy.copy(source, target);

		long elapsedNanos = System.nanoTime() - startTime;
		long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
		String perSec = Utils.size(source.size() * 1000 / ms);
		System.out.print("Buffer: " + Utils.size(numBytes) + ": " + ms + "ms [" + perSec + "/s]");
	}

}

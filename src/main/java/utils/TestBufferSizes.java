package utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import app.Utils;

public class TestBufferSizes {

	public static final long SIZE = 1024 * 1024 * 64;

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
			SeekableByteChannel inChannel = Files.newByteChannel(file, StandardOpenOption.READ);
			SeekableByteChannel outChannel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE);

			long seek = random.nextLong(size - SIZE);
			inChannel.position(seek);

			testCopy(inChannel, outChannel, numBytes);
			System.out.println(" (Pos: " + Utils.size(seek) + ")");

			inChannel.close();
			outChannel.close();

			numBytes *= 2;
		}

	}

	private static void testCopy(SeekableByteChannel inChannel, SeekableByteChannel outChannel, int numBytes)
			throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(numBytes);
		long bytesRead = 0;
		long startTime = System.nanoTime();
		while (bytesRead < SIZE) {
			int read = inChannel.read(bb.clear());
			int write = outChannel.write(bb.flip());
			if (read != write) {
				throw new IOException("Bytes missmatch, read: " + read + ", write: " + write);
			}
			bytesRead += read;
		}
		long elapsedNanos = System.nanoTime() - startTime;
		long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
		String perSec = Utils.size(bytesRead * 1000 / ms);
		System.out.print("Buffer: " + Utils.size(numBytes) + ": " + ms + "ms [" + perSec + "/s]");
	}

}

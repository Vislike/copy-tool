package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TestBufferSizes {

	public static void main(String[] args) {
		Random random = new Random();
		String fileName = "testfile";
		Path targetDir = Paths.get("testdir");
		Path file = Paths.get(fileName);
		try {
			int numBytes = 512;
			for (int i = 0; i < 16; i++) {

				InputStream in = Files.newInputStream(file, StandardOpenOption.READ);
				OutputStream out = Files.newOutputStream(targetDir.resolve(file.getFileName()),
						StandardOpenOption.WRITE, StandardOpenOption.CREATE);
				in.skipNBytes(random.nextLong(1024l * 1024 * 1024 * 10));

				testCopy(in, out, numBytes);
				in.close();
				out.close();
				numBytes *= 2;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void testCopy(InputStream in, OutputStream out, int bytes) throws IOException {
		long startTime = System.nanoTime();
		long bytesRead = 0;
		while (bytesRead < 1024l * 1024 * 50) {
			byte[] bs = in.readNBytes(bytes);
			bytesRead += bs.length;
			out.write(bs);
		}
		long elapsedNanos = System.nanoTime() - startTime;
		System.out.println(bytes + ": " + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + "ms");

	}

}

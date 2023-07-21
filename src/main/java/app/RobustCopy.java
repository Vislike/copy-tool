package app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

public class RobustCopy {

	public static final int WAIT_TIME = 10;

	public static void robustCopy(Path from, Path to, int bufferSize) {
		// Allocate Buffer
		ByteBuffer bb = ByteBuffer.allocate(bufferSize);

		// Get Size of Source
		long size = -1;
		while (size < 0) {
			try {
				size = Files.size(from);
			} catch (IOException e) {
				System.err.println("Error getting size: " + e.getMessage());
				waitBeforeRetry();
			}
		}

		// Create all parent directories of target
		Path targetDirectories = null;
		while (targetDirectories == null) {
			try {
				targetDirectories = Files.createDirectories(to.getParent());
			} catch (IOException e) {
				System.err.println("Error creating parent directories: " + e.getMessage());
				waitBeforeRetry();
			}
		}

		// Print
		System.out.println("Copying " + from + " => " + to + " (" + size + " bytes)");
		long printTime = 0;

		// States
		long bytesCopied = -1;
		SeekableByteChannel inChannel = null;
		SeekableByteChannel outChannel = null;

		// Copy file
		while (bytesCopied < size) {
			try {
				// Open files
				inChannel = Files.newByteChannel(from, StandardOpenOption.READ);
				outChannel = Files.newByteChannel(to, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Rollback last two buffers
				bytesCopied = Math.max(0, bytesCopied - bufferSize * 2);

				// Resume
				inChannel.position(bytesCopied);
				outChannel.position(bytesCopied);
				System.out.println("(Re)Starting at byte: " + bytesCopied);

				while (bytesCopied < size) {
					// Copy chunk
					int numBytes = inChannel.read(bb.clear());
					outChannel.write(bb.flip());
					bytesCopied += numBytes;

					// Print progress
					if (printTime + 9500 < System.currentTimeMillis() || bytesCopied == size) {
						printTime = System.currentTimeMillis();
						System.out.println(bytesCopied + " / " + size + " bytes ("
								+ String.format("%.1f", (double) bytesCopied / (double) size * 100.0) + "%)");
					}
				}
			} catch (IOException e) {
				System.err.println("Copy problem detected: " + e.getMessage());

				if (inChannel != null) {
					try {
						inChannel.close();
					} catch (IOException ce) {
						System.err.println("Error closing inChannel: " + ce.getMessage());
					}
					inChannel = null;
				}

				if (outChannel != null) {
					try {
						outChannel.close();
					} catch (IOException ce) {
						System.err.println("Error closing outChannel: " + ce.getMessage());
					}
					outChannel = null;
				}

				waitBeforeRetry();
			}
		}

		// Set last modified time to the same
		FileTime fileTime = null;
		while (fileTime == null) {
			try {
				fileTime = Files.getLastModifiedTime(from);
			} catch (IOException e) {
				System.err.println("Error getting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
		System.out.println("Setting Last Modified Time to: " + fileTime);
		Path targetTimeModified = null;
		while (targetTimeModified == null) {
			try {
				targetTimeModified = Files.setLastModifiedTime(to, fileTime);
			} catch (IOException e) {
				System.err.println("Error setting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private static void waitBeforeRetry() {
		System.out.println("Waiting " + WAIT_TIME + "s...");
		try {
			Thread.sleep(Duration.ofSeconds(WAIT_TIME));
		} catch (InterruptedException e) {
			System.err.println("Wait Interrupted: " + e.getMessage());
		}
		System.out.println("Retrying...");
	}
}

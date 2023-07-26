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

	private final ByteBuffer bb;
	private final int waitBeforeRetryTimeSec;

	private long progressPrintTime;
	private long progressLastBytes;
	private long progressStartCopyTime;

	public RobustCopy(int bufferSize, int waitBeforeRetryTimeSec) {
		// Allocate Buffer
		this.bb = ByteBuffer.allocate(bufferSize);
		this.waitBeforeRetryTimeSec = waitBeforeRetryTimeSec;
	}

	public void robustCopy(Path from, Path to) {
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

		// Print file to copy
		System.out.println("Copying " + from + " => " + to + " (" + Utils.size(size) + ")");

		// States
		long bytesCopied = -1;
		progressPrintTime = -1;
		progressLastBytes = -1;
		progressStartCopyTime = System.currentTimeMillis();
		SeekableByteChannel inChannel = null;
		SeekableByteChannel outChannel = null;

		// Copy file
		while (bytesCopied < size) {
			try {
				// Open files
				inChannel = Files.newByteChannel(from, StandardOpenOption.READ);
				outChannel = Files.newByteChannel(to, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Rollback last two buffers
				bytesCopied = Math.max(0, bytesCopied - bb.capacity() * 2);

				// Resume
				inChannel.position(bytesCopied);
				outChannel.position(bytesCopied);
				System.out.println("(Re)Starting at: " + Utils.size(bytesCopied));

				while (bytesCopied < size) {
					// Copy chunk
					int numBytes = inChannel.read(bb.clear());
					outChannel.write(bb.flip());
					bytesCopied += numBytes;

					printProgress(bytesCopied, size);
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

		// Get last modified time from source file
		FileTime fileTime = null;
		while (fileTime == null) {
			try {
				fileTime = Files.getLastModifiedTime(from);
			} catch (IOException e) {
				System.err.println("Error getting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}

		// Set target last modified time to the same
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

	private void printProgress(long bytesCopied, long size) {
		// Print progress
		if (progressPrintTime + 9000 <= System.currentTimeMillis() || bytesCopied == size) {
			long lastTime = progressPrintTime;
			progressPrintTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder();

			// Elapsed seconds
			long totalElapsedSec = (progressPrintTime - progressStartCopyTime) / 1000;
			sb.append("Elapsed: " + totalElapsedSec + "s  |  ");

			// Progress in bytes
			sb.append(Utils.size(bytesCopied) + " / " + Utils.size(size));

			// Progress in %
			sb.append(" (" + String.format("%.1f", (double) bytesCopied / (double) size * 100.0) + "%)");

			// Total speed
			if (totalElapsedSec > 0) {
				long bytesPerSec = bytesCopied / totalElapsedSec;
				sb.append("  |  [Average: " + Utils.size(bytesPerSec) + "/s, ");

				long remaningBytes = size - bytesCopied;
				long remaningSec = remaningBytes / bytesPerSec;
				sb.append("Remaning: " + remaningSec + "s]");
			}

			// Current speed
			long elapsed = progressPrintTime - lastTime;
			long diffBytes = bytesCopied - progressLastBytes;
			if (elapsed > 0 && elapsed < 100000 && diffBytes > 0) {
				sb.append("  |  [Current: " + Utils.size(diffBytes * 1000 / elapsed) + "/s]");
			}
			progressLastBytes = bytesCopied;

			System.out.println(sb);
		}
	}

	private void waitBeforeRetry() {
		System.out.println("Waiting " + waitBeforeRetryTimeSec + "s...");
		try {
			Thread.sleep(Duration.ofSeconds(waitBeforeRetryTimeSec));
		} catch (InterruptedException e) {
			System.err.println("Wait Interrupted: " + e.getMessage());
		}
		System.out.println("Retrying...");
	}
}

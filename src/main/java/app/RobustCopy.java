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

	public void copy(Path from, Path to) {
		// Get Size of Source
		long size = getSize(from);

		// Create all parent directories of target
		createDirectories(to.getParent());

		// Print file to copy
		System.out.println("Copying " + from + " => " + to + " (" + Utils.size(size) + ")");

		// States
		long bytesCopied = -1;
		progressPrintTime = -1;
		progressLastBytes = -1;
		SeekableByteChannel inChannel = null;
		SeekableByteChannel outChannel = null;
		progressStartCopyTime = System.currentTimeMillis();

		// Copy file
		while (bytesCopied < size) {
			try {
				// Open files
				inChannel = Files.newByteChannel(from, StandardOpenOption.READ);
				outChannel = Files.newByteChannel(to, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Rollback last two buffers
				bytesCopied = Math.max(0, bytesCopied - bb.capacity() * 2);
				if (bytesCopied > 0) {
					System.out.println("Restarting at: " + Utils.size(bytesCopied));
					inChannel.position(bytesCopied);
					outChannel.position(bytesCopied);
				}

				// Copy all bytes
				while (bytesCopied < size) {
					// Copy chunk
					int bytesRead = inChannel.read(bb.clear());
					int bytesWrite = outChannel.write(bb.flip());

					if (bytesRead != bytesWrite) {
						throw new IOException("Bytes missmatch, read: " + bytesRead + ", write: " + bytesWrite);
					}

					bytesCopied += bytesRead;

					printProgress(bytesCopied, size);
				}
			} catch (IOException e) {
				System.err.println("Copy problem: " + e.getMessage());
				waitBeforeRetry();
			} finally {
				// Close channels, ignore problems
				close(inChannel);
				close(outChannel);
			}
		}

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(from);
		System.out.println("Setting Last Modified Time to: " + lastModifiedTime);
		setLastModifiedTime(to, lastModifiedTime);
	}

	private void printProgress(long bytesCopied, long size) {
		// Print progress
		if (progressPrintTime + 9000 <= System.currentTimeMillis() || bytesCopied == size) {
			long lastTime = progressPrintTime;
			progressPrintTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder();

			// Elapsed seconds
			long totalElapsedSec = (progressPrintTime - progressStartCopyTime) / 1000;
			sb.append(Utils.timeElapsed(totalElapsedSec));

			// Progress in bytes
			sb.append("  |  " + Utils.size(bytesCopied) + " / " + Utils.size(size));

			// Progress in %
			sb.append(" (" + String.format("%.1f", (double) bytesCopied / (double) size * 100.0) + "%)");
			long remaningBytes = size - bytesCopied;

			// Total speed
			if (totalElapsedSec > 0) {
				long bytesPerSec = bytesCopied / totalElapsedSec;
				sb.append("  |  [Avg: " + Utils.size(bytesPerSec) + "/s");

				if (bytesPerSec > 0) {
					long remaningSec = remaningBytes / bytesPerSec;
					sb.append(", Rem: " + Utils.timeLeft(remaningSec));
				}
				sb.append("]");
			}

			// Current speed
			long elapsed = progressPrintTime - lastTime;
			long diffBytes = bytesCopied - progressLastBytes;
			if (elapsed > 0 && elapsed < 100000 && diffBytes > 0) {
				long bytesPerSec = diffBytes * 1000 / elapsed;
				sb.append("  |  [Cur: " + Utils.size(bytesPerSec) + "/s");
				sb.append("]");
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

	private long getSize(Path path) {
		long size = -1;
		while (size < 0) {
			try {
				size = Files.size(path);
			} catch (IOException e) {
				System.err.println("Error getting size: " + e.getMessage());
				waitBeforeRetry();
			}
		}
		return size;
	}

	private void createDirectories(Path path) {
		Path success = null;
		while (success == null) {
			try {
				success = Files.createDirectories(path);
			} catch (IOException e) {
				System.err.println("Error creating directories: " + e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private FileTime getLastModifiedTime(Path path) {
		FileTime fileTime = null;
		while (fileTime == null) {
			try {
				fileTime = Files.getLastModifiedTime(path);
			} catch (IOException e) {
				System.err.println("Error getting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
		return fileTime;
	}

	private void setLastModifiedTime(Path path, FileTime fileTime) {
		Path success = null;
		while (success == null) {
			try {
				success = Files.setLastModifiedTime(path, fileTime);
			} catch (IOException e) {
				System.err.println("Error setting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private void close(SeekableByteChannel channel) {
		if (channel != null) {
			try {
				if (channel.isOpen()) {
					channel.close();
				}
			} catch (IOException e) {
				System.err.println("Warning closing channel failed: " + e.getMessage());
			}
		}
	}
}

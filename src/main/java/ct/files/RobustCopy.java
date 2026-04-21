package ct.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import ct.files.io.IProgress;
import ct.files.io.IOWrapper;
import ct.files.meta.FileRecord;
import ct.files.meta.Settings;
import ct.utils.Utils;

public class RobustCopy {

	private final IOWrapper io;
	private final Settings settings;
	private final IProgress pr;
	private final ByteBuffer bb;

	private long progressPrintTime;
	private long progressLastBytes;
	private long progressStartCopyTime;

	public RobustCopy(IOWrapper io, Settings settings, IProgress pr) {
		this.io = io;
		this.settings = settings;
		this.pr = pr;
		// Allocate Buffer
		this.bb = ByteBuffer.allocateDirect(this.settings.bufferSize());
	}

	public void copy(FileRecord source, FileRecord target) {
		// Create all parent directories of target
		createDirectories(target.path().getParent());

		// Print file to copy
		pr.message("Copying " + source + " => " + target);

		// States
		boolean copyComplete = false;
		long bytesCopied = 0;
		progressPrintTime = -1;
		progressLastBytes = -1;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		progressStartCopyTime = System.currentTimeMillis();

		// Copy file
		while (!copyComplete) {
			try {
				// Open files
				inChannel = io.open(source.path(), StandardOpenOption.READ);
				outChannel = io.open(target.path(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Rollback last buffer
				bytesCopied = Math.max(0, bytesCopied - settings.bufferSize() * settings.rollbackBuffersNum());
				if (bytesCopied > 0) {
					pr.message("Restarting at: " + Utils.size(bytesCopied));
					io.position(inChannel, bytesCopied);
					io.position(outChannel, bytesCopied);
				}

				// Copy all bytes
				while (bytesCopied < source.size()) {
					// Copy chunk
					int bytesRead = io.read(inChannel, bb.clear());
					int bytesWrite = io.write(outChannel, bb.flip());

					if (bytesRead != bytesWrite) {
						throw new IOException("Bytes missmatch, read: " + bytesRead + ", write: " + bytesWrite);
					}

					bytesCopied += bytesRead;

					printProgress(bytesCopied, source.size());
				}

				// Truncate if larger (can be the case during overwrite)
				if (io.size(outChannel) > source.size()) {
					pr.message("Truncating to: " + Utils.size(source.size()));
					io.truncate(outChannel, source.size());
				}

				// Done
				copyComplete = true;
			} catch (IOException e) {
				pr.message("Copy problem: " + e.getMessage());
				waitBeforeRetry();
			} finally {
				// Close channels, ignore problems
				close(inChannel);
				close(outChannel);
			}
		}

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(source.path());
		pr.message("Setting Last Modified Time to: " + lastModifiedTime);
		setLastModifiedTime(target.path(), lastModifiedTime);
	}

	private void printProgress(long bytesCopied, long size) {
		// Print progress
		long currentTime = System.currentTimeMillis();
		if (progressPrintTime + 9000 <= currentTime || bytesCopied == size) {
			StringBuilder sb = new StringBuilder();

			// Elapsed seconds
			long totalElapsedSec = (currentTime - progressStartCopyTime) / 1000;
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
			long elapsed = currentTime - progressPrintTime;
			long diffBytes = bytesCopied - progressLastBytes;
			if (elapsed > 0 && diffBytes > 0 && progressPrintTime > 0) {
				long bytesPerSec = diffBytes * 1000 / elapsed;
				sb.append("  |  [Cur: " + Utils.size(bytesPerSec) + "/s");
				sb.append("]");
			}
			progressLastBytes = bytesCopied;
			progressPrintTime = currentTime;

			pr.message(sb.toString());
		}
	}

	private void waitBeforeRetry() {
		pr.message("Waiting " + settings.waitBeforeRetryTimeSec() + "s...");
		try {
			Thread.sleep(Duration.ofSeconds(settings.waitBeforeRetryTimeSec()));
		} catch (InterruptedException e) {
			pr.message("Warning: Wait Interrupted: " + e.getMessage());
		}
		pr.message("Retrying...");
	}

	private void createDirectories(Path path) {
		Path success = null;
		while (success == null) {
			try {
				success = io.createDirectories(path);
			} catch (IOException e) {
				pr.message("Error creating directories: " + e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private FileTime getLastModifiedTime(Path path) {
		FileTime fileTime = null;
		while (fileTime == null) {
			try {
				fileTime = io.getLastModifiedTime(path);
			} catch (IOException e) {
				pr.message("Error getting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
		return fileTime;
	}

	private void setLastModifiedTime(Path path, FileTime fileTime) {
		Path success = null;
		while (success == null) {
			try {
				success = io.setLastModifiedTime(path, fileTime);
			} catch (IOException e) {
				pr.message("Error setting last modified time: " + e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private void close(FileChannel channel) {
		if (channel != null) {
			try {
				if (channel.isOpen()) {
					channel.close();
				}
			} catch (IOException e) {
				pr.message("Warning: Closing channel failed: " + e.getMessage());
			}
		}
	}
}

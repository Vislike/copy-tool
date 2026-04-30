package ct.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import ct.app.Settings.RobustCopySettings;
import ct.files.io.IOWrapper;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressEvent.ModifiedTimeEvent;
import ct.files.progress.IProgressEvent.RestartEvent;
import ct.files.progress.IProgressEvent.TruncateEvent;
import ct.files.progress.IProgressEvent.WaitEndEvent;
import ct.files.progress.IProgressEvent.WaitStartEvent;
import ct.files.progress.IProgressReport;
import ct.files.types.CopyTask;

public class RobustCopy {

	private final IOWrapper io;
	private final RobustCopySettings settings;
	private final IProgressReport pr;
	private final ByteBuffer bb;

	public RobustCopy(IOWrapper io, RobustCopySettings settings, IProgressReport pr) {
		this.io = io;
		this.settings = settings;
		this.pr = pr;
		// Allocate Buffer
		this.bb = ByteBuffer.allocateDirect(this.settings.bufferSize());
	}

	public void copy(CopyTask ct) {
		// Start
		pr.event(new CopyStartEvent(ct));

		// Create all parent directories of target
		createDirectories(ct.targetFile().path().getParent());

		// States
		boolean copyComplete = false;
		long bytesCopied = 0;
		FileChannel inChannel = null;
		FileChannel outChannel = null;

		// Copy file
		while (!copyComplete) {
			try {
				// Open files
				inChannel = io.open(ct.sourceFile().path(), StandardOpenOption.READ);
				outChannel = io.open(ct.targetFile().path(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Rollback last buffer
				bytesCopied = Math.max(0, bytesCopied - settings.bufferSize() * settings.rollbackBuffersNum());
				if (bytesCopied > 0) {
					pr.event(new RestartEvent(bytesCopied));
					io.position(inChannel, bytesCopied);
					io.position(outChannel, bytesCopied);
				}

				// Copy all bytes
				while (bytesCopied < ct.sourceFile().size()) {
					// Copy chunk
					int bytesRead = io.read(inChannel, bb.clear());
					int bytesWrite = io.write(outChannel, bb.flip());

					if (bytesRead != bytesWrite) {
						throw new IOException("Bytes mismatch, read: " + bytesRead + ", write: " + bytesWrite);
					}

					bytesCopied += bytesRead;

					pr.event(new CopyProgressEvent(bytesCopied));
				}

				// Truncate if larger (can be the case during overwrite)
				if (io.size(outChannel) > ct.sourceFile().size()) {
					pr.event(new TruncateEvent(ct.sourceFile().size()));
					io.truncate(outChannel, ct.sourceFile().size());
				}

				// Done
				copyComplete = true;
			} catch (IOException e) {
				pr.error("Copy problem", e.getMessage());
				waitBeforeRetry();
			} finally {
				// Close channels, ignore problems
				close(inChannel);
				close(outChannel);
			}
		}

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(ct.sourceFile().path());
		pr.event(new ModifiedTimeEvent(lastModifiedTime));
		setLastModifiedTime(ct.targetFile().path(), lastModifiedTime);

		// End
		pr.event(new CopyEndEvent(ct));
	}

	private void waitBeforeRetry() {
		pr.event(new WaitStartEvent(settings.waitBeforeRetryTimeSec()));
		try {
			Thread.sleep(Duration.ofSeconds(settings.waitBeforeRetryTimeSec()));
		} catch (InterruptedException e) {
			pr.warning("Warning wait interrupted", e.getMessage());
		}
		pr.event(new WaitEndEvent());
	}

	private void createDirectories(Path path) {
		Path success = null;
		while (success == null) {
			try {
				success = io.createDirectories(path);
			} catch (IOException e) {
				pr.error("Error creating directories", e.getMessage());
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
				pr.error("Error getting last modified time", e.getMessage());
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
				pr.error("Error setting modified time", e.getMessage());
				waitBeforeRetry();
			}
		}
	}

	private void close(FileChannel channel) {
		if (channel != null) {
			try {
				if (channel.isOpen()) {
					io.close(channel);
				}
			} catch (IOException e) {
				pr.warning("Warning closing channel failed", e.getMessage());
			}
		}
	}
}

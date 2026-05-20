package ct.action;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import ct.action.io.IOWrapper;
import ct.action.progress.IProgressEvent.CopyEndEvent;
import ct.action.progress.IProgressEvent.CopyProgressEvent;
import ct.action.progress.IProgressEvent.CopyStartEvent;
import ct.action.progress.IProgressEvent.ModifiedTimeEvent;
import ct.action.progress.IProgressEvent.RestartEvent;
import ct.action.progress.IProgressEvent.RestartType;
import ct.action.progress.IProgressEvent.ResumeEvent;
import ct.action.progress.IProgressEvent.TruncateEvent;
import ct.action.progress.IProgressEvent.WaitEndEvent;
import ct.action.progress.IProgressEvent.WaitStartEvent;
import ct.action.progress.IProgressReport;
import ct.action.type.Buffers;
import ct.action.type.CopyTask;
import ct.app.Settings.RobustCopySettings;
import ct.util.Utils;

public class RobustCopy {

	private final IOWrapper io;
	private final RobustCopySettings settings;
	private final IProgressReport pr;
	private final Buffers buffers;

	public RobustCopy(IOWrapper io, RobustCopySettings settings, IProgressReport pr) {
		this.io = io;
		this.settings = settings;
		this.pr = pr;
		// Allocate Buffer
		this.buffers = createBuffers();
	}

	private Buffers createBuffers() {
		return new Buffers(1, settings.bufferSize());
	}

	public void copy(CopyTask ct) throws InterruptedException {
		// Start
		pr.event(new CopyStartEvent(ct));

		// Create all parent directories of target
		createDirectories(ct.targetFile().path().getParent());

		// Resume
		long startByte = 0;

		if (ct.sourceFile().position() > 0) {
			startByte = ct.sourceFile().position();
			pr.event(new ResumeEvent(startByte));
			if (startByte < ct.sourceFile().size() && startByte % settings.bufferSize() != 0) {
				pr.warning("Warning unaligned resume", ct + " at " + Utils.size(startByte));
			}
		}

		// Copy file
		singleThreadedBufferCopy(ct, startByte);

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(ct.sourceFile().path());
		pr.event(new ModifiedTimeEvent(lastModifiedTime));

		setLastModifiedTime(ct.targetFile().path(), lastModifiedTime);

		// End
		pr.event(new CopyEndEvent(ct));
	}

	private void singleThreadedBufferCopy(final CopyTask ct, final long startByte) throws InterruptedException {
		// States
		boolean copyComplete = false;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		long bytesCopied = startByte;
		ByteBuffer bb = buffers.next();

		// Error handling loop
		while (!copyComplete) {
			try {
				// Open files
				inChannel = io.open(ct.sourceFile().path(), StandardOpenOption.READ);
				outChannel = io.open(ct.targetFile().path(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Restart with Rollback
				bytesCopied = Math.max(0, bytesCopied - settings.bufferSize() * settings.rollbackBuffersNum());
				if (bytesCopied > 0) {
					pr.event(new RestartEvent(bytesCopied, RestartType.copy));
					io.position(inChannel, bytesCopied);
					io.position(outChannel, bytesCopied);
				}

				// Copy all bytes
				while (bytesCopied < ct.sourceFile().size()) {
					// Copy chunk
					int bytesRead = io.read(inChannel, bb.clear());
					int bytesWrite = io.write(outChannel, bb.flip());

					// Error checking
					if (bytesRead == -1) {
						throw new IOException("Unexpected EOF at: " + Utils.size(bytesCopied) + ", expected size: "
								+ Utils.size(ct.sourceFile().size()));
					}
					if (bytesRead == 0) {
						throw new IOException("Unexpected 0 byte read at: " + Utils.size(bytesCopied));
					}
					if (bytesWrite == 0) {
						throw new IOException("Unexpected 0 byte write at: " + Utils.size(bytesCopied));
					}
					if (bytesRead != bytesWrite) {
						throw new IOException("Unexpected mismatch at: " + Utils.size(bytesCopied) + ", read: "
								+ Utils.size(bytesRead) + ", write: " + Utils.size(bytesWrite));
					}

					// Successfully copied bytes
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
			} catch (ClosedByInterruptException e) {
				throw new InterruptedException();
			} catch (IOException e) {
				pr.error(switch (e) {
				case NoSuchFileException _ -> "Error no such file";
				default -> "Copy problem";
				}, e.getMessage());
				waitBeforeRetry();
			} finally {
				// Close channels, ignore problems
				close(inChannel);
				close(outChannel);
			}
		}
	}

	private void waitBeforeRetry() throws InterruptedException {
		pr.event(new WaitStartEvent(settings.waitBeforeRetryTimeSec()));
		Thread.sleep(Duration.ofSeconds(settings.waitBeforeRetryTimeSec()));
		pr.event(new WaitEndEvent());
	}

	private void createDirectories(Path path) throws InterruptedException {
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

	private FileTime getLastModifiedTime(Path path) throws InterruptedException {
		FileTime fileTime = null;
		while (fileTime == null) {
			try {
				fileTime = io.getLastModifiedTime(path);
			} catch (IOException e) {
				pr.error("Error getting modified time", e.getMessage());
				waitBeforeRetry();
			}
		}
		return fileTime;
	}

	private void setLastModifiedTime(Path path, FileTime fileTime) throws InterruptedException {
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

	private void close(FileChannel channel) throws InterruptedException {
		if (channel != null) {
			try {
				if (channel.isOpen()) {
					io.close(channel);
				}
			} catch (IOException e) {
				pr.error("Error closing channel", e.getMessage());
			}
		}
	}
}

package ct.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.RobustCopySettings;
import ct.files.io.IOWrapper;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressEvent.ModifiedTimeEvent;
import ct.files.progress.IProgressEvent.RestartEvent;
import ct.files.progress.IProgressEvent.RestartType;
import ct.files.progress.IProgressEvent.ResumeEvent;
import ct.files.progress.IProgressEvent.TruncateEvent;
import ct.files.progress.IProgressEvent.WaitEndEvent;
import ct.files.progress.IProgressEvent.WaitStartEvent;
import ct.files.progress.IProgressReport;
import ct.files.types.Buffers;
import ct.files.types.CopyTask;
import ct.utils.Utils;

public class RobustCopy {

	private static final boolean DEV_TRANSFER = true;
	private static final int MT_BUFFERS_IN_FLIGHT = 2;
	private static final int MT_BUFFERS_QUEUE = 2;

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
		if (Settings.devMode) {
			if (DEV_TRANSFER) {
				return null;
			} else {
				return new Buffers(MT_BUFFERS_IN_FLIGHT + MT_BUFFERS_QUEUE, settings.bufferSize());
			}
		} else {
			return new Buffers(1, settings.bufferSize());
		}
	}

	public void copy(CopyTask ct) {
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
		if (Settings.devMode) {
			if (DEV_TRANSFER) {
				devTestTransferTo(ct, startByte);
			} else {
				devTestmultiThreaded(ct, startByte);
			}
		} else {
			singleThreadedSynchronousCopyWithRollbackSupport(ct, startByte);
		}

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(ct.sourceFile().path());
		pr.event(new ModifiedTimeEvent(lastModifiedTime));

		setLastModifiedTime(ct.targetFile().path(), lastModifiedTime);

		// End
		pr.event(new CopyEndEvent(ct));
	}

	private void devTestTransferTo(final CopyTask ct, final long startByte) {
		// States
		boolean copyComplete = false;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		long bytesCopied = startByte;

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
					io.position(outChannel, bytesCopied);
				}

				// Copy all bytes
				while (bytesCopied < ct.sourceFile().size()) {
					// Copy chunk
					long toTransfer = Math.min(ct.sourceFile().size() - bytesCopied, settings.bufferSize());
					long bytesTransfered = io.transferTo(inChannel, bytesCopied, toTransfer, outChannel);

					// Error checking
					if (bytesTransfered == 0) {
						throw new IOException("Unexpected 0 byte transfer at: " + Utils.size(bytesCopied));
					}
					if (toTransfer != bytesTransfered) {
						throw new IOException("Unexpected mismatch at: " + Utils.size(bytesCopied) + ", expected: "
								+ Utils.size(toTransfer) + ", actual: " + Utils.size(bytesTransfered));
					}

					// Successfully copied bytes
					bytesCopied += bytesTransfered;
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
	}

	private void devTestmultiThreaded(final CopyTask ct, final long startByte) {

		// Thread sync
		final BlockingQueue<ByteBuffer> syncQueue = new ArrayBlockingQueue<>(MT_BUFFERS_QUEUE);

		// Read Thread
		App.thread().name(Thread.currentThread().getName() + "Reader").start(() -> {
			// Read States
			boolean readComplete = false;
			FileChannel inChannel = null;
			long bytesRead = startByte;

			// Read error handling loop
			while (!readComplete) {
				try {
					// Open source file
					inChannel = io.open(ct.sourceFile().path(), StandardOpenOption.READ);

					// Read restart
					if (bytesRead > 0) {
						pr.event(new RestartEvent(bytesRead, RestartType.read));
						io.position(inChannel, bytesRead);
					}

					// Read all bytes
					while (bytesRead < ct.sourceFile().size()) {
						// Read bytes
						int read = io.read(inChannel, buffers.current().clear());

						// Error checking
						if (read == -1) {
							throw new IOException("Unexpected EOF at: " + Utils.size(bytesRead) + ", expected size: "
									+ Utils.size(ct.sourceFile().size()));
						}
						if (read == 0) {
							throw new IOException("Unexpected 0 byte read at: " + Utils.size(bytesRead));
						}

						// Successfully read bytes
						syncQueue.put(buffers.next());
						bytesRead += read;
					}

					// Read done
					readComplete = true;
				} catch (IOException e) {
					pr.error("Read problem", e.getMessage());
					waitBeforeRetry();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError("Interrupt not implemented yet", e);
				} finally {
					close(inChannel);
				}
			}
		});

		// Write in current thread

		// Write states
		boolean writeComplete = false;
		FileChannel outChannel = null;
		long bytesWritten = startByte;
		boolean takeBuffer = true;
		ByteBuffer bb = null;

		// Write error handling loop
		while (!writeComplete) {
			try {
				// Open target file
				outChannel = io.open(ct.targetFile().path(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

				// Write restart
				if (bytesWritten > 0) {
					pr.event(new RestartEvent(bytesWritten, RestartType.write));
					io.position(outChannel, bytesWritten);
				}

				// Write all bytes
				while (bytesWritten < ct.sourceFile().size()) {
					// Take buffer from read thread
					if (takeBuffer) {
						bb = syncQueue.take().flip();
						takeBuffer = false;
					}

					// Write bytes
					int write = io.write(outChannel, bb.rewind());

					// Error checking
					if (write == 0) {
						throw new IOException("Unexpected 0 byte write at: " + Utils.size(bytesWritten));
					}
					if (bb.limit() != write) {
						throw new IOException("Unexpected mismatch at: " + Utils.size(bytesWritten) + ", read: "
								+ Utils.size(bb.limit()) + ", write: " + Utils.size(write));
					}

					// Successfully written bytes
					bytesWritten += write;
					takeBuffer = true;
					pr.event(new CopyProgressEvent(bytesWritten));
				}

				// Truncate if larger (can be the case during overwrite)
				if (io.size(outChannel) > ct.sourceFile().size()) {
					pr.event(new TruncateEvent(ct.sourceFile().size()));
					io.truncate(outChannel, ct.sourceFile().size());
				}

				// Write done
				writeComplete = true;
			} catch (IOException e) {
				pr.error("Write problem", e.getMessage());
				waitBeforeRetry();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupt not implemented yet", e);
			} finally {
				close(outChannel);
			}
		}
	}

	private void singleThreadedSynchronousCopyWithRollbackSupport(final CopyTask ct, final long startByte) {
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
			} catch (IOException e) {
				pr.error("Copy problem", e.getMessage());
				waitBeforeRetry();
			} finally {
				// Close channels, ignore problems
				close(inChannel);
				close(outChannel);
			}
		}
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
				pr.error("Error getting modified time", e.getMessage());
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
				pr.error("Error closing channel", e.getMessage());
			}
		}
	}
}

package ct.action.copy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.action.copy.io.Buffers;
import ct.action.copy.io.IOWrapper;
import ct.action.copy.model.CopyTask;
import ct.action.copy.progress.IProgressEvent.CopyProgressEvent;
import ct.action.copy.progress.IProgressEvent.RestartEvent;
import ct.action.copy.progress.IProgressEvent.RestartType;
import ct.action.copy.progress.IProgressEvent.TruncateEvent;
import ct.action.copy.progress.IProgressReport;
import ct.app.App;
import ct.app.Settings.RobustCopySettings;
import ct.util.Utils;

public class MultiThreadedCopy extends RobustCopy {

	private static final int MT_BUFFERS_IN_FLIGHT = 2;
	private static final int MT_BUFFERS_QUEUE = 2;

	private final Buffers buffers;

	MultiThreadedCopy(RobustCopySettings settings, IOWrapper io, IProgressReport pr) {
		super(settings, io, pr);

		// Allocate Buffer
		this.buffers = new Buffers(MT_BUFFERS_IN_FLIGHT + MT_BUFFERS_QUEUE, settings.bufferSize());
	}

	@Override
	void copyFile(CopyTask ct, long startByte) throws InterruptedException {
		// Thread sync
		final BlockingQueue<ByteBuffer> syncQueue = new ArrayBlockingQueue<>(MT_BUFFERS_QUEUE);

		// Read Thread
		App.thread().name(Thread.currentThread().getName() + "Reader").start(() -> {
			try {
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
								throw new IOException("Unexpected EOF at: " + Utils.size(bytesRead)
										+ ", expected size: " + Utils.size(ct.sourceFile().size()));
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
					} finally {
						close(inChannel);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupt not implemented yet", e);
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
			} catch (ClosedByInterruptException e) {
				throw new InterruptedException();
			} catch (IOException e) {
				pr.error("Write problem", e.getMessage());
				waitBeforeRetry();
			} finally {
				close(outChannel);
			}
		}
	}
}

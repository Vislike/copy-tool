package ct.action.copy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

import ct.action.copy.io.Buffers;
import ct.action.copy.io.IOWrapper;
import ct.action.copy.model.CopyTask;
import ct.action.copy.progress.IProgressEvent.CopyProgressEvent;
import ct.action.copy.progress.IProgressEvent.RestartEvent;
import ct.action.copy.progress.IProgressEvent.RestartType;
import ct.action.copy.progress.IProgressEvent.TruncateEvent;
import ct.action.copy.progress.IProgressReport;
import ct.app.Settings.RobustCopySettings;
import ct.util.Utils;

public class DirectBufferCopy extends RobustCopy {

	private final Buffers buffers;

	DirectBufferCopy(RobustCopySettings settings, IOWrapper io, IProgressReport pr) {
		super(settings, io, pr);

		// Allocate Buffer
		this.buffers = new Buffers(1, settings.bufferSize());
	}

	@Override
	void copyFile(CopyTask ct, final long startByte) throws InterruptedException {
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
}

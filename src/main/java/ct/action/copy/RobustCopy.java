package ct.action.copy;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import ct.action.copy.io.IOWrapper;
import ct.action.copy.model.CopyTask;
import ct.action.copy.progress.IProgressEvent.CopyEndEvent;
import ct.action.copy.progress.IProgressEvent.CopyStartEvent;
import ct.action.copy.progress.IProgressEvent.ModifiedTimeEvent;
import ct.action.copy.progress.IProgressEvent.ResumeEvent;
import ct.action.copy.progress.IProgressEvent.WaitEndEvent;
import ct.action.copy.progress.IProgressEvent.WaitStartEvent;
import ct.action.copy.progress.IProgressReport;
import ct.app.Settings;
import ct.app.Settings.RobustCopySettings;
import ct.util.Utils;

public abstract class RobustCopy {

	public static RobustCopy create(RobustCopySettings settings, IOWrapper io, IProgressReport pr) {
		if (Settings.devMode) {
			return new MultiThreadedCopy(settings, io, pr);
		}
		if (settings.zeroCopy()) {
			return new ZeroCopy(settings, io, pr);
		}
		return new DirectBufferCopy(settings, io, pr);
	}

	protected final RobustCopySettings settings;
	protected final IOWrapper io;
	protected final IProgressReport pr;

	RobustCopy(RobustCopySettings settings, IOWrapper io, IProgressReport pr) {
		this.settings = settings;
		this.io = io;
		this.pr = pr;
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
		copyFile(ct, startByte);

		// Set last modified time to same as source
		FileTime lastModifiedTime = getLastModifiedTime(ct.sourceFile().path());
		pr.event(new ModifiedTimeEvent(lastModifiedTime));
		setLastModifiedTime(ct.targetFile().path(), lastModifiedTime);

		// End
		pr.event(new CopyEndEvent(ct));
	}

	abstract void copyFile(CopyTask ct, long startByte) throws InterruptedException;

	protected void waitBeforeRetry() throws InterruptedException {
		pr.event(new WaitStartEvent(settings.waitBeforeRetryTimeSec()));
		Thread.sleep(Duration.ofSeconds(settings.waitBeforeRetryTimeSec()));
		pr.event(new WaitEndEvent());
	}

	protected void close(FileChannel channel) {
		if (channel != null) {
			try {
				if (channel.isOpen()) {
					io.close(channel);
				}
			} catch (IOException e) {
				// Ignore
			}
		}
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
}

package ct.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.app.Settings;
import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressReport;
import ct.files.types.CopyTask;
import ct.tui.types.ProgressUpdate;

public class MultiFileCopy {

	private final Settings settings;
	private final BlockingQueue<ProgressUpdate> progressQueue;

	private static class ProgressSender implements IProgressReport {

		private final int threadId;
		private final BlockingQueue<ProgressUpdate> mq;

		public ProgressSender(int threadId, BlockingQueue<ProgressUpdate> mq) {
			this.threadId = threadId;
			this.mq = mq;
		}

		@Override
		public void event(IProgressEvent event) {
			try {
				mq.put(new ProgressUpdate(threadId, event, false));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupt not implemented yet", e);
			}
		}

		public void done() {
			try {
				mq.put(new ProgressUpdate(threadId, null, true));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupt not implemented yet", e);
			}
		}

	}

	public MultiFileCopy(Settings settings) {
		this.settings = settings;
		progressQueue = new ArrayBlockingQueue<>(settings.filesSimultaneously() * 4);
	}

	public void copyAll(List<CopyTask> fileList) {
		BlockingQueue<CopyTask> copyTaskQueue = new ArrayBlockingQueue<>(fileList.size(), false, fileList);

		List<Optional<Thread>> threads = new ArrayList<>();

		for (int i = 0; i < settings.filesSimultaneously(); i++) {
			Thread thread = workerThread(i, copyTaskQueue);
			threads.add(Optional.of(thread));
		}

		TerminalUpdater terminalUpdater = new TerminalUpdater(settings);

		try {
			while (!allEof(threads)) {
				ProgressUpdate progressUpdate = progressQueue.take();
				if (progressUpdate.eof()) {
					threads.set(progressUpdate.threadId(), Optional.empty());
				}
				terminalUpdater.update(progressUpdate);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupt not implemented yet", e);
		}
	}

	private Thread workerThread(int threadId, BlockingQueue<CopyTask> copyTaskQueue) {
		return Thread.ofVirtual().start(() -> {
			ProgressSender ps = new ProgressSender(threadId, progressQueue);
			RobustCopy rc = new RobustCopy(new FilesIO(), settings, ps);
			CopyTask ct;
			while ((ct = copyTaskQueue.poll()) != null) {
				rc.copy(ct);
			}
			ps.done();
		});
	}

	private boolean allEof(List<Optional<Thread>> threads) {
		for (var t : threads) {
			if (t.isPresent()) {
				return false;
			}
		}
		return true;
	}
}

package ct.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.app.Settings;
import ct.files.RobustCopy;
import ct.files.io.IOWrapper;
import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressReport;
import ct.files.types.CopyTask;
import ct.tui.types.ProgressUpdate;

public class MultiFileCopy {

	private final Settings settings;
	private final IOWrapper io;
	private final BlockingQueue<ProgressUpdate> progressQueue;

	public MultiFileCopy(Settings settings, IOWrapper io) {
		this.settings = settings;
		this.io = io;
		progressQueue = new ArrayBlockingQueue<>(settings.filesSimultaneously() * 4);
	}

	public void copyAll(List<CopyTask> tasks) {
		BlockingQueue<CopyTask> copyTaskQueue = new ArrayBlockingQueue<>(tasks.size(), false, tasks);

		List<Optional<Thread>> threads = new ArrayList<>();

		for (int tId = 0; tId < settings.filesSimultaneously(); tId++) {
			threads.add(Optional.of(workerThread(tId, copyTaskQueue)));
		}

		TerminalUpdater terminalUpdater = new TerminalUpdater(settings);

		try {
			while (threads.stream().anyMatch(Optional::isPresent)) {
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

	private Thread workerThread(int tId, BlockingQueue<CopyTask> copyTaskQueue) {
		return Thread.ofVirtual().start(() -> {
			ProgressSender ps = new ProgressSender(tId, progressQueue);
			RobustCopy rc = new RobustCopy(io, settings, ps);
			CopyTask ct;
			while ((ct = copyTaskQueue.poll()) != null) {
				rc.copy(ct);
			}
			ps.done();
		});
	}
}

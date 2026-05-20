package ct.runner.copy;

import java.lang.Thread.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.action.RobustCopy;
import ct.action.io.IOWrapper;
import ct.action.progress.IProgressEvent;
import ct.action.progress.IProgressEvent.AbortEvent;
import ct.action.progress.IProgressReport;
import ct.action.type.CopyTask;
import ct.app.App;
import ct.app.Settings;
import ct.tui.copy.AnsiTerminalProgress;
import ct.tui.copy.ProgressUpdate;

public class MultiFileCopy {

	private static final int QUEUE_SIZE_PER_THREAD = 4;

	private final Settings settings;
	private final IOWrapper io;
	private final BlockingQueue<ProgressUpdate> progressQueue;

	private static class WorkerThread {
		private final Thread thread;
		private boolean active = true;

		WorkerThread(Thread thread) {
			this.thread = thread;
		}

		boolean isActive() {
			return active;
		}

		public void eof() {
			active = false;
		}
	}

	public MultiFileCopy(Settings settings, IOWrapper io) {
		this.settings = settings;
		this.io = io;
		progressQueue = new ArrayBlockingQueue<>(settings.multiFile().filesSimultaneously() * QUEUE_SIZE_PER_THREAD);
	}

	public void copyAll(List<CopyTask> tasks) {
		AnsiTerminalProgress progress = new AnsiTerminalProgress(settings.multiFile(), tasks.size());
		List<WorkerThread> threads = new ArrayList<>();

		Builder threadBuilder = App.thread().name("CopyWorker", 1);
		BlockingQueue<CopyTask> copyTaskQueue = new ArrayBlockingQueue<>(tasks.size(), false, tasks);

		for (int tId = 0; tId < settings.multiFile().filesSimultaneously(); tId++) {
			threads.add(new WorkerThread(workerThread(threadBuilder, tId, copyTaskQueue)));
		}

		eventLoop(threads, progress);
	}

	private void eventLoop(List<WorkerThread> threads, AnsiTerminalProgress progress) {
		try {
			while (threads.stream().anyMatch(WorkerThread::isActive)) {
				ProgressUpdate progressUpdate = progressQueue.take();
				if (progressUpdate.eof()) {
					threads.get(progressUpdate.threadId()).eof();
					progress.eof(progressUpdate.threadId());
				} else {
					progress.update(progressUpdate.event(), progressUpdate.threadId());
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupt not implemented yet", e);
		}
	}

	private Thread workerThread(Builder tb, final int tId, BlockingQueue<CopyTask> copyTaskQueue) {
		return tb.start(() -> {
			ProgressSender ps = new ProgressSender(tId, progressQueue);
			RobustCopy rc = new RobustCopy(io, settings.robustCopy(), ps);
			CopyTask ct = null;
			try {
				while ((ct = copyTaskQueue.poll()) != null) {
					rc.copy(ct);
				}
				ps.done();
			} catch (InterruptedException e) {
				ps.abort(new AbortEvent(ct));
			}
		});
	}

	private static class ProgressSender implements IProgressReport {

		private final int threadId;
		private final BlockingQueue<ProgressUpdate> mq;

		public ProgressSender(int threadId, BlockingQueue<ProgressUpdate> mq) {
			this.threadId = threadId;
			this.mq = mq;
		}

		@Override
		public void event(IProgressEvent event) throws InterruptedException {
			mq.put(new ProgressUpdate(threadId, event, false));
		}

		@Override
		public void abort(AbortEvent event) {
			mq.offer(new ProgressUpdate(threadId, event, true));
		}

		public void done() throws InterruptedException {
			mq.put(new ProgressUpdate(threadId, null, true));
		}
	}
}

package ct.runner.copy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.action.copy.RobustCopy;
import ct.action.copy.io.IOWrapper;
import ct.action.copy.model.CopyTask;
import ct.action.copy.progress.IProgressEvent;
import ct.action.copy.progress.IProgressReport;
import ct.action.copy.progress.IProgressEvent.AbortEvent;
import ct.app.App;
import ct.app.Settings;
import ct.tui.copy.AnsiTerminalProgress;

public class MultiFileCopy implements ICopyRunnerModule {

	private static final int QUEUE_SIZE_PER_THREAD = 4;
	private static final String NAME_PREFIX = "CopyWorker";

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

	@Override
	public void copyAll(List<CopyTask> tasks) {
		AnsiTerminalProgress progress = new AnsiTerminalProgress(settings.multiFile(), tasks.size());
		List<WorkerThread> threads = new ArrayList<>();

		BlockingQueue<CopyTask> copyTaskQueue = new ArrayBlockingQueue<>(tasks.size(), false, tasks);

		for (int tId = 0; tId < settings.multiFile().filesSimultaneously(); tId++) {
			threads.add(new WorkerThread(workerThread(tId, copyTaskQueue)));
		}

		eventLoop(threads, progress);
	}

	private void eventLoop(List<WorkerThread> threads, AnsiTerminalProgress progress) {
		try {
			// Run until done
			while (threads.stream().anyMatch(WorkerThread::isActive)) {
				ProgressUpdate pu = progressQueue.take();
				if (pu.exception() != null) {
					App.error("Exception thrown by", threadName(pu.threadId()));
					throw pu.exception();
				} else if (pu.event() != null) {
					progress.update(pu.event(), pu.threadId());
				} else {
					threads.get(pu.threadId()).eof();
					progress.eof(pu.threadId());
				}
			}
		} catch (Throwable t) {
			final long maxTime = Duration.ofSeconds(App.SHUTDOWN_SOFT_WAIT).toMillis() + System.currentTimeMillis();

			// Abort all workers
			threads.forEach(w -> {
				if (w.thread.isAlive()) {
					App.verbose("Stopping thread", w.thread.getName());
					w.thread.interrupt();
				}
			});

			// Wait for all workers
			for (WorkerThread w : threads) {
				boolean alive = w.thread.isAlive();
				long waitTime = maxTime - System.currentTimeMillis();
				if (waitTime > 0) {
					try {
						alive = !w.thread.join(Duration.ofMillis(waitTime));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new AssertionError("Unexpected interrupt during worker abort", e);
					}
				}

				if (alive) {
					App.error("Timeout stopping", w.thread.getName());
				}
			}
			if (!(t instanceof InterruptedException)) {
				throw new RuntimeException(t);
			}
		}
	}

	private Thread workerThread(final int tId, BlockingQueue<CopyTask> copyTaskQueue) {
		ProgressSender ps = new ProgressSender(tId, progressQueue);
		return App.thread().name(threadName(tId)).uncaughtExceptionHandler((_, e) -> {
			try {
				ps.exception(e);
			} catch (InterruptedException _) {
				// Just let thread die
			}
		}).start(() -> {
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

	private String threadName(int tId) {
		return NAME_PREFIX + (tId + 1);
	}

	public record ProgressUpdate(int threadId, IProgressEvent event, Throwable exception) {
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
			mq.put(new ProgressUpdate(threadId, event, null));
		}

		@Override
		public void abort(AbortEvent event) {
			App.highlight("Aborted", event.ct().sourceFile());
		}

		void done() throws InterruptedException {
			mq.put(new ProgressUpdate(threadId, null, null));
		}

		void exception(Throwable e) throws InterruptedException {
			mq.put(new ProgressUpdate(threadId, null, e));
		}
	}
}

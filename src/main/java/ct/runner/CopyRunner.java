package ct.runner;

import java.time.Duration;

import ct.action.io.FilesIO;
import ct.action.type.AnalyseResult;
import ct.app.App;
import ct.app.Settings;
import ct.runner.copy.LogModeCopy;
import ct.runner.copy.MultiFileCopy;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class CopyRunner {

	private static final int SHUTDOWN_WAIT = 10;
	private static Thread shutdownHookThread;

	public static void execute(AnalyseResult files, Settings settings) {
		createAndAddShutdownHook();

		Timer timer = Utils.timer();
		if (settings.multiFile().logMode()) {
			new LogModeCopy(settings, new FilesIO()).copyall(files.copy());
		} else {
			new MultiFileCopy(settings, new FilesIO()).copyAll(files.copy());
		}
		App.infolb(timer.elapsedSeconds("Copy Complete in"));

		removeShutdownHook();
	}

	private static void createAndAddShutdownHook() {
		final Thread mainThread = Thread.currentThread();
		shutdownHookThread = new Thread(() -> {
			try {
				App.warning("Shutdown requested, aborting...");
				mainThread.interrupt();
				boolean terminated = mainThread.join(Duration.ofSeconds(SHUTDOWN_WAIT));
				if (!terminated) {
					App.error("Graceful shutdown failed, hard exiting, timeout seconds", SHUTDOWN_WAIT);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Unexpected interrupt in shutdown hook", e);
			}
		});

		Runtime.getRuntime().addShutdownHook(shutdownHookThread);
	}

	private static void removeShutdownHook() {
		try {
			Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
		} catch (IllegalStateException _) {
			// Ignore
		}
	}
}

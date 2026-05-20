package ct.runner;

import java.time.Duration;

import ct.action.io.FilesIO;
import ct.action.type.AnalyseResult;
import ct.app.App;
import ct.app.Settings;
import ct.runner.copy.ICopyRunnerModule;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class CopyRunner {

	private static Thread shutdownHookThread;

	public static void execute(AnalyseResult files, Settings settings) {
		Timer timer = Utils.timer();
		createAndAddShutdownHook();
		try {
			ICopyRunnerModule cm = ICopyRunnerModule.create(settings, new FilesIO());
			cm.copyAll(files.copy());
		} finally {
			removeShutdownHook();
			App.infolb(timer.elapsedSeconds("Copy Finished in"));
		}
	}

	private static void createAndAddShutdownHook() {
		final Thread mainThread = Thread.currentThread();
		shutdownHookThread = new Thread(() -> {
			try {
				App.warning("Shutdown requested, aborting...");
				mainThread.interrupt();
				boolean terminated = mainThread.join(Duration.ofSeconds(App.SHUTDOWN_HARD_WAIT));
				if (!terminated) {
					App.error("Graceful shutdown failed, hard exiting, timeout reached", App.SHUTDOWN_HARD_WAIT);
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

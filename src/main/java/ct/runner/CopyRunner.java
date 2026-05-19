package ct.runner;

import java.time.Duration;
import java.util.List;

import ct.action.RobustCopy;
import ct.action.io.FilesIO;
import ct.action.type.AnalyseResult;
import ct.action.type.CopyTask;
import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.RobustCopySettings;
import ct.tui.StdoutPrinter;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class CopyRunner {

	private static final int SHUTDOWN_WAIT = 2;

	public static void execute(AnalyseResult files, Settings settings) {

		Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					App.warning("Shutdown requested, waiting max", SHUTDOWN_WAIT);
					mainThread.interrupt();
					boolean terminated = mainThread.join(Duration.ofSeconds(SHUTDOWN_WAIT));
					if (!terminated) {
						App.error("All threads not stopped, hard exit...");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError("Interrupt not implemented yet", e);
				}
			}
		});

		Timer timer = Utils.timer();
		if (settings.multiFile().logMode()) {
			logMode(settings.robustCopy(), files.copy());
		} else {
			new MultiFileCopy(settings, new FilesIO()).copyAll(files.copy());
		}
		App.infolb(timer.elapsedSeconds("Copy Complete in"));
	}

	private static void logMode(RobustCopySettings settings, List<CopyTask> tasks) {
		RobustCopy rc = new RobustCopy(new FilesIO(), settings, new StdoutPrinter());
		tasks.forEach(ct -> {
			App.info();
			rc.copy(ct);
		});
	}
}

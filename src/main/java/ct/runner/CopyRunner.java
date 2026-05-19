package ct.runner;

import java.util.List;

import ct.action.RobustCopy;
import ct.action.io.FilesIO;
import ct.action.type.AnalyseResult;
import ct.action.type.CopyTask;
import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.RobustCopySettings;
import ct.tui.MultiFileCopy;
import ct.tui.StdoutPrinter;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class CopyRunner {

	public static void execute(AnalyseResult files, Settings settings) {
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

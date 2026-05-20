package ct.runner.copy;

import java.util.List;

import ct.action.RobustCopy;
import ct.action.io.IOWrapper;
import ct.action.progress.IProgressEvent.AbortEvent;
import ct.action.progress.IProgressReport;
import ct.action.type.CopyTask;
import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.RobustCopySettings;
import ct.tui.copy.StdoutProgress;

public class LogModeCopy implements ICopyRunnerModule {

	private final RobustCopySettings settings;
	private final IOWrapper io;

	public LogModeCopy(Settings settings, IOWrapper io) {
		this.settings = settings.robustCopy();
		this.io = io;
	}

	@Override
	public void copyAll(List<CopyTask> tasks) {
		IProgressReport pr = new StdoutProgress();
		RobustCopy rc = new RobustCopy(io, settings, pr);
		for (CopyTask task : tasks) {
			try {
				App.info();
				rc.copy(task);
			} catch (InterruptedException e) {
				pr.abort(new AbortEvent(task));
				return;
			}
		}
	}
}

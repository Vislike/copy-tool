package ct.runner.copy;

import java.util.List;

import ct.action.io.IOWrapper;
import ct.action.type.CopyTask;
import ct.app.Settings;

public interface ICopyRunnerModule {

	void copyAll(List<CopyTask> tasks);

	static ICopyRunnerModule create(Settings s, IOWrapper io) {
		return s.multiFile().logMode() ? new LogModeCopy(s, io) : new MultiFileCopy(s, io);
	}
}

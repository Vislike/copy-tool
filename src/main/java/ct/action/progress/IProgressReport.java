package ct.action.progress;

import ct.action.progress.IProgressEvent.ErrorEvent;
import ct.action.progress.IProgressEvent.WarningEvent;

public interface IProgressReport {

	void event(IProgressEvent event);

	default void warning(String desc, String cause) {
		event(new WarningEvent(desc, cause));
	}

	default void error(String desc, String cause) {
		event(new ErrorEvent(desc, cause));
	}
}

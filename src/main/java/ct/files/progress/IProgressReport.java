package ct.files.progress;

import ct.files.progress.IProgressEvent.ErrorEvent;
import ct.files.progress.IProgressEvent.WarningEvent;

public interface IProgressReport {

	void raise(IProgressEvent event);

	default void warning(String desc, String cause) {
		raise(new WarningEvent(desc, cause));
	}

	default void error(String desc, String cause) {
		raise(new ErrorEvent(desc, cause));
	}
}

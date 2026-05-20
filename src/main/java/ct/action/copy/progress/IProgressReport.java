package ct.action.copy.progress;

import ct.action.copy.progress.IProgressEvent.AbortEvent;
import ct.action.copy.progress.IProgressEvent.ErrorEvent;
import ct.action.copy.progress.IProgressEvent.WarningEvent;

public interface IProgressReport {

	void event(IProgressEvent event) throws InterruptedException;

	void abort(AbortEvent event);

	default void warning(String desc, String cause) throws InterruptedException {
		event(new WarningEvent(desc, cause));
	}

	default void error(String desc, String cause) throws InterruptedException {
		event(new ErrorEvent(desc, cause));
	}
}

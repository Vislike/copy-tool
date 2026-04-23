package ct.files.progress;

import ct.files.progress.IProgressEvent.ErrorEvent;

public interface IProgressReport {

	void raise(IProgressEvent event);

	default void error(String msg) {
		raise(new ErrorEvent(msg));
	}
}

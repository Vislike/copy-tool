package ct.action;

import ct.action.progress.IProgressEvent;
import ct.action.progress.IProgressReport;

public class TestVoidProgress implements IProgressReport {

	@Override
	public void event(IProgressEvent event) {
		// Void
	}
}

package ct.files;

import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressReport;

public class TestVoidProgress implements IProgressReport {

	@Override
	public void raise(IProgressEvent event) {
		// Void
	}
}

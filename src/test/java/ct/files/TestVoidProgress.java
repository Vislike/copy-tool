package ct.files;

import ct.files.progress.IProgressReport;

public class TestVoidProgress implements IProgressReport {

	@Override
	public void message(String str) {
		// Void
	}
}

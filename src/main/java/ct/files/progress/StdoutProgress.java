package ct.files.progress;

import ct.app.App;

public class StdoutProgress implements IProgressReport {

	@Override
	public void message(String str) {
		App.info(str);
	}
}

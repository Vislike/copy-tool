package ct.files.io;

import ct.app.App;

public class StdoutProgress implements IProgress {

	@Override
	public void message(String str) {
		App.info(str);
	}
}

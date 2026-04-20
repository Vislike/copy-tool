package ct.app;

import java.util.List;

import ct.files.Analyse.Copy;
import ct.files.RobustCopy;
import ct.files.io.FilesWrapper;
import ct.files.meta.Settings;

public class Tui {

	private final Settings settings;

	public Tui(Settings settings) {
		this.settings = settings;
	}

	public void copyAll(List<Copy> copy) {
		RobustCopy rc = new RobustCopy(new FilesWrapper(), settings);
		copy.forEach(c -> {
			rc.copy(c.sourceFile(), c.targetFile());
			App.info();
		});
	}

}

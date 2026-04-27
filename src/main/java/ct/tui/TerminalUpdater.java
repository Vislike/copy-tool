package ct.tui;

import java.util.ArrayList;
import java.util.List;

import ct.app.App;
import ct.app.Settings;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.tui.StdoutPrinter.DeBounce;
import ct.tui.types.ProgressUpdate;
import ct.utils.AnsiEscapeCodes;

public class TerminalUpdater {

	private static final long DEBOUNCE_TIME = 900;

	private class ThreadStatus {

		private DeBounce db;
		boolean eof = false;
		String message = "";

		public ThreadStatus() {
		}

		public boolean update(ProgressUpdate pu) {

			if (pu.eof()) {
				eof = true;
				return true;
			}

			switch (pu.event()) {
			case CopyStartEvent e -> {
				db = new DeBounce(e.ct().sourceFile().size());
				message = StdoutPrinter.stringMessage(pu.event(), db);
			}
			case CopyProgressEvent e -> {
				if (db.shouldUpdate(e.size(), DEBOUNCE_TIME)) {
					message = StdoutPrinter.stringMessage(pu.event(), db);
				} else {
					return false;
				}
			}
			default -> message = StdoutPrinter.stringMessage(pu.event(), db);
			}

			return true;
		}
	}

	private final Settings settings;
	private final List<ThreadStatus> statuses = new ArrayList<>();

	private int rows = 0;
	private StringBuilder sb = new StringBuilder();

	public TerminalUpdater(Settings settings) {
		this.settings = settings;
		for (int i = 0; i < settings.filesSimultaneously(); i++) {
			statuses.add(new ThreadStatus());
		}
	}

	public void update(ProgressUpdate pu) {
		if (statuses.get(pu.threadId()).update(pu)) {
			draw();
		}
	}

	private void draw() {
		// Clear old
		clear();

		sb.append(nl()).append("Copy status:").append(nl());
		for (ThreadStatus threadStatus : statuses) {
			if (!threadStatus.eof) {
				sb.append(threadStatus.message.substring(0,
						Math.min(threadStatus.message.length(), settings.terminalWidth()))).append(nl());
			}
		}
		App.infonn(sb.toString());
	}

	private String nl() {
		rows++;
		return System.lineSeparator();
	}

	private void clear() {
		sb.setLength(0);
		AnsiEscapeCodes.moveUpAndErase(sb, rows);
		rows = 0;
	}
}

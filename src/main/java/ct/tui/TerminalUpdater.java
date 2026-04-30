package ct.tui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ct.app.App;
import ct.app.Settings.MultiFileSettings;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressEvent.ErrorEvent;
import ct.files.progress.IProgressEvent.WaitEndEvent;
import ct.files.progress.IProgressEvent.WaitStartEvent;
import ct.tui.types.DeBounce;
import ct.tui.types.ProgressUpdate;
import ct.utils.AnsiEscapeCodes;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Utils;

public class TerminalUpdater {

	private static final long DEBOUNCE_TIME = 1000;
	// GREEN = 5, RESET = 3, "[] " = 3, Copying = 7
	private static final int STATUS_SIZE = 18;

	enum State {
		Copying(Color.GREEN), Waiting(Color.RED), Retryin(Color.YELLOW);

		final Color c;

		State(Color c) {
			this.c = c;
		}
	}

	private class Row {
		boolean eof = false;
		DeBounce db;
		String name;
		String heading = "Starting up...";
		String body = "Grabbing task...";

		public void heading(Path path) {
			int len = path.getNameCount();
			name = path.toString();
			for (int i = 1; i < len && name.length() > settings.terminalWidth() - STATUS_SIZE; i++) {
				name = path.subpath(i, len).toString();
			}
		}

		void state(State s) {
			this.heading = overflow(s.c.state(s.toString(), name));
		}

		void body(String body) {
			this.body = overflow(body);
		}

		private String overflow(String text) {
			String prot = text.replaceAll("\r?\n", " ");
			return prot.substring(0, Math.min(prot.length(), settings.terminalWidth()));
		}

	}

	private final MultiFileSettings settings;
	private final List<Row> rows = new ArrayList<>();

	private int newLines = 0;
	private StringBuilder sb = new StringBuilder();
	private boolean firstLog = true;

	public TerminalUpdater(MultiFileSettings settings) {
		this.settings = settings;
		for (int tId = 0; tId < settings.filesSimultaneously(); tId++) {
			rows.add(new Row());
		}
	}

	public void update(ProgressUpdate pu) {
		Row row = rows.get(pu.threadId());
		if (pu.eof()) {
			row.eof = true;
			draw();
			return;
		}

		switch (pu.event()) {
		case CopyStartEvent e -> {
			row.db = new DeBounce(DEBOUNCE_TIME, e.ct().sourceFile().size());
			row.heading(e.ct().sourceFile().relativeFromSource());
			row.state(State.Copying);
			row.body(StdoutPrinter.createProgress(0, row.db));
			draw();
		}
		case CopyProgressEvent e -> {
			if (row.db.shouldUpdate(e.size())) {
				row.state(State.Copying);
				row.body(StdoutPrinter.createProgress(e.size(), row.db));
				draw();
			}
		}
		case CopyEndEvent e -> log(Color.YELLOW.highlight("Copied", e.ct().sourceFile() + copyStats(row.db)));
		case ErrorEvent e -> row.body(Color.RED.highlight(e.description(), e.cause()));
		case WaitStartEvent _ -> {
			row.state(State.Waiting);
			draw();
		}
		case WaitEndEvent _ -> {
			row.state(State.Retryin);
			draw();
		}
		default -> {
		}
		}
	}

	private void draw() {
		clear();
		paint();
	}

	private void log(String msg) {
		if (firstLog) {
			clear();
			firstLog = false;
		} else {
			clearAppendLog();
		}
		sb.append(System.lineSeparator());
		sb.append(msg).append(System.lineSeparator());
		paint();
	}

	private void paint() {
		if (rows.stream().anyMatch(r -> !r.eof)) {
			Color.WHITE_INTENSE.highlight(sb.append(nl()), "Copy progress:").append(nl());
		}
		for (Row row : rows) {
			if (!row.eof) {
				sb.append(row.heading).append(nl());
				sb.append(row.body).append(nl());
			}
		}
		App.infonn(sb.toString());
	}

	private String nl() {
		newLines++;
		return System.lineSeparator();
	}

	private void clear() {
		sb.setLength(0);
		AnsiEscapeCodes.moveUpAndErase(sb, newLines);
		newLines = 0;
	}

	private void clearAppendLog() {
		sb.setLength(0);
		AnsiEscapeCodes.moveEndOfPrevAndErase(sb, newLines);
		newLines = 0;
	}

	private String copyStats(DeBounce db) {
		StringBuilder sb = new StringBuilder();
		long seconds = (db.time() - db.startTime()) / 1000;
		sb.append(" in ").append(Utils.timeDuration(seconds));
		if (seconds > 0) {
			sb.append(" [").append(Utils.size(db.size() / seconds)).append("/s]");
		}
		return sb.toString();
	}
}

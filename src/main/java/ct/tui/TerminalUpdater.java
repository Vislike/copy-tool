package ct.tui;

import java.util.ArrayList;
import java.util.List;

import ct.app.App;
import ct.app.Settings;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.tui.types.DeBounce;
import ct.tui.types.ProgressUpdate;
import ct.utils.AnsiEscapeCodes;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Utils;

public class TerminalUpdater {

	private static final long DEBOUNCE_TIME = 1000;

	private class Row {
		private DeBounce db;
		boolean eof = false;
		String heading = "Starting up...";
		String body = "Grabbing task...";
	}

	private final Settings settings;
	private final List<Row> rows = new ArrayList<>();

	private int newLines = 0;
	private StringBuilder sb = new StringBuilder();
	private boolean firstLog = true;

	public TerminalUpdater(Settings settings) {
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
			row.heading = e.ct().sourceFile().relativeFromSource().toString();
			row.body = StdoutPrinter.createProgress(0, row.db);
			draw();
		}
		case CopyProgressEvent e -> {
			if (row.db.shouldUpdate(e.size())) {
				row.body = StdoutPrinter.createProgress(e.size(), row.db);
				draw();
			}
		}
		case CopyEndEvent e -> {
			log(Color.YELLOW.highlight("Copied", e.ct().sourceFile() + copyStats(row.db)));
		}
		default -> {
		}
		}
	}

	private String copyStats(DeBounce db) {
		StringBuilder sb = new StringBuilder();
		long seconds = (db.time() - db.startTime()) / 1000;
		sb.append(" in ").append(Utils.timeDuration(seconds));
		if (seconds > 0) {
			sb.append(" (").append(Utils.size(db.size() / seconds)).append("/s)");
		}
		return sb.toString();
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
		for (Row threadStatus : rows) {
			if (!threadStatus.eof) {
				sb.append(maxWidth(threadStatus.heading)).append(nl());
				sb.append(maxWidth(threadStatus.body)).append(nl());
			}
		}
		App.infonn(sb.toString());
	}

	private String maxWidth(String text) {
		return text.substring(0, Math.min(text.length(), settings.terminalWidth()));
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
}

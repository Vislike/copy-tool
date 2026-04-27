package ct.tui;

import ct.app.App;
import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressEvent.ErrorEvent;
import ct.files.progress.IProgressEvent.ModifiedTimeEvent;
import ct.files.progress.IProgressEvent.RestartEvent;
import ct.files.progress.IProgressEvent.TruncateEvent;
import ct.files.progress.IProgressEvent.WaitEndEvent;
import ct.files.progress.IProgressEvent.WaitStartEvent;
import ct.files.progress.IProgressEvent.WarningEvent;
import ct.files.progress.IProgressReport;
import ct.tui.types.DeBounce;
import ct.utils.Utils;

public class StdoutPrinter implements IProgressReport {

	private static final long DEBOUNCE_TIME = 10000;

	private DeBounce db;

	@Override
	public void event(IProgressEvent event) {
		switch (event) {
		case CopyStartEvent e -> {
			db = new DeBounce(DEBOUNCE_TIME, e.ct().sourceFile().size());
			App.highlight("Copying", e.ct().sourceFile() + " => " + e.ct().targetFile());
		}
		case CopyProgressEvent e -> {
			if (db.shouldUpdate(e.size())) {
				App.info(createProgress(e.size(), db));
			}
		}
		case CopyEndEvent e -> App.highlight("Complete", e.ct().sourceFile());
		case ModifiedTimeEvent e -> App.verbose("Setting Modified Time to", e.time());
		case WarningEvent e -> App.recoverWarning(e.description(), e.cause());
		case ErrorEvent e -> App.recoverError(e.description(), e.cause());
		default -> App.info(message(event));
		}
	}

	private static String message(IProgressEvent event) {
		return switch (event) {
		case RestartEvent e -> "Restarting at: " + Utils.size(e.pos());
		case TruncateEvent e -> "Truncating to: " + Utils.size(e.size());
		case WaitStartEvent e -> "Waiting " + e.seconds() + "s...";
		case WaitEndEvent _ -> "Retrying...";
		default -> null;
		};
	}

	static String createProgress(long bytes, DeBounce db) {
		StringBuilder sb = new StringBuilder(Utils.SB_SIZE);
		long currentTime = System.currentTimeMillis();

		// Elapsed seconds
		long totalElapsedSec = (currentTime - db.startTime()) / 1000;
		sb.append(Utils.timeElapsed(totalElapsedSec));

		// Progress in bytes
		sb.append("  |  " + Utils.size(bytes) + " / " + Utils.size(db.size()));

		// Progress in %
		sb.append(" (" + String.format("%.1f", (double) bytes / (double) db.size() * 100.0) + "%)");

		// Total speed
		if (totalElapsedSec > 0) {
			long bytesPerSec = bytes / totalElapsedSec;
			sb.append("  |  [Avg: " + Utils.size(bytesPerSec) + "/s");

			if (bytesPerSec > 0) {
				long remaningSec = (db.size() - bytes) / bytesPerSec;
				sb.append(", Rem: " + Utils.timeLeft(remaningSec));
			}
			sb.append("]");
		}

		// Current speed
		long elapsed = currentTime - db.time();
		long diffBytes = bytes - db.bytes();
		if (elapsed > 0 && diffBytes > 0 && db.time() > 0) {
			long bytesPerSec = diffBytes * 1000 / elapsed;
			sb.append("  |  [Cur: " + Utils.size(bytesPerSec) + "/s");
			sb.append("]");
		}

		// Update de bounce
		db.update(currentTime, bytes);

		return sb.toString();
	}
}

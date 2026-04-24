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
import ct.utils.Utils;

public class PrintBufferedProgress implements IProgressReport {

	private static final long DEBOUNCE_TIME = 9000;

	private DeBounce db = new DeBounce(-1);

	@Override
	public void raise(IProgressEvent event) {
		switch (event) {
		case CopyStartEvent e -> {
			db = new DeBounce(e.ct().sourceFile().size());
			App.highlight("Copying", e.ct().sourceFile() + " => " + e.ct().targetFile());
		}
		case CopyProgressEvent e -> {
			if (db.shouldUpdate(e.size(), DEBOUNCE_TIME)) {
				App.info(stringMessage(event, db));
			}
		}
		case CopyEndEvent e -> App.highlight("Successfully copied", e.ct().sourceFile());
		case ModifiedTimeEvent e -> App.verbose("Setting Modified Time to", e.time());
		case WarningEvent e -> App.recoverWarning(e.description(), e.cause());
		case ErrorEvent e -> App.recoverError(e.description(), e.cause());
		default -> App.info(stringMessage(event, db));
		}
	}

	public static String stringMessage(IProgressEvent event, DeBounce db) {
		return switch (event) {
		case CopyStartEvent e -> "Copying " + e.ct().sourceFile() + " => " + e.ct().targetFile();
		case CopyEndEvent e -> "Successfully copied " + e.ct().sourceFile();
		case CopyProgressEvent e -> db.status(e.size());
		case RestartEvent e -> "Restarting at: " + Utils.size(e.pos());
		case TruncateEvent e -> "Truncating to: " + Utils.size(e.size());
		case ModifiedTimeEvent e -> "Setting Modified Time to: " + e.time();
		case WaitStartEvent e -> "Waiting " + e.seconds() + "s...";
		case WaitEndEvent _ -> "Retrying...";
		case WarningEvent e -> e.description() + ": " + e.cause();
		case ErrorEvent e -> e.description() + ": " + e.cause();
		};
	}

	static class DeBounce {

		private final long size;
		private final long startTime;

		private long oldTime = -1;
		private long oldBytes = -1;

		DeBounce(long size) {
			this.size = size;
			this.startTime = System.currentTimeMillis();
		}

		public boolean shouldUpdate(long bytes, long time) {
			return oldTime + time <= System.currentTimeMillis() || bytes >= size;
		}

		String status(long bytes) {
			long currentTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder();

			// Elapsed seconds
			long totalElapsedSec = (currentTime - startTime) / 1000;
			sb.append(Utils.timeElapsed(totalElapsedSec));

			// Progress in bytes
			sb.append("  |  " + Utils.size(bytes) + " / " + Utils.size(size));

			// Progress in %
			sb.append(" (" + String.format("%.1f", (double) bytes / (double) size * 100.0) + "%)");

			// Total speed
			if (totalElapsedSec > 0) {
				long bytesPerSec = bytes / totalElapsedSec;
				sb.append("  |  [Avg: " + Utils.size(bytesPerSec) + "/s");

				if (bytesPerSec > 0) {
					long remaningSec = (size - bytes) / bytesPerSec;
					sb.append(", Rem: " + Utils.timeLeft(remaningSec));
				}
				sb.append("]");
			}

			// Current speed
			long elapsed = currentTime - oldTime;
			long diffBytes = bytes - oldBytes;
			if (elapsed > 0 && diffBytes > 0 && oldTime > 0) {
				long bytesPerSec = diffBytes * 1000 / elapsed;
				sb.append("  |  [Cur: " + Utils.size(bytesPerSec) + "/s");
				sb.append("]");
			}
			oldBytes = bytes;
			oldTime = currentTime;

			return sb.toString();
		}
	}
}

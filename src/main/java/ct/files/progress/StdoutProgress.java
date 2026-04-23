package ct.files.progress;

import ct.app.App;
import ct.files.progress.IProgressEvent.CopyEndEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressEvent.ErrorEvent;
import ct.files.progress.IProgressEvent.ModifiedTimeEvent;
import ct.files.progress.IProgressEvent.RestartEvent;
import ct.files.progress.IProgressEvent.TruncateEvent;
import ct.files.progress.IProgressEvent.WaitEndEvent;
import ct.files.progress.IProgressEvent.WaitStartEvent;
import ct.utils.Utils;

public class StdoutProgress implements IProgressReport {

	public Progress p = new Progress(-1);

	@Override
	public void raise(IProgressEvent event) {
		switch (event) {
		case CopyStartEvent e -> {
			progressStart(e.ct().sourceFile().size());
			App.info(stringMessage(event));
		}
		case CopyProgressEvent e -> {
			if (progressUpdate(e.size(), 9000)) {
				App.info(stringMessage(event));
			}
		}
		default -> {
			App.info(stringMessage(event));
		}
		}
	}

	public String stringMessage(IProgressEvent event) {
		return switch (event) {
		case CopyStartEvent e -> "Copying " + e.ct().sourceFile() + " => " + e.ct().targetFile();
		case CopyEndEvent e -> "Successfully copied " + e.ct().sourceFile();
		case CopyProgressEvent e -> progress(e.size());
		case RestartEvent e -> "Restarting at: " + Utils.size(e.pos());
		case TruncateEvent e -> "Truncating to: " + Utils.size(e.size());
		case ModifiedTimeEvent e -> "Setting Modified Time to: " + e.time();
		case WaitStartEvent e -> "Waiting " + e.seconds() + "s...";
		case WaitEndEvent _ -> "Retrying...";
		case ErrorEvent e -> e.msg();
		};
	}

	public void progressStart(long size) {
		p = new Progress(size);
	}

	public boolean progressUpdate(long bytes, int time) {
		long currentTime = System.currentTimeMillis();
		return p.oldTime + time <= currentTime || bytes == p.size;
	}

	private String progress(long size) {
		return p.status(size);
	}

	private static class Progress {

		private final long size;
		private final long startTime;

		private long oldTime = -1;
		private long oldBytes = -1;

		Progress(long size) {
			this.size = size;
			this.startTime = System.currentTimeMillis();
		}

		String status(long bytesCopied) {
			long currentTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder();

			// Elapsed seconds
			long totalElapsedSec = (currentTime - startTime) / 1000;
			sb.append(Utils.timeElapsed(totalElapsedSec));

			// Progress in bytes
			sb.append("  |  " + Utils.size(bytesCopied) + " / " + Utils.size(size));

			// Progress in %
			sb.append(" (" + String.format("%.1f", (double) bytesCopied / (double) size * 100.0) + "%)");

			// Total speed
			if (totalElapsedSec > 0) {
				long bytesPerSec = bytesCopied / totalElapsedSec;
				sb.append("  |  [Avg: " + Utils.size(bytesPerSec) + "/s");

				if (bytesPerSec > 0) {
					long remaningSec = (size - bytesCopied) / bytesPerSec;
					sb.append(", Rem: " + Utils.timeLeft(remaningSec));
				}
				sb.append("]");
			}

			// Current speed
			long elapsed = currentTime - oldTime;
			long diffBytes = bytesCopied - oldBytes;
			if (elapsed > 0 && diffBytes > 0 && oldTime > 0) {
				long bytesPerSec = diffBytes * 1000 / elapsed;
				sb.append("  |  [Cur: " + Utils.size(bytesPerSec) + "/s");
				sb.append("]");
			}
			oldBytes = bytesCopied;
			oldTime = currentTime;

			return sb.toString();
		}
	}
}

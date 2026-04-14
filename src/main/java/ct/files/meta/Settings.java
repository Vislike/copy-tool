package ct.files.meta;

import java.nio.file.Path;

public record Settings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite, int bufferSize,
		int waitBeforeRetryTimeSec) {
	public static boolean rawBytes = false;

	public static Settings bufferSize(int bufferSize) {
		return new Settings(null, null, false, false, bufferSize, 0);
	}
}

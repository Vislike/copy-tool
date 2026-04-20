package ct.files.meta;

import java.nio.file.Path;

public record Settings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite, int bufferSize,
		int waitBeforeRetryTimeSec, int rollbackBuffersNum) {

	public static boolean verbose = false;
	public static boolean rawBytes = false;
	public static boolean terminalColor = true;
	public static boolean terminalUserInterface = true;

	public static Settings bufferSize(int bufferSize) {
		return rollback(bufferSize, 0);
	}

	public static Settings rollback(int bufferSize, int rollbackBuffersNum) {
		return new Settings(null, null, false, false, bufferSize, 0, rollbackBuffersNum);
	}
}

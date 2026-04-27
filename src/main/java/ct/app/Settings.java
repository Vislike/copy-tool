package ct.app;

import java.nio.file.Path;

public record Settings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite, int bufferSize,
		int waitBeforeRetryTimeSec, int rollbackBuffersNum, int filesSimultaneously, int terminalWidth) {

	public static boolean verbose = false;
	public static boolean rawBytes = false;
	public static boolean terminalColor = true;
	public static boolean terminalUserInterface = true;
	public static boolean devMode = false;

	public static Settings bufferSize(int bufferSize) {
		return rollback(bufferSize, 0);
	}

	public static Settings rollback(int bufferSize, int rollbackBuffersNum) {
		return new Settings(null, null, false, false, bufferSize, 0, rollbackBuffersNum, 0, 0);
	}
}

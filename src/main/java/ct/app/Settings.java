package ct.app;

import java.nio.file.Path;

public record Settings(AnalyseSettings analyse, RobustCopySettings robustCopy, MultiFileSettings multiFile) {

	public static record AnalyseSettings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite,
			boolean resume) {
	}

	public static record RobustCopySettings(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum) {
	}

	public static record MultiFileSettings(boolean logMode, int filesSimultaneously, int terminalWidth) {
	}

	public static boolean verbose = false;
	public static boolean rawBytes = false;
	public static boolean terminalColor = true;
	public static boolean terminalUserInterface = true;
	public static boolean devMode = false;

	public static Settings bufferSize(int bufferSize) {
		return rollback(bufferSize, 0);
	}

	public static Settings rollback(int bufferSize, int rollbackBuffersNum) {
		return numFiles(bufferSize, rollbackBuffersNum, 0, 0);
	}

	public static Settings numFiles(int bufferSize, int rollbackBuffersNum, int waitBeforeRetryTimeSec,
			int filesSimultaneously) {
		return new Settings(new AnalyseSettings(null, null, false, false, false),
				new RobustCopySettings(bufferSize, waitBeforeRetryTimeSec, rollbackBuffersNum),
				new MultiFileSettings(false, filesSimultaneously, App.TERMINAL_WIDTH));
	}
}

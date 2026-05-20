package ct.app;

import java.nio.file.Path;

public record Settings(AnalyseSettings analyse, RobustCopySettings robustCopy, MultiFileSettings multiFile) {

	public static record AnalyseSettings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite,
			boolean resume) {
	}

	public static record RobustCopySettings(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum,
			boolean zeroCopy) {
	}

	public static record MultiFileSettings(boolean logMode, int filesSimultaneously, int terminalWidth) {
	}

	public static boolean verbose = false;
	public static boolean rawBytes = false;
	public static boolean terminalColor = true;
	public static boolean terminalUserInterface = true;
	public static boolean devMode = false;

	public static Settings testBufferSizes(int bufferSize, boolean zeroCopy) {
		return testFactory(bufferSize, 0, 0, 0, zeroCopy);
	}

	public static Settings testRobustCopy(int bufferSize, int rollbackBuffersNum, boolean zeroCopy) {
		return testFactory(bufferSize, 0, rollbackBuffersNum, 0, zeroCopy);
	}

	public static Settings testFactory(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum,
			int filesSimultaneously, boolean zeroCopy) {
		return new Settings(new AnalyseSettings(null, null, false, false, false),
				new RobustCopySettings(bufferSize, waitBeforeRetryTimeSec, rollbackBuffersNum, zeroCopy),
				new MultiFileSettings(false, filesSimultaneously, App.TERMINAL_WIDTH));
	}
}

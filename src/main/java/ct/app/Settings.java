package ct.app;

import java.nio.file.Path;

public record Settings(AnalyseSettings analyse, RobustCopySettings robustCopy, MultiFileSettings multiFile) {

	public static record AnalyseSettings(Path sourceDir, Path targetDir, boolean dryRun, boolean overwrite,
			boolean resume) {
	}

	public static record RobustCopySettings(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum,
			boolean multiThreaded) {
	}

	public static record MultiFileSettings(boolean logMode, int filesSimultaneously, int terminalWidth) {
	}

	public static boolean verbose = false;
	public static boolean rawBytes = false;
	public static boolean terminalColor = true;
	public static boolean terminalUserInterface = true;
	public static boolean devMode = false;

	public static Settings testBufferSizes(int bufferSize) {
		return testParametersHelper(bufferSize, 0, 0, false, 0);
	}

	public static Settings testRobustCopy(int bufferSize, int rollbackBuffersNum, boolean multiThreaded) {
		return testParametersHelper(bufferSize, 0, rollbackBuffersNum, multiThreaded, 0);
	}

	public static Settings testChaos(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum,
			int filesSimultaneously) {
		return testParametersHelper(bufferSize, waitBeforeRetryTimeSec, rollbackBuffersNum, false, filesSimultaneously);
	}

	private static Settings testParametersHelper(int bufferSize, int waitBeforeRetryTimeSec, int rollbackBuffersNum,
			boolean multiThreaded, int filesSimultaneously) {
		return new Settings(new AnalyseSettings(null, null, false, false, false),
				new RobustCopySettings(bufferSize, waitBeforeRetryTimeSec, rollbackBuffersNum, multiThreaded),
				new MultiFileSettings(false, filesSimultaneously, App.TERMINAL_WIDTH));
	}

}

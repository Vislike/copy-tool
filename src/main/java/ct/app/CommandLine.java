package ct.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import ct.app.Settings.AnalyseSettings;
import ct.app.Settings.MultiFileSettings;
import ct.app.Settings.RobustCopySettings;

public class CommandLine {

	private CommandLine() {
	}

	static void help() {
		App.infolb("""
				Usage:
				ct [-options] <src> <dst>

				    <src> Can be file or directory (filesystem root is not supported).
				    <dst> Must be directory (since <src> structure is kept).

				Options - Defaults in parentheses, D = Disabled, E = Eanbled:
				  Functional:
				    -h    Show this help, and exit.
				    -d    Dry Run, analyse only, skips file copy. (D)
				    -n n  Copy multiple files at the same time, 1-8. (%1$d)
				    -o    Overwrite modified files instead of skipping them. (D)
				    -v    Verbose output, for debugging purpose. (D)
				    -x    Dev Mode, enables experimental features. (D)
				  Visual:
				    -b    Enable show all sizes in raw bytes instead of human readable. (D)
				    -c    Disable colors in text output. (E)
				    -w n  Max width of dynamic content, 40-500. (%2$d)
				""".formatted(App.NUM_FILES_SIMULTANEOUSLY, App.TERMINAL_WIDTH));
	}

	private static enum ReqParams {
		SOURCE, TARGET, DONE;
	}

	private static enum OptParams {
		NONE, TERM_WIDTH, MULTIPLE_FILES;
	}

	static void parseOutputArgs(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'b' -> Settings.rawBytes = true;
					case 'c' -> Settings.terminalColor = false;
					case 'v' -> Settings.verbose = true;
					case 'x' -> Settings.devMode = true;
					}
				}
			}
		}
	}

	static Optional<Settings> parseArgs(String[] args) {
		// States
		ReqParams reqParams = ReqParams.SOURCE;
		OptParams optParams = OptParams.NONE;

		Path sourceDir = null;
		Path targetDir = null;
		boolean dryRun = false;
		boolean overwrite = false;
		int filesSimultaneously = App.NUM_FILES_SIMULTANEOUSLY;
		int terminalWidth = App.TERMINAL_WIDTH;

		// Parse
		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'h' -> {
						return Optional.empty();
					}
					case 'd' -> dryRun = true;
					case 'o' -> overwrite = true;
					case 'b', 'c', 'v', 'x' -> {
						// Handled in parseOutputArgs
					}
					case 'w' -> optParams = OptParams.TERM_WIDTH;
					case 'n' -> optParams = OptParams.MULTIPLE_FILES;
					default -> {
						App.error("Invalid parameter", arg.charAt(i));
						return Optional.empty();
					}
					}
				}
			} else {
				if (optParams == OptParams.NONE) {
					// Parse Required
					switch (reqParams) {
					case SOURCE -> {
						sourceDir = Paths.get(arg).toAbsolutePath().normalize();
						reqParams = ReqParams.TARGET;
					}
					case TARGET -> {
						targetDir = Paths.get(arg).toAbsolutePath().normalize();
						reqParams = ReqParams.DONE;
					}
					default -> {
						App.error("Invalid parameter", arg);
						return Optional.empty();
					}
					}
				} else {
					// Parse Optional
					try {
						switch (optParams) {
						case NONE -> throw new AssertionError();
						case TERM_WIDTH -> terminalWidth = Integer.parseInt(arg);
						case MULTIPLE_FILES -> filesSimultaneously = Integer.parseInt(arg);
						}
					} catch (NumberFormatException e) {
						App.error("N must be a number", arg);
						return Optional.empty();
					}
					optParams = OptParams.NONE;
				}
			}
		}

		// Validate
		if (sourceDir == null) {
			App.error("Missing required parameter", "<src>", "<dst>");
			return Optional.empty();
		} else if (sourceDir.getFileName() == null) {
			App.error("Invalid parameter, <src> must be a file or directory", sourceDir);
			return Optional.empty();
		}

		if (targetDir == null) {
			App.error("Missing required parameter", "<dst>");
			return Optional.empty();
		} else if (Files.isRegularFile(targetDir)) {
			App.error("Invalid parameter, <dst> must be a directory", targetDir);
			return Optional.empty();
		}

		if (filesSimultaneously < 1 || filesSimultaneously > 8) {
			App.error("Invlaid value for -n", filesSimultaneously);
			return Optional.empty();
		}

		if (terminalWidth < 40 || terminalWidth > 500) {
			App.error("Invlaid value for -w", terminalWidth);
			return Optional.empty();
		}

		// Done
		AnalyseSettings aSettings = new AnalyseSettings(sourceDir, targetDir, dryRun, overwrite);
		RobustCopySettings rcSettings = new RobustCopySettings(App.BUFF_SIZE, App.WAIT_TIME, App.ROLLBACK_BUFFERS);
		MultiFileSettings mfSettings = new MultiFileSettings(filesSimultaneously, terminalWidth);
		return Optional.of(new Settings(aSettings, rcSettings, mfSettings));
	}
}

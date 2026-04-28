package ct.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CommandLine {

	private CommandLine() {
	}

	static void help() {
		App.info("""
				Usage:
				ct [-options] <src> <dst>

				    <src> Can be file or directory (filesystem root is not supported).
				    <dst> Must be directory (since <src> structure is kept).

				Options - Defaults in parentheses, D = Disabled, E = Eanbled:
				  Functional:
				    -h    Show this help, and exit.
				    -d    Dry Run, analyse only, skips file copy. (D)
				    -o    Overwrite modified files instead of skipping them. (D)
				    -v    Verbose output, for debugging purpose. (D)
				  Visual:
				    -b    Enable show all sizes in raw bytes instead of human readable. (D)
				    -c    Disable colors in text output. (E)
				    -w n  Max width of dynamic content. (120)
				""");
	}

	private static enum ReqParams {
		SOURCE, TARGET, DONE;
	}

	private static enum OptParams {
		TERM_WIDTH, NONE;
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
		ReqParams reqParams = ReqParams.SOURCE;
		OptParams optParams = OptParams.NONE;

		Path sourceDir = null;
		Path targetDir = null;
		boolean dryRun = false;
		boolean overwrite = false;
		int terminalWidth = App.TERMINAL_WIDTH;

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
						case TERM_WIDTH -> terminalWidth = Integer.parseInt(arg);
						case NONE -> throw new AssertionError();
						}
					} catch (NumberFormatException e) {
						App.error("N must be a number", arg);
						return Optional.empty();
					}
					optParams = OptParams.NONE;
				}
			}
		}

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
		Settings s = new Settings(sourceDir, targetDir, dryRun, overwrite, App.BUFF_SIZE, App.WAIT_TIME,
				App.ROLLBACK_BUFFERS, App.NUM_FILES_SIMULTANEOUSLY, terminalWidth);
		return Optional.of(s);
	}
}

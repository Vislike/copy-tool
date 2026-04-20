package ct.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import ct.files.meta.Settings;

public class CommandLine {

	private CommandLine() {
	}

	static void printHelp() {
		App.info("""
				Usage:
				ct [-options] <src> <dst>

				    <src> Can be file or directory (filesystem root is not supported).
				    <dst> Must be directory (since <src> structure is kept).

				Options:
				    -h    Show this help, and exit.
				    -d    Dry Run, skips file copy.
				    -o    Overwrite modified files instead of skipping them.

				    -b    Show all sizes in raw bytes instead of human readable.
				    -c    Disable colors in text output.
				    -v    Enable verbose output, for debugging purpose.
				""");
	}

	private static enum ReqParams {
		SOURCE, TARGET, DONE;
	}

	private static enum OptParams {
		NONE;
	}

	static void parseOutputArgs(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'b' -> Settings.rawBytes = true;
					case 'c' -> Settings.terminalColor = false;
					case 'v' -> Settings.verbose = true;
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

		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'h' -> {
						return Optional.empty();
					}
					case 'd' -> dryRun = true;
					case 'o' -> overwrite = true;
					case 'b', 'c', 'v' -> {
						// Handled in parseOutputArgs
					}
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
				App.ROLLBACK_BUFFERS, App.NUM_FILES_SIMULTANEOUSLY);
		return Optional.of(s);
	}
}

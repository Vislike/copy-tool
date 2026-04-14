package ct.app;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import ct.files.Analyse;
import ct.files.RobustCopy;
import ct.files.Analyse.FoundFiles;
import ct.files.meta.Settings;

public class App {

	public static final int BUFF_SIZE = 1024 * 1024 * 1;
	public static final int WAIT_TIME = 10;

	public static void main(String[] args) throws IOException {
		System.out.println("= = = = Copy Tool = = = =" + System.lineSeparator());

		parseArgs(args).ifPresentOrElse(App::copyAllFiles, App::printHelp);
	}

	private static Optional<Settings> parseArgs(String[] args) {
		Path sourceDir = null;
		Path targetDir = null;
		boolean dryRun = false;
		boolean overwrite = false;

		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'd' -> dryRun = true;
					case 'b' -> Settings.rawBytes = true;
					case 'o' -> overwrite = true;
					case 'h' -> {
						return Optional.empty();
					}
					default -> {
						System.err.println("Invalid parameter: " + arg + System.lineSeparator());
						return Optional.empty();
					}
					}
				}
			} else {
				if (sourceDir == null) {
					sourceDir = Paths.get(arg).toAbsolutePath().normalize();
				} else if (targetDir == null) {
					targetDir = Paths.get(arg).toAbsolutePath().normalize();
				}
			}
		}

		if (sourceDir == null) {
			System.err.println("Missing required parameter: <src>" + System.lineSeparator());
			return Optional.empty();
		} else if (sourceDir.getFileName() == null) {
			System.err.println("Invalid: <src> must be a file or directory" + System.lineSeparator());
			return Optional.empty();
		}

		if (targetDir == null) {
			System.err.println("Missing required parameter: <dst>" + System.lineSeparator());
			return Optional.empty();
		}

		return Optional.of(new Settings(sourceDir, targetDir, dryRun, overwrite, BUFF_SIZE, WAIT_TIME));
	}

	private static void printHelp() {
		System.out.println("""
				Usage:
				ct [-options] <src> <dst>

				    <src> Can be file or directory (filesystem root is not supported).
				    <dst> Must be directory (since <src> structure is kept).

				Options:
				-h    Show this help, and exit.
				-d    Dry Run, skips file copy.
				-o    Overwrite modified files instead of skipping them.
				-b    Show all sizes in raw bytes instead of human readable.
				""");
	}

	private static void copyAllFiles(Settings settings) {
		System.out.println("Copy from: " + settings.sourceDir());
		System.out.println("Copy to: " + settings.targetDir().resolve(settings.sourceDir().getFileName()));
		System.out.println();

		System.out.print("Finding files...");

		FoundFiles files = Analyse.findAllFiles(settings);

		System.out.println("complete");
		System.out.println();

		if (!files.match().isEmpty()) {
			System.out.println("+ + + + Existing matching files (size and modify date) + + + +");
			files.match().forEach(System.out::println);
			System.out.println();
		}

		if (!files.missmatch().isEmpty()) {
			System.out.println("- - - - Existing mismatching files, "
					+ (settings.overwrite() ? "overwriting" : "skipping") + " - - - -");
			files.missmatch().forEach(System.out::println);
			System.out.println();
		}

		if (!files.copy().isEmpty()) {
			System.out.println("* * * * Files to Copy * * * *");
			files.copy().forEach(System.out::println);
			System.out.println();
		}

		if (settings.dryRun()) {
			System.out.println("Dry Run Complete");
		} else {
			RobustCopy rc = new RobustCopy(settings);
			files.copy().forEach(c -> {
				rc.copy(c.sourceFile(), c.targetFile());
				System.out.println();
			});
		}
	}
}

package ct.app;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import ct.app.Analyse.Result;
import ct.copy.RobustCopy;
import ct.copy.meta.FileRecord;
import ct.copy.meta.Settings;
import ct.utils.Utils;

public class App {

	public static final int BUFF_SIZE = 1024 * 1024 * 1;
	public static final int WAIT_TIME = 10;

	public static boolean dryRun = false;
	public static boolean overwrite = false;

	private static record Copy(FileRecord sourceFile, FileRecord targetFile) {
		@Override
		public String toString() {
			return sourceFile.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("= = = = Copy Tool = = = =");
		System.out.println();

		Path sourceDir = null;
		Path targetDir = null;

		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'd' -> App.dryRun = true;
					case 'b' -> Utils.rawBytes = true;
					case 'o' -> App.overwrite = true;
					case 'h' -> {
						printHelp();
						return;
					}
					default -> {
						System.out.println("Invalid parameter: " + arg);
						System.out.println();
						printHelp();
						return;
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

		if (sourceDir != null && targetDir != null) {
			findAllFiles(sourceDir, targetDir);
		} else {
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println("""
				Usage:
				ct [-options] <src> <dst>

				    <src> Can be file or directory.
				    <dst> Must be directory (since <src> structure is kept).

				Options:
				-h    Show this help, and exit.
				-d    Dry Run, skips file copy.
				-o    Overwrite modified files instead of skipping them.
				-b    Show all sizes in raw bytes instead of human readable.
				""");
	}

	private static void findAllFiles(final Path sourceDir, final Path targetDir) throws IOException {
		System.out.println("Copy from: " + sourceDir);
		System.out.println("Copy to: " + targetDir);
		System.out.println();

		System.out.print("Finding files...");
		List<FileRecord> match = new ArrayList<>();
		List<FileRecord> noMatch = new ArrayList<>();
		List<Copy> copy = new ArrayList<>();

		Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path sourceFile, BasicFileAttributes attrs) throws IOException {
				final Path relativize = sourceDir.getParent().relativize(sourceFile);
				final Path targetFile = targetDir.resolve(relativize);
				Result result = Analyse.files(sourceFile, targetFile);
				switch (result.status()) {
				case COPY -> copy.add(new Copy(result.sourceFile(), result.targetFile()));
				case MATCH -> match.add(result.sourceFile());
				case NO_MATCH -> {
					noMatch.add(result.sourceFile());
					if (overwrite) {
						copy.add(new Copy(result.sourceFile(), result.targetFile()));
					}
				}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		System.out.println("complete");
		System.out.println();

		if (!match.isEmpty()) {
			System.out.println("+ + + + Existing matching files (size and modify date) + + + +");
			match.forEach(System.out::println);
			System.out.println();
		}

		if (!noMatch.isEmpty()) {
			System.out.println(
					"- - - - Existing mismatching files, " + (overwrite ? "overwriting" : "skipping") + " - - - -");
			noMatch.forEach(System.out::println);
			System.out.println();
		}

		if (!copy.isEmpty()) {
			System.out.println("* * * * Files to Copy * * * *");
			copy.forEach(System.out::println);
			System.out.println();
		}

		if (dryRun) {
			System.out.println("Dry Run Complete");
		} else {
			RobustCopy rc = new RobustCopy(new Settings(BUFF_SIZE, WAIT_TIME));
			copy.forEach(c -> {
				rc.copy(c.sourceFile(), c.targetFile());
				System.out.println();
			});
		}
	}
}

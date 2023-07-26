package app;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import app.Analyse.Result;

public class App {

	public static final int BUFF_SIZE = 1024 * 1024 * 1;
	public static final int WAIT_TIME = 10;

	public static boolean dryRun = false;
	public static boolean overwrite = false;

	private static record Copy(Path fromFile, Path toFile) {
		@Override
		public String toString() {
			return fromFile.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("= = = = Copy Tool = = = =");
		System.out.println();

		Path fromDir = null;
		Path toDir = null;

		for (String arg : args) {
			if (arg.startsWith("-")) {
				for (int i = 1; i < arg.length(); i++) {
					switch (arg.charAt(i)) {
					case 'd' -> App.dryRun = true;
					case 'b' -> Utils.rawBytes = true;
					case 'o' -> App.overwrite = true;
					default -> {
						System.out.println("Invalid parameter: " + arg);
						System.out.println();
						printHelp();
						return;
					}
					}
				}
			} else {
				if (fromDir == null) {
					fromDir = Paths.get(arg).toAbsolutePath().normalize();
				} else if (toDir == null) {
					toDir = Paths.get(arg).toAbsolutePath().normalize();
				}
			}
		}

		if (fromDir != null && toDir != null) {
			findAllFiles(fromDir, toDir);
		} else {
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println("ct [-options] *src* *dst*");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-d    Dry Run, skips file copy");
		System.out.println("-o    Overwrite modified files instead of skipping them.");
		System.out.println("-b    Show all sizes in raw bytes instead of human readable.");
		System.out.println();
	}

	private static void findAllFiles(Path fromDir, Path toDir) throws IOException {
		System.out.println("Copy from: " + fromDir);
		System.out.println("Copy to: " + toDir);
		System.out.println();

		System.out.print("Finding files...");
		List<String> match = new ArrayList<>();
		List<String> noMatch = new ArrayList<>();
		List<Copy> copy = new ArrayList<>();

		Files.walkFileTree(fromDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path fromFile, BasicFileAttributes attrs) throws IOException {
				Path relativize = fromDir.getParent().relativize(fromFile);
				Path toFile = toDir.resolve(relativize);
				Result result = Analyse.files(fromFile, toFile);
				switch (result) {
				case COPY -> copy.add(new Copy(fromFile, toFile));
				case MATCH -> match.add(fromFile.toString());
				case NO_MATCH -> {
					noMatch.add(fromFile.toString());
					if (overwrite) {
						copy.add(new Copy(fromFile, toFile));
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
			RobustCopy rc = new RobustCopy(BUFF_SIZE, WAIT_TIME);
			copy.forEach(c -> {
				rc.copy(c.fromFile(), c.toFile());
				System.out.println();
			});
		}
	}
}

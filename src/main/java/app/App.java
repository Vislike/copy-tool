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

	private static record Copy(Path fromFile, Path toFile) {
		@Override
		public String toString() {
			return fromFile.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("= = = = File Copy Tool = = = =");
		System.out.println();

		if (args.length != 2) {
			System.out.println("Usage: copytool *source dir/file* *destination dir*");
		} else {

			Path fromDir = Paths.get(args[0]).toAbsolutePath().normalize();
			Path toDir = Paths.get(args[1]).toAbsolutePath().normalize();

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
					case NO_MATCH -> noMatch.add(fromFile.toString());
					}
					return FileVisitResult.CONTINUE;
				}
			});
			System.out.println("complete");
			System.out.println();

			if (!match.isEmpty()) {
				System.out.println("+ + + + Existing files that match in size and modify date + + + +");
				match.forEach(System.out::println);
				System.out.println();
			}

			if (!noMatch.isEmpty()) {
				System.out.println("- - - - Modified files, need manual attention - - - -");
				noMatch.forEach(System.out::println);
				System.out.println();
			}

			if (!copy.isEmpty()) {
				System.out.println("* * * * Files to Copy * * * *");
				copy.forEach(System.out::println);
				System.out.println();
			}

			copy.forEach(c -> {
				RobustCopy.robustCopy(c.fromFile(), c.toFile(), BUFF_SIZE);
				System.out.println();
			});

		}
	}
}

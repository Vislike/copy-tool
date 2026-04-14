package ct.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import ct.files.meta.FileRecord;
import ct.files.meta.Settings;

public class Analyse {

	private Analyse() {
	}

	public static record Copy(FileRecord sourceFile, FileRecord targetFile) {
		@Override
		public String toString() {
			return sourceFile.toString();
		}
	}

	public static record FoundFiles(List<Copy> copy, List<FileRecord> match, List<FileRecord> missmatch) {
		public FoundFiles() {
			this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}
	}

	private static record CheckResult(Status status, long sourceSize, Path sourcePath, Path targetPath) {

		private static enum Status {
			COPY, MATCH, NO_MATCH
		}

		public FileRecord sourceFile() {
			return FileRecord.sourceFile(sourcePath, sourceSize);
		}

		public FileRecord targetFile() {
			return FileRecord.targetFile(targetPath);
		}
	}

	private static CheckResult checkFiles(Path source, Path target) throws IOException {
		CheckResult.Status status;
		long sourceSize = Files.size(source);

		if (Files.notExists(target)) {
			status = CheckResult.Status.COPY;
		} else if (sourceSize == Files.size(target)
				&& Files.getLastModifiedTime(source).equals(Files.getLastModifiedTime(target))) {
			status = CheckResult.Status.MATCH;
		} else {
			status = CheckResult.Status.NO_MATCH;
		}

		return new CheckResult(status, sourceSize, source, target);
	}

	public static FoundFiles findAllFiles(Settings settings) {
		FoundFiles files = new FoundFiles();

		try {
			Files.walkFileTree(settings.sourceDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path sourceFile, BasicFileAttributes attrs) throws IOException {
					final Path relativize = settings.sourceDir().getParent().relativize(sourceFile);
					final Path targetFile = settings.targetDir().resolve(relativize);

					CheckResult res = checkFiles(sourceFile, targetFile);

					switch (res.status()) {
					case COPY -> files.copy.add(new Copy(res.sourceFile(), res.targetFile()));
					case MATCH -> files.match.add(res.sourceFile());
					case NO_MATCH -> {
						files.missmatch.add(res.sourceFile());
						if (settings.overwrite()) {
							files.copy.add(new Copy(res.sourceFile(), res.targetFile()));
						}
					}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return files;
	}
}

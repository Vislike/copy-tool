package ct.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import ct.files.metadata.AnalyseResult;
import ct.files.metadata.CopyTask;
import ct.files.metadata.FileRecord;
import ct.files.metadata.Settings;

public class Analyse {

	private Analyse() {
	}

	private static enum Status {
		COPY, MATCH, MISSMATCH
	}

	private static record FilesResult(Status status, long sourceSize, Path sourcePath, Path targetPath) {

		public FileRecord sourceFile() {
			return FileRecord.sourceFile(sourcePath, sourceSize);
		}

		public FileRecord targetFile() {
			return FileRecord.targetFile(targetPath);
		}
	}

	private static FilesResult filesStatus(Path source, Path target) throws IOException {
		Status status;
		long sourceSize = Files.size(source);

		if (Files.notExists(target)) {
			status = Status.COPY;
		} else if (sourceSize == Files.size(target)
				&& Files.getLastModifiedTime(source).equals(Files.getLastModifiedTime(target))) {
			status = Status.MATCH;
		} else {
			status = Status.MISSMATCH;
		}

		return new FilesResult(status, sourceSize, source, target);
	}

	public static AnalyseResult findAllFiles(Settings settings) {
		AnalyseResult files = new AnalyseResult();

		try {
			Files.walkFileTree(settings.sourceDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path sourceFile, BasicFileAttributes attrs) throws IOException {
					final Path relativize = settings.sourceDir().getParent().relativize(sourceFile);
					final Path targetFile = settings.targetDir().resolve(relativize);

					FilesResult res = filesStatus(sourceFile, targetFile);

					switch (res.status()) {
					case COPY -> files.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
					case MATCH -> files.match().add(res.sourceFile());
					case MISSMATCH -> {
						files.missmatch().add(res.sourceFile());
						if (settings.overwrite()) {
							files.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
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

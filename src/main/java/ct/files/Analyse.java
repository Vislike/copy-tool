package ct.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import ct.app.Settings.AnalyseSettings;
import ct.files.types.AnalyseResult;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;

public class Analyse {

	private Analyse() {
	}

	private static enum Status {
		COPY, MATCH, MISMATCH
	}

	private static record FilesResult(Status status, long sourceSize, Path sourcePath, long targetSize, Path targetPath,
			Path relativeFromSource) {

		public FileRecord sourceFile() {
			return FileRecord.sourceFile(sourcePath, sourceSize, relativeFromSource);
		}

		public FileRecord targetFile() {
			return FileRecord.targetFile(targetPath);
		}

		public FileRecord resumeSource() {
			return FileRecord.resumeSource(sourcePath, sourceSize, targetSize, relativeFromSource);
		}
	}

	private static FilesResult filesStatus(Path source, Path target, Path relativeFromSource) throws IOException {
		Status status;
		long sourceSize = Files.size(source);
		long targetSize = -1;

		if (Files.notExists(target)) {
			status = Status.COPY;
		} else {
			targetSize = Files.size(target);
			if (sourceSize == targetSize
					&& Files.getLastModifiedTime(source).equals(Files.getLastModifiedTime(target))) {
				status = Status.MATCH;
			} else {
				status = Status.MISMATCH;
			}
		}

		return new FilesResult(status, sourceSize, source, targetSize, target, relativeFromSource);
	}

	public static AnalyseResult findAllFiles(AnalyseSettings settings) {
		AnalyseResult files = new AnalyseResult();

		try {
			Files.walkFileTree(settings.sourceDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path sourceFile, BasicFileAttributes attrs) throws IOException {
					final Path relativeFromSource = settings.sourceDir().getParent().relativize(sourceFile);
					final Path targetFile = settings.targetDir().resolve(relativeFromSource);

					FilesResult res = filesStatus(sourceFile, targetFile, relativeFromSource);

					switch (res.status()) {
					case COPY -> files.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
					case MATCH -> files.match().add(res.sourceFile());
					case MISMATCH -> {
						files.mismatch().add(res.sourceFile());
						if (settings.overwrite()) {
							files.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
						} else if (settings.resume()) {
							files.copy().add(new CopyTask(res.resumeSource(), res.targetFile()));
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

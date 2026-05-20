package ct.action;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import ct.action.copy.model.CopyTask;
import ct.action.copy.model.FileRecord;
import ct.app.Settings.AnalyseSettings;

public class AnalyseAction {

	private AnalyseAction() {
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
		AnalyseResult result = new AnalyseResult();

		try {
			Files.walkFileTree(settings.sourceDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path sourceFile, BasicFileAttributes attrs) throws IOException {
					final Path relativeFromSource = settings.sourceDir().getParent().relativize(sourceFile);
					final Path targetFile = settings.targetDir().resolve(relativeFromSource);

					FilesResult res = filesStatus(sourceFile, targetFile, relativeFromSource);

					switch (res.status()) {
					case COPY -> result.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
					case MATCH -> result.match().add(res.sourceFile());
					case MISMATCH -> {
						result.mismatch().add(res.sourceFile());
						if (settings.overwrite()) {
							result.copy().add(new CopyTask(res.sourceFile(), res.targetFile()));
						} else if (settings.resume()) {
							result.copy().add(new CopyTask(res.resumeSource(), res.targetFile()));
						}
					}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return result;
	}
}

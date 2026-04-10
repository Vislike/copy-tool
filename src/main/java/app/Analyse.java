package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import copy.meta.FileRecord;

public class Analyse {

	public static record Result(Status status, long sourceSize, Path sourcePath, Path targetPath) {

		public FileRecord sourceFile() {
			return FileRecord.sourceFile(sourcePath, sourceSize);
		}

		public FileRecord targetFile() {
			return FileRecord.targetFile(targetPath);
		}
	}

	public static enum Status {
		COPY, MATCH, NO_MATCH
	}

	public static Result files(Path source, Path target) throws IOException {

		long sourceSize = Files.size(source);
		Status status;

		if (Files.notExists(target)) {
			status = Status.COPY;
		} else if (sourceSize == Files.size(target)
				&& Files.getLastModifiedTime(source).equals(Files.getLastModifiedTime(target))) {
			status = Status.MATCH;
		} else {
			status = Status.NO_MATCH;
		}

		return new Result(status, sourceSize, source, target);
	}
}

package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Analyse {

	public static enum Result {
		COPY, MATCH, NO_MATCH
	}

	public static Result files(Path from, Path to) throws IOException {
		if (Files.notExists(to)) {
			return Result.COPY;
		}

		if (Files.size(from) == Files.size(to)
				&& Files.getLastModifiedTime(from).equals(Files.getLastModifiedTime(to))) {
			return Result.MATCH;
		}

		return Result.NO_MATCH;
	}
}

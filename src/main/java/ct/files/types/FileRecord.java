package ct.files.types;

import java.nio.file.Path;

import ct.utils.Utils;

public record FileRecord(Path path, long size, long position, Path relativeFromSource) {

	private static final long NO_SIZE = -1;

	public static FileRecord sourceFile(Path path, long size, Path relativeFromSource) {
		return new FileRecord(path, size, 0, relativeFromSource);
	}

	public static FileRecord targetFile(Path path) {
		return new FileRecord(path, NO_SIZE, 0, null);
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();

		if (relativeFromSource == null) {
			sb.append(path);
		} else {
			sb.append(relativeFromSource);
		}

		if (size != NO_SIZE) {
			sb.append(" (").append(Utils.size(size)).append(")");
		}

		return sb.toString();
	}
}

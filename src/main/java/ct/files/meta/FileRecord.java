package ct.files.meta;

import java.nio.file.Path;

import ct.utils.Utils;

public record FileRecord(Path path, long position, long size) {

	private static final long NO_SIZE = -1;

	public static FileRecord sourceFile(Path path, long size) {
		return new FileRecord(path, 0, size);
	}

	public static FileRecord targetFile(Path path) {
		return new FileRecord(path, 0, NO_SIZE);
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(path);

		if (size != NO_SIZE) {
			sb.append(" (").append(Utils.size(size)).append(")");
		}

		return sb.toString();
	}
}

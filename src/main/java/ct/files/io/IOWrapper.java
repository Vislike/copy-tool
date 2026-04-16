package ct.files.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public interface IOWrapper {

	// Files

	Path createDirectories(Path path) throws IOException;

	FileTime getLastModifiedTime(Path path) throws IOException;

	Path setLastModifiedTime(Path path, FileTime time) throws IOException;

	// FileChannel

	FileChannel open(Path path, OpenOption... options) throws IOException;

	FileChannel position(FileChannel channel, long newPosition) throws IOException;

	int read(FileChannel channel, ByteBuffer dst) throws IOException;

	int write(FileChannel channel, ByteBuffer src) throws IOException;

	long size(FileChannel channel) throws IOException;

	FileChannel truncate(FileChannel channel, long size) throws IOException;
}

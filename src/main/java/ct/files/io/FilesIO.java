package ct.files.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FilesIO implements IOWrapper {

	@Override
	public Path createDirectories(Path path) throws IOException {
		return Files.createDirectories(path);
	}

	@Override
	public FileTime getLastModifiedTime(Path path) throws IOException {
		return Files.getLastModifiedTime(path);
	}

	@Override
	public Path setLastModifiedTime(Path path, FileTime time) throws IOException {
		return Files.setLastModifiedTime(path, time);
	}

	@Override
	public FileChannel open(Path path, OpenOption... options) throws IOException {
		return FileChannel.open(path, options);
	}

	@Override
	public FileChannel position(FileChannel channel, long newPosition) throws IOException {
		return channel.position(newPosition);
	}

	@Override
	public int read(FileChannel channel, ByteBuffer dst) throws IOException {
		return channel.read(dst);
	}

	@Override
	public int write(FileChannel channel, ByteBuffer src) throws IOException {
		return channel.write(src);
	}

	@Override
	public long size(FileChannel channel) throws IOException {
		return channel.size();
	}

	@Override
	public FileChannel truncate(FileChannel channel, long size) throws IOException {
		return channel.truncate(size);
	}
}

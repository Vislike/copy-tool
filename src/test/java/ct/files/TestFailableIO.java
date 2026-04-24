package ct.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;

public class TestFailableIO implements IOWrapper {

	private final IOWrapper io;
	private final int count[];
	private final int failAt[];
	private int writeOneLessByteAt = 0;

	TestFailableIO() {
		io = new FilesIO();
		count = new int[TT.values().length];
		failAt = new int[TT.values().length];
	}

	enum TT {
		createDirectories, getLastModifiedTime, setLastModifiedTime, open, position, read, write, size, truncate, close
	};

	int count(TT t) {
		return count[t.ordinal()];
	}

	TestFailableIO failAt(TT t, int n) {
		failAt[t.ordinal()] = n;
		return this;
	}

	TestFailableIO writeOneLessAt(int n) {
		writeOneLessByteAt = n;
		return this;
	}

	private void incCoundAndCheckFail(TT t) throws IOException {
		int i = t.ordinal();
		count[i]++;
		if (failAt[i] == count[i]) {
			throw new IOException("Test IOException thrown by: " + t.name() + " (" + count[i] + ")");
		}
	}

	@Override
	public Path createDirectories(Path path) throws IOException {
		incCoundAndCheckFail(TT.createDirectories);
		return io.createDirectories(path);
	}

	@Override
	public FileTime getLastModifiedTime(Path path) throws IOException {
		incCoundAndCheckFail(TT.getLastModifiedTime);
		return io.getLastModifiedTime(path);
	}

	@Override
	public Path setLastModifiedTime(Path path, FileTime time) throws IOException {
		incCoundAndCheckFail(TT.setLastModifiedTime);
		return io.setLastModifiedTime(path, time);
	}

	@Override
	public FileChannel open(Path path, OpenOption... options) throws IOException {
		incCoundAndCheckFail(TT.open);
		return io.open(path, options);
	}

	@Override
	public FileChannel position(FileChannel channel, long newPosition) throws IOException {
		incCoundAndCheckFail(TT.position);
		return io.position(channel, newPosition);
	}

	@Override
	public int read(FileChannel channel, ByteBuffer dst) throws IOException {
		incCoundAndCheckFail(TT.read);
		return io.read(channel, dst);
	}

	@Override
	public int write(FileChannel channel, ByteBuffer src) throws IOException {
		incCoundAndCheckFail(TT.write);
		int testError = writeOneLessByteAt == count(TT.write) ? 1 : 0;
		return io.write(channel, src) - testError;
	}

	@Override
	public long size(FileChannel channel) throws IOException {
		incCoundAndCheckFail(TT.size);
		return io.size(channel);
	}

	@Override
	public FileChannel truncate(FileChannel channel, long size) throws IOException {
		incCoundAndCheckFail(TT.truncate);
		return io.truncate(channel, size);
	}

	@Override
	public void close(FileChannel channel) throws IOException {
		incCoundAndCheckFail(TT.close);
		io.close(channel);
	}
}

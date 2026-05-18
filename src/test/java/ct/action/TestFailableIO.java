package ct.action;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import ct.action.io.FilesIO;
import ct.action.io.IOWrapper;
import ct.app.App;
import ct.util.Utils;

public class TestFailableIO implements IOWrapper {

	private final IOWrapper io;
	private final int count[];
	private final int failAt[];
	private final int corruptAt[];
	private int writeOneLessByteAt = 0;
	private int writeZeroAt = 0;
	private int readZeroAt = 0;
	private int readEofAt = 0;

	TestFailableIO() {
		io = new FilesIO();
		count = new int[WT.values().length];
		failAt = new int[WT.values().length];
		corruptAt = new int[WT.values().length];
	}

	int count(WT t) {
		return count[t.ordinal()];
	}

	TestFailableIO failAt(WT t, int n) {
		failAt[t.ordinal()] = n;
		return this;
	}

	TestFailableIO corruptAt(WT t, int n) {
		corruptAt[t.ordinal()] = n;
		return this;
	}

	TestFailableIO writeOneLessAt(int n) {
		writeOneLessByteAt = n;
		return this;
	}

	TestFailableIO writeZeroAt(int n) {
		writeZeroAt = n;
		return this;
	}

	TestFailableIO readZeroAt(int n) {
		readZeroAt = n;
		return this;
	}

	TestFailableIO readEofAt(int n) {
		readEofAt = n;
		return this;
	}

	private void incCoundAndCheckFail(WT t) throws IOException {
		int i = t.ordinal();
		count[i]++;
		if (failAt[i] == count[i]) {
			throw new IOException("Test IOException thrown by: " + t.name() + " (" + count[i] + ")");
		}
	}

	@Override
	public Path createDirectories(Path path) throws IOException {
		incCoundAndCheckFail(WT.createDirectories);
		return io.createDirectories(path);
	}

	@Override
	public FileTime getLastModifiedTime(Path path) throws IOException {
		incCoundAndCheckFail(WT.getLastModifiedTime);
		return io.getLastModifiedTime(path);
	}

	@Override
	public Path setLastModifiedTime(Path path, FileTime time) throws IOException {
		incCoundAndCheckFail(WT.setLastModifiedTime);
		return io.setLastModifiedTime(path, time);
	}

	@Override
	public FileChannel open(Path path, OpenOption... options) throws IOException {
		incCoundAndCheckFail(WT.open);
		return io.open(path, options);
	}

	@Override
	public FileChannel position(FileChannel channel, long newPosition) throws IOException {
		incCoundAndCheckFail(WT.position);
		return io.position(channel, newPosition);
	}

	@Override
	public int read(FileChannel channel, ByteBuffer dst) throws IOException {
		incCoundAndCheckFail(WT.read);
		int read = io.read(channel, dst);
		if (corruptAt[WT.read.ordinal()] == count[WT.read.ordinal()]) {
			dst.put(0, (byte) 0);
			if (RobustCopyIT.OUTPUT_VISIBLE) {
				App.verbose("Corrupting read at", Utils.size(channel.position() - read));
			}
		}
		if (readZeroAt == count(WT.read)) {
			return 0;
		} else if (readEofAt == count(WT.read)) {
			return -1;
		}
		return read;
	}

	@Override
	public int write(FileChannel channel, ByteBuffer src) throws IOException {
		incCoundAndCheckFail(WT.write);
		if (corruptAt[WT.write.ordinal()] == count[WT.write.ordinal()]) {
			if (RobustCopyIT.OUTPUT_VISIBLE) {
				App.verbose("Corrupting write at", Utils.size(channel.position()));
			}
			src.put(1, (byte) 0);
		}
		int write = io.write(channel, src);
		if (writeOneLessByteAt == count(WT.write)) {
			return write - 1;
		} else if (writeZeroAt == count(WT.write)) {
			return 0;
		}
		return write;
	}

	@Override
	public long transferTo(FileChannel source, long position, long count, FileChannel target) throws IOException {
		incCoundAndCheckFail(WT.transferTo);
		return io.transferTo(source, position, count, target);
	}

	@Override
	public long size(FileChannel channel) throws IOException {
		incCoundAndCheckFail(WT.size);
		return io.size(channel);
	}

	@Override
	public FileChannel truncate(FileChannel channel, long size) throws IOException {
		incCoundAndCheckFail(WT.truncate);
		return io.truncate(channel, size);
	}

	@Override
	public void close(FileChannel channel) throws IOException {
		incCoundAndCheckFail(WT.close);
		io.close(channel);
	}
}

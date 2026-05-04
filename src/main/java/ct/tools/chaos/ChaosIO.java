package ct.tools.chaos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.util.Random;

import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;

public class ChaosIO implements IOWrapper {

	private static final int SCALE = 10_000;
	private static final DecimalFormat df = new DecimalFormat("#.#####%");

	private final int chance;
	private final Random rand;
	private final IOWrapper io = new FilesIO();

	public ChaosIO(int chance, long seed) {
		this.chance = chance;
		this.rand = new Random(seed);
	}

	private void chaos(WT t) throws IOException {
		int roll = rand.nextInt(SCALE);
		if (roll < chance) {
			if (rand.nextInt(10) < 1) {
				throw new IOException("""
						Complete chaos errupted\r\n
						What should we dooooooo(m)?????\r\n
						This is many lines, and huge,
						Maybe a stack trace, formating chaos ensure.
						Maybe a stack trace, formating chaos ensure.
						Maybe a stack trace, formating chaos ensure.
						""");
			} else {
				throw new IOException("Chaos rolled: " + (roll + 1) + "/" + SCALE + ", during \"" + t
						+ "\", your odds: " + df.format((double) chance / (double) (SCALE)));
			}
		}
	}

	@Override
	public Path createDirectories(Path path) throws IOException {
		chaos(WT.createDirectories);
		return io.createDirectories(path);
	}

	@Override
	public FileTime getLastModifiedTime(Path path) throws IOException {
		chaos(WT.getLastModifiedTime);
		return io.getLastModifiedTime(path);
	}

	@Override
	public Path setLastModifiedTime(Path path, FileTime time) throws IOException {
		chaos(WT.setLastModifiedTime);
		return io.setLastModifiedTime(path, time);
	}

	@Override
	public FileChannel open(Path path, OpenOption... options) throws IOException {
		chaos(WT.open);
		return io.open(path, options);
	}

	@Override
	public FileChannel position(FileChannel channel, long newPosition) throws IOException {
		chaos(WT.position);
		return io.position(channel, newPosition);
	}

	@Override
	public int read(FileChannel channel, ByteBuffer dst) throws IOException {
		chaos(WT.read);
		return io.read(channel, dst);
	}

	@Override
	public int write(FileChannel channel, ByteBuffer src) throws IOException {
		chaos(WT.write);
		return io.write(channel, src);
	}

	@Override
	public long size(FileChannel channel) throws IOException {
		chaos(WT.size);
		return io.size(channel);
	}

	@Override
	public FileChannel truncate(FileChannel channel, long size) throws IOException {
		chaos(WT.truncate);
		return io.truncate(channel, size);
	}

	@Override
	public void close(FileChannel channel) throws IOException {
		chaos(WT.close);
		io.close(channel);
	}

}

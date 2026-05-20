package ct.support.chaos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.util.Random;

import ct.action.io.FilesIO;
import ct.action.io.IOWrapper;

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
			String msg = "Chaos rolled: " + (roll + 1) + "/" + SCALE + ", during \"" + t + "\", your odds: "
					+ df.format((double) chance / (double) (SCALE));

			int randNum = rand.nextInt(10);
			if (randNum < 1) {
				msg += """
						... Complete chaos errupted\r\n
						What should we dooooooo(m)?????\r\n
						This is many lines, and huge,
						Maybe a stack trace, formating chaos ensure.
						Maybe a stack trace, formating chaos ensure.
						Maybe a stack trace, formating chaos ensure.
						""";
			} else if (randNum < 2) {
				throw new NoSuchFileException(msg);
			}
			throw new IOException(msg);
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
		try {
			chaos(WT.read);
		} catch (IOException e) {
			int i = rand.nextInt(10);
			return switch (i) {
			case 0 -> -1;
			case 1 -> 0;
			default -> throw e;
			};
		}
		return io.read(channel, dst);
	}

	@Override
	public int write(FileChannel channel, ByteBuffer src) throws IOException {
		int writeError = 0;
		try {
			chaos(WT.write);
		} catch (IOException e) {
			int i = rand.nextInt(10);
			switch (i) {
			case 0 -> writeError = rand.nextInt(Math.max(src.limit(), 1));
			case 1 -> {
				return 0;
			}
			default -> throw e;
			}
		}
		return io.write(channel, src) - writeError;
	}

	@Override
	public long transferTo(FileChannel source, long position, long count, FileChannel target) throws IOException {
		long transferError = 0;
		try {
			chaos(WT.transferTo);
		} catch (IOException e) {
			int i = rand.nextInt(10);
			switch (i) {
			case 0 -> transferError = rand.nextLong(Math.max(count, 1));
			case 1 -> {
				return 0;
			}
			default -> throw e;
			}
		}
		return io.transferTo(source, position, count, target) - transferError;
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

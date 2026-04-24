package ct.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import ct.app.App;
import ct.files.metadata.FileRecord;
import ct.utils.TestUtils;
import ct.utils.Utils;
import ct.utils.Utils.Timer;

public class GenTestFiles {

	public static final int SIZE = 1024 * 1024 * 512;

	public static void main(String[] args) throws IOException {
		App.infona("= = = = Copy Tool Gen Test Files = = = =");

		if (args.length == 0) {
			App.info("Usage: ct-gen-files *path-to-test-dir*");
			return;
		}

		Path testDir = Paths.get(args[0]);
		if (!Files.isDirectory(testDir)) {
			App.error("Not a directory", args[0]);
			return;
		}
		Path hashFile = testDir.resolve(Shared.HASHES_FILE);
		if (Files.exists(hashFile)) {
			App.error("Hashfile exists, remove it to create new files", hashFile);
			return;
		}

		App.highlight("Test Dir", testDir);
		App.highlight("Filesize", Utils.size(SIZE));
		App.highlight("Hashfile", hashFile);
		App.info();

		Timer timer = Utils.timer();
		Random random = new Random();
		int numBytes = 512;

		try (BufferedWriter hashWriter = Files.newBufferedWriter(hashFile)) {
			// 512 B to 16 MiB
			for (int i = 0; i < 16; i++) {
				Path testFile = testDir.resolve(Shared.nameOfGenFile(numBytes));
				FileRecord fileRecord = FileRecord.sourceFile(testFile, SIZE, testDir.relativize(testFile));
				App.highlight("Creating", fileRecord);
				ByteBuffer bb = randomBb(random, (int) fileRecord.size());
				Files.write(fileRecord.path(), bb.array());
				hashWriter.append(hash(bb, fileRecord));

				numBytes *= 2;
			}
		}

		App.infonb(timer.elapsedSeconds("Done in"));
	}

	private static String hash(ByteBuffer bb, FileRecord fileRecord) throws IOException {
		String sha256 = TestUtils.sha256(bb.array());
		return sha256 + " *" + fileRecord.path().getFileName() + '\n';
	}

	private static ByteBuffer randomBb(Random random, int size) {
		ByteBuffer bb = ByteBuffer.allocate(size);
		random.nextBytes(bb.array());
		return bb;
	}
}

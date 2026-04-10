package benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import copy.meta.FileRecord;
import utils.Utils;

public class GenTestFiles {

	public static final int SIZE = 1024 * 1024 * 64;

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println("= = = = Copy Tool Gen Test Files = = = =");
		System.out.println();

		if (args.length == 0) {
			System.out.println("Usage: ct-gen-files *path-to-test-dir*");
			return;
		}

		Path testDir = Paths.get(args[0]);
		if (!Files.isDirectory(testDir)) {
			System.err.println("Not a directory: " + args[0]);
			return;
		}
		Path hashFile = testDir.resolve(Shared.HASHES_FILE);
		if (Files.exists(hashFile)) {
			System.err.println("Dir already contain hashfile, remove it to create new test files: " + hashFile);
			return;
		}

		System.out.println("Test dir for generating files: " + testDir);
		System.out.println("Size of generated files: " + Utils.size(SIZE));
		System.out.println("Hashes file: " + hashFile);
		System.out.println();

		try (BufferedWriter hashWriter = Files.newBufferedWriter(hashFile)) {
			Random random = new Random();

			int numBytes = 512;
			// 512 B to 16 MiB
			for (int i = 0; i < 16; i++) {
				FileRecord fileRecord = FileRecord.sourceFile(testDir.resolve(Shared.nameOfGenFile(numBytes)), SIZE);
				System.out.println("Creating " + fileRecord);
				ByteBuffer bb = randomBb(random, (int) fileRecord.size());
				Files.write(fileRecord.path(), bb.array());
				hashWriter.append(hash(bb, fileRecord));

				numBytes *= 2;
			}
		}

		System.out.println(System.lineSeparator() + "Done.");
	}

	private static String hash(ByteBuffer bb, FileRecord fileRecord) throws NoSuchAlgorithmException {
		String sha256 = Shared.sha256(bb.array());
		return sha256 + " *" + fileRecord.path().getFileName() + '\n';
	}

	private static ByteBuffer randomBb(Random random, int size) {
		ByteBuffer bb = ByteBuffer.allocate(size);
		random.nextBytes(bb.array());
		return bb;
	}
}

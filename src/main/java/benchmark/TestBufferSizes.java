package benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import copy.RobustCopy;
import copy.meta.FileRecord;
import copy.meta.Settings;
import utils.Utils;

public class TestBufferSizes {

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println("= = = = Copy Tool Buffer Test = = = =");
		System.out.println();

		if (args.length == 0) {
			System.out.println("Usage: ct-buffer-test *path-to-test-dir-with-generated-files*");
			return;
		}

		Path testDir = Paths.get(args[0]);
		if (!Files.isDirectory(testDir)) {
			System.err.println("Not a directory: " + args[0]);
			return;
		}
		Path hashFile = testDir.resolve(Shared.HASHES_FILE);
		if (Files.notExists(hashFile)) {
			System.err.println("No hashfile found, generate files first: " + hashFile);
			return;
		}

		Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "ct-buffer-test-temp-file");
		System.out.println("Test dir for reading: " + testDir);
		System.out.println("Temp file for writing: " + tempFile);
		System.out.println("Hashes file: " + hashFile);
		System.out.println();

		FileRecord targetFile = FileRecord.targetFile(tempFile);
		Map<String, String> sha256Map = readHashFileToMap(hashFile);
		List<String> log = new ArrayList<>();

		int numBytes = 512;
		// 512 B to 16 MiB
		for (int i = 0; i < 16; i++) {
			Path testFile = testDir.resolve(Shared.nameOfGenFile(numBytes));
			FileRecord sourceFile = FileRecord.sourceFile(testFile, Files.size(testFile));
			testCopy(sourceFile, targetFile, numBytes, sha256Map, log);

			numBytes *= 2;
		}

		System.out.println();
		log.forEach(System.out::println);
		Files.delete(targetFile.path());
		System.out.println(System.lineSeparator() + "Done.");
	}

	private static void testCopy(FileRecord source, FileRecord target, int numBytes, Map<String, String> sha256Map,
			List<String> log) throws IOException, NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder();
		sb.append("Buffer: ").append(Utils.size(numBytes)).append(", Size: ").append(Utils.size(source.size()));

		long startTime = System.nanoTime();
		RobustCopy robustCopy = new RobustCopy(new Settings(numBytes, 0));
		robustCopy.copy(source, target);
		long elapsedNanos = System.nanoTime() - startTime;

		long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
		String perSec = Utils.size(source.size() * 1000 / ms);
		sb.append(", Time: ").append(ms).append("ms [").append(perSec).append("/s]");

		compareHashes(source, target, sha256Map, sb);
		String status = sb.toString();
		System.out.println(status);
		log.add(status);
	}

	private static void compareHashes(FileRecord source, FileRecord target, Map<String, String> sha256Map,
			StringBuilder sb) throws NoSuchAlgorithmException, IOException {
		String sha256sum = Shared.sha256(Files.readAllBytes(target.path()));
		String storedHash = sha256Map.get(source.path().getFileName().toString());
		sb.append(", Hash: ");
		if (sha256sum.equals(storedHash)) {
			sb.append("ok");
		} else {
			sb.append("Warning <Failed> ").append(sha256sum).append(" != ").append(storedHash).append(" </Failed>");
		}
	}

	private static Map<String, String> readHashFileToMap(Path hashFile) throws IOException {
		Map<String, String> sha256 = new HashMap<>();
		List<String> lines = Files.readAllLines(hashFile);
		for (String line : lines) {
			String[] split = line.split(" +");
			String sha256sum = split[0];
			String filename = split[1];
			if (filename.startsWith("*")) {
				filename = filename.substring(1);
			}
			sha256.put(filename, sha256sum);
		}
		return sha256;
	}
}

package ct.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ct.app.App;
import ct.app.Settings;
import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;
import ct.tui.StdoutPrinter;
import ct.utils.TestUtils;
import ct.utils.Utils;
import ct.utils.Utils.Timer;

public class TestBufferSizes {

	public static void main(String[] args) throws IOException {
		App.info("= = = = Copy Tool Buffer Test = = = =");

		if (args.length == 0) {
			App.infolb("Usage: ct-buffer-test *path-to-test-dir-with-generated-files*");
			return;
		}

		Path testDir = Paths.get(args[0]);
		if (!Files.isDirectory(testDir)) {
			App.error("Not a directory", args[0]);
			return;
		}
		Path hashFile = testDir.resolve(Shared.HASHES_FILE);
		if (Files.notExists(hashFile)) {
			App.error("No hashfile found, generate files first", hashFile);
			return;
		}

		Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "ct-test-buffer-temp-file");
		App.info();
		App.highlight("Test Dir", testDir);
		App.highlight("Tempfile", tempFile);
		App.highlight("Hashfile", hashFile);
		App.info();

		Timer timer = Utils.timer();
		FileRecord targetFile = FileRecord.targetFile(tempFile);
		Map<String, String> sha256Map = readHashFileToMap(hashFile);
		List<String> log = new ArrayList<>();
		int numBytes = 512;

		// 512 B to 16 MiB
		for (int i = 0; i < 16; i++) {
			Path testFile = testDir.resolve(Shared.nameOfGenFile(numBytes));
			FileRecord sourceFile = FileRecord.sourceFile(testFile, Files.size(testFile), testDir.relativize(testFile));
			testCopy(sourceFile, targetFile, numBytes, sha256Map, log);
			App.info();

			numBytes *= 2;
		}

		log.forEach(App::info);
		Files.delete(targetFile.path());

		App.infolb(timer.elapsedSeconds("Done in"));
	}

	private static void testCopy(FileRecord source, FileRecord target, int numBytes, Map<String, String> sha256Map,
			List<String> log) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("Buffer: ").append(Utils.size(numBytes)).append(", Size: ").append(Utils.size(source.size()));

		long startTime = System.nanoTime();
		RobustCopy robustCopy = new RobustCopy(new FilesIO(), Settings.bufferSize(numBytes), new StdoutPrinter());
		robustCopy.copy(new CopyTask(source, target));
		long elapsedNanos = System.nanoTime() - startTime;

		long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
		String perSec = Utils.size(source.size() * 1000 / ms);
		sb.append(", Time: ").append(ms).append("ms [").append(perSec).append("/s]");

		compareHashes(source, target, sha256Map, sb);
		String status = sb.toString();
		App.info(status);
		log.add(status);
	}

	private static void compareHashes(FileRecord source, FileRecord target, Map<String, String> sha256Map,
			StringBuilder sb) throws IOException {
		String sha256sum = TestUtils.sha256(target.path());
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

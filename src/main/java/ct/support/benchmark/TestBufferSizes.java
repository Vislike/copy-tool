package ct.support.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ct.action.RobustCopy;
import ct.action.io.FilesIO;
import ct.action.type.CopyTask;
import ct.action.type.FileRecord;
import ct.app.App;
import ct.app.Settings;
import ct.support.SupportUtils;
import ct.tui.copy.StdoutProgress;
import ct.util.TestUtils;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class TestBufferSizes {

	private static final boolean DEV_MODE = true;

	public static void main(String[] args) throws Exception {
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
		Path hashFile = testDir.resolve(SupportUtils.HASHES_FILE);
		if (Files.notExists(hashFile)) {
			App.error("No hashfile found, generate files first", hashFile);
			return;
		}

		Settings.devMode = DEV_MODE;
		Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "ct-test-buffer-temp-file");
		App.info();
		App.highlight("Test Dir ", testDir);
		App.highlight("Temp File", tempFile);
		App.highlight("Hash File", hashFile);
		App.highlight("Dev Mode ", Settings.devMode);
		App.info();

		Timer timer = Utils.timer();
		FileRecord targetFile = FileRecord.targetFile(tempFile);
		Map<String, String> sha256Map = SupportUtils.readHashFileToMap(hashFile);
		List<String> log = new ArrayList<>();

		for (int numBytes : SupportUtils.bytesList()) {
			SupportUtils.waitBetweenTests();

			Path testFile = testDir.resolve(SupportUtils.nameOfGenFile(numBytes));
			FileRecord sourceFile = FileRecord.sourceFile(testFile, Files.size(testFile), testDir.relativize(testFile));
			testCopy(sourceFile, targetFile, numBytes, sha256Map, log);

			App.info();
			Files.delete(targetFile.path());
		}

		log.forEach(App::info);

		App.infolb(timer.elapsedSeconds("Done in"));
	}

	private static void testCopy(FileRecord source, FileRecord target, int numBytes, Map<String, String> sha256Map,
			List<String> log) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("Buffer: ").append(Utils.size(numBytes)).append(" (").append(Integer.numberOfTrailingZeros(numBytes))
				.append("), Size: ").append(Utils.size(source.size()));

		long startTime = System.nanoTime();
		RobustCopy robustCopy = new RobustCopy(new FilesIO(), Settings.testBufferSizes(numBytes).robustCopy(),
				new StdoutProgress());
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
}

package ct.tools.chaos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ct.app.App;
import ct.app.Settings;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;
import ct.tools.Shared;
import ct.tui.MultiFileCopy;
import ct.utils.TestUtils;
import ct.utils.Utils;
import ct.utils.Utils.Timer;

public class ChaosTest {

	private static final int BUFF_SIZE = 512 << 8;
	private static final int ROLLBACK = 0;
	private static final int WAIT_RETRY = 1;
	private static final int FILES_AT_TIME = 4;
	private static final int CHAOS_CHANCE = 5;

	public static void main(String[] args) throws IOException {
		App.info("= = = = Copy Tool Chaos Test = = = =");

		if (args.length == 0) {
			App.infolb("Usage: ct-chaos-test *path-to-test-dir-with-generated-files*");
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

		Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "ct-test-chaos-temp-dir");
		App.info();
		App.highlight("Test Dir", testDir);
		App.highlight("Temp Dir", tempDir);
		App.highlight("Hashfile", hashFile);
		App.highlight("Buffsize", BUFF_SIZE);
		App.highlight("Rollback", ROLLBACK);
		App.highlight("WaitTime", WAIT_RETRY);
		App.highlight("NumFiles", FILES_AT_TIME);
		App.highlight("ChaosVal", CHAOS_CHANCE);

		Timer timer = Utils.timer();
		Map<String, String> sha256Map = Shared.readHashFileToMap(hashFile);
		List<CopyTask> tasks = new ArrayList<>();

		for (int numBytes : Shared.bytesList()) {
			String fileName = Shared.nameOfGenFile(numBytes);
			Path testFile = testDir.resolve(fileName);
			FileRecord sourceFile = FileRecord.sourceFile(testFile, Files.size(testFile), testDir.relativize(testFile));
			FileRecord targetFile = FileRecord.targetFile(tempDir.resolve(fileName));
			tasks.add(new CopyTask(sourceFile, targetFile));
		}

		new MultiFileCopy(Settings.numFiles(BUFF_SIZE, ROLLBACK, WAIT_RETRY, FILES_AT_TIME), new ChaosIO(CHAOS_CHANCE))
				.copyAll(tasks);

		App.infolb("Verifying files");
		for (CopyTask task : tasks) {
			String fileName = task.targetFile().path().getFileName().toString();
			App.infonn(fileName);
			String sha256sum = TestUtils.sha256(task.targetFile().path());
			String storedHash = sha256Map.get(fileName);
			if (sha256sum.equals(storedHash)) {
				App.info(" ok");
			} else {
				App.recoverError(" Warning Failed", sha256sum + " != " + storedHash);
			}
		}

		App.infolb("Removing temp files");
		deleteFiles(tempDir);

		App.infolb(timer.elapsedSeconds("Done in"));
	}

	private static void deleteFiles(Path tempDir) throws IOException {
		try (Stream<Path> s = Files.walk(tempDir)) {
			s.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}
}

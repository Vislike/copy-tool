package ct.support.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ct.action.RobustCopy;
import ct.action.io.FilesIO;
import ct.action.type.CopyTask;
import ct.action.type.FileRecord;
import ct.app.App;
import ct.app.Settings;
import ct.support.SupportUtils;
import ct.tui.StdoutPrinter;
import ct.util.Utils;
import ct.util.Utils.Timer;

public class TestLargeFile {

	private static final boolean DEV_MODE = true;
	private static final int BUFF_FROM = 19;
	private static final int BUFF_TO = 25;

	public static void main(String[] args) throws IOException {
		App.info("= = = = Copy Tool Large File Test = = = =");

		if (args.length == 0) {
			App.infolb("Usage: ct-largefile-test *path-to-largefile*");
			return;
		}

		Path testFile = Paths.get(args[0]);
		if (!Files.exists(testFile)) {
			App.error("File does not exists", args[0]);
			return;
		}

		if (!Files.isRegularFile(testFile)) {
			App.error("Not a file", args[0]);
			return;
		}

		Settings.devMode = DEV_MODE;
		Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "ct-test-largefile-temp-file");
		FileRecord sourceFile = FileRecord.sourceFile(testFile, Files.size(testFile), testFile.getFileName());
		App.info();
		App.highlight("Test File", sourceFile.path());
		App.highlight("File Size", Utils.size(sourceFile.size()));
		App.highlight("Temp File", tempFile);
		App.highlight("Buff From", Utils.size(1 << BUFF_FROM));
		App.highlight("Buff To  ", Utils.size(1 << BUFF_TO));
		App.highlight("Num Tests", BUFF_TO - BUFF_FROM + 1);
		App.highlight("Dev Mode ", Settings.devMode);
		App.info();

		Timer timer = Utils.timer();
		FileRecord targetFile = FileRecord.targetFile(tempFile);
		List<String> log = new ArrayList<>();

		for (int i = BUFF_FROM; i <= BUFF_TO; i++) {
			SupportUtils.waitBetweenTests();

			testCopy(sourceFile, targetFile, 1 << i, log);

			App.info();
			Files.delete(targetFile.path());
		}

		log.forEach(App::info);

		App.infolb(timer.elapsedSeconds("Done in"));
	}

	private static void testCopy(FileRecord source, FileRecord target, int buff, List<String> log) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("Buffer: ").append(Utils.size(buff)).append(" (").append(Integer.numberOfTrailingZeros(buff))
				.append("), Size: ").append(Utils.size(source.size()));

		long startTime = System.nanoTime();
		RobustCopy robustCopy = new RobustCopy(new FilesIO(), Settings.testBufferSizes(buff).robustCopy(),
				new StdoutPrinter());
		robustCopy.copy(new CopyTask(source, target));
		long elapsedNanos = System.nanoTime() - startTime;

		long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
		String perSec = Utils.size(source.size() * 1000 / ms);
		sb.append(", Time: ").append(ms).append("ms [").append(perSec).append("/s]");
		String status = sb.toString();
		App.info(status);
		log.add(status);
	}
}

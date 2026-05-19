package ct.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ct.app.App;
import ct.util.Utils;

public class SupportUtils {

	private static final int WAIT_BETWEEN_TEST_TIME = 1000;

	private SupportUtils() {
	}

	public static final String HASHES_FILE = "sha256sums.txt";

	public static String nameOfGenFile(int numBytes) {
		return "ct-buffer-test-" + Utils.size(numBytes).replace(' ', '-') + ".bin";
	}

	public static List<Integer> bytesList() {
		List<Integer> result = new ArrayList<>();

		int numBytes = 1024;
		// 16 files
		for (int i = 0; i < 16; i++) {
			result.add(numBytes);

			numBytes <<= 1;
		}

		return result;
	}

	public static Map<String, String> readHashFileToMap(Path hashFile) throws IOException {
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

	public static void waitBetweenTests() {
		App.info("Wait between tests " + Duration.ofMillis(WAIT_BETWEEN_TEST_TIME).toSeconds() + "s...");
		try {
			Thread.sleep(Duration.ofMillis(WAIT_BETWEEN_TEST_TIME));
		} catch (InterruptedException e) {
			// Ignore
		}
	}
}

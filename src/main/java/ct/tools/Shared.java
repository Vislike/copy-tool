package ct.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ct.utils.Utils;

public class Shared {

	private Shared() {
	}

	public static final String HASHES_FILE = "sha256sums.txt";

	public static String nameOfGenFile(int numBytes) {
		return "ct-buffer-test-" + Utils.size(numBytes).replace(' ', '-') + ".bin";
	}

	public static List<Integer> bytesList() {
		List<Integer> result = new ArrayList<>();

		int numBytes = 512;
		// 512 B to 16 MiB
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
}

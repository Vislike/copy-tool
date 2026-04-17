package ct.benchmark;

import ct.utils.Utils;

public class Shared {

	public static final String HASHES_FILE = "sha256sums.txt";

	public static String nameOfGenFile(int numBytes) {
		return "ct-buffer-test-" + Utils.size(numBytes).replace(' ', '-') + ".bin";
	}
}

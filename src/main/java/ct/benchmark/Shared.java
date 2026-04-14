package ct.benchmark;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ct.utils.Utils;

public class Shared {

	public static final String HASHES_FILE = "sha256sums.txt";

	public static String nameOfGenFile(int numBytes) {
		return "ct-buffer-test-" + Utils.size(numBytes).replace(' ', '-') + ".bin";
	}

	/**
	 * Helper for HEX converting
	 */
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

	/**
	 * Converts an array of bytes to a hexadecimal string representation
	 * 
	 * @param bytes array of bytes
	 * @return string with lower case hexadecimal
	 */
	public static String bytesToHex(byte[] bytes) {
		final int l = bytes.length;
		char[] hexChars = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			hexChars[j++] = HEX_ARRAY[(bytes[i] & 0xF0) >>> 4];
			hexChars[j++] = HEX_ARRAY[(bytes[i] & 0x0F)];
		}
		return new String(hexChars);
	}

	public static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return bytesToHex(digest.digest(bytes));
	}
}

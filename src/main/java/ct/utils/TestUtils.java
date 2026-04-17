package ct.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TestUtils {
	private TestUtils() {
	}

	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		final int l = bytes.length;
		char[] hexChars = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			hexChars[j++] = HEX_ARRAY[(bytes[i] & 0xF0) >>> 4];
			hexChars[j++] = HEX_ARRAY[(bytes[i] & 0x0F)];
		}
		return new String(hexChars);
	}

	public static String sha256(byte[] bytes) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return bytesToHex(digest.digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static String sha256(Path path) throws IOException {
		return sha256(Files.readAllBytes(path));
	}
}

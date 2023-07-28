package app;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class Utils {
	private Utils() {
	}

	public static boolean rawBytes = false;

	public static String time(long seconds) {
		StringBuilder sb = new StringBuilder();
		if (seconds >= 60) {
			long minutes = seconds / 60;
			seconds %= 60;

			if (minutes >= 60) {
				long hours = minutes / 60;
				minutes %= 60;

				sb.append(hours + "h ");
			}
			sb.append(minutes + "m ");
		}
		sb.append(seconds + "s");
		return sb.toString();
	}

	public static String size(long bytes) {
		if (rawBytes) {
			return bytes + " B";
		}
		return humanReadableByteCountBin(bytes);
	}

	// From:
	// https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
	private static String humanReadableByteCountBin(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format("%.1f %ciB", value / 1024.0, ci.current());
	}
}

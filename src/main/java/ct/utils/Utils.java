package ct.utils;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.concurrent.TimeUnit;

import ct.app.Settings;

public class Utils {

	public static final int SB_SIZE = 128;

	private Utils() {
	}

	public static String timeDuration(long seconds) {
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

	public static String timeClock(long seconds) {
		long minutes = seconds / 60;
		seconds %= 60;

		if (minutes >= 60) {
			long hours = minutes / 60;
			minutes %= 60;

			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%d:%02d", minutes, seconds);
	}

	public static String size(long bytes) {
		if (Settings.rawBytes) {
			return bytes + " B";
		}
		return humanReadableByteCountBin(bytes, Locale.getDefault(Category.FORMAT));
	}

	/**
	 * <p>
	 * From:<br>
	 * https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
	 *
	 * <p>
	 * Why use 0xfffccccccccccccL?
	 *
	 * <pre>
	 * Because that is the point at which one should transition from PB to EB. Think
	 * of it like this: 0xfffccccccccccccL is to 2^50 what 999,950,000 is to 10^9.
	 *
	 * 0xfff cccc cccc cccc >> 40 = 0xf ffcc
	 *                   0xf ffcc = 1 048 524
	 *             1024 * 1023.95 = 1 048 524.8
	 *
	 * 0xfff cccc cccc cccc >> 30 = 0x3fff 3333
	 *                0x3fff 3333 = 1 073 689 395
	 *           1024^2 * 1023.95 = 1 073 689 395,2
	 *
	 * 0xfff cccc cccc cccc >> 20 = 0xff fccc cccc
	 *             0xff fccc cccc = 1 099 457 940 684
	 *           1024^3 * 1023.95 = 1 099 457 940 684,8
	 *
	 * 0xfff cccc cccc cccc >> 10 = 0x3 fff3 3333 3333
	 *         0x3 fff3 3333 3333 = 1 125 844 931 261 235
	 *           1024^4 * 1023.95 = 1 125 844 931 261 235,2
	 *
	 *  0xfff cccc cccc cccc >> 0 = 1 152 865 209 611 504 844
	 *           1024^5 * 1023.95 = 1 152 865 209 611 504 844,8
	 * </pre>
	 *
	 * @param bytes
	 * @param locale
	 * @return
	 */
	static String humanReadableByteCountBin(long bytes, Locale locale) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfff_cccc_cccc_ccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format(locale, "%.1f %ciB", value / 1024.0, ci.current());
	}

	public static class Timer {
		private final long startTime;

		private Timer() {
			startTime = System.currentTimeMillis();
		}

		public String elapsedSeconds(String prefix) {
			long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
			return prefix + ": " + timeDuration(seconds);
		}
	}

	public static Timer timer() {
		return new Timer();
	}
}

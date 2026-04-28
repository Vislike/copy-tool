package ct.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class UtilsTest {

	@Test
	void timeDuration() {
		assertEquals("0s", Utils.timeDuration(0));
		assertEquals("9s", Utils.timeDuration(9));
		assertEquals("59s", Utils.timeDuration(59));
		assertEquals("1m 0s", Utils.timeDuration(60));
		assertEquals("1m 9s", Utils.timeDuration(69));
		assertEquals("1m 59s", Utils.timeDuration(119));
		assertEquals("2m 0s", Utils.timeDuration(120));
		assertEquals("59m 59s", Utils.timeDuration(3599));
		assertEquals("1h 0m 0s", Utils.timeDuration(3600));
		assertEquals("1h 0m 59s", Utils.timeDuration(3659));
		assertEquals("1h 1m 0s", Utils.timeDuration(3660));
		assertEquals("10h 0m 0s", Utils.timeDuration(36000));
	}

	@Test
	void timeClock() {
		assertEquals("0:00", Utils.timeClock(0));
		assertEquals("0:09", Utils.timeClock(9));
		assertEquals("0:59", Utils.timeClock(59));
		assertEquals("1:00", Utils.timeClock(60));
		assertEquals("1:09", Utils.timeClock(69));
		assertEquals("1:59", Utils.timeClock(119));
		assertEquals("2:00", Utils.timeClock(120));
		assertEquals("59:59", Utils.timeClock(3599));
		assertEquals("1:00:00", Utils.timeClock(3600));
		assertEquals("1:00:59", Utils.timeClock(3659));
		assertEquals("1:01:00", Utils.timeClock(3660));
		assertEquals("10:00:00", Utils.timeClock(36000));
	}

	@Test
	void humanReadableByteCountBin() {
		assertEquals("0 B", Utils.humanReadableByteCountBin(0, Locale.ROOT));
		assertEquals("1023 B", Utils.humanReadableByteCountBin(0x3ff, Locale.ROOT));
		assertEquals("1.0 KiB", Utils.humanReadableByteCountBin(0x3ff + 1, Locale.ROOT));
		assertEquals("1023.9 KiB", Utils.humanReadableByteCountBin(0xf_ffcc, Locale.ROOT));
		assertEquals("1.0 MiB", Utils.humanReadableByteCountBin(0xf_ffcc + 1, Locale.ROOT));
		assertEquals("1023.9 MiB", Utils.humanReadableByteCountBin(0x3fff_3333, Locale.ROOT));
		assertEquals("1.0 GiB", Utils.humanReadableByteCountBin(0x3fff_3333 + 1, Locale.ROOT));
		assertEquals("1023.9 GiB", Utils.humanReadableByteCountBin(0xff_fccc_ccccL, Locale.ROOT));
		assertEquals("1.0 TiB", Utils.humanReadableByteCountBin(0xff_fccc_ccccL + 1, Locale.ROOT));
		assertEquals("1023.9 TiB", Utils.humanReadableByteCountBin(0x3_fff3_3333_3333L, Locale.ROOT));
		assertEquals("1.0 PiB", Utils.humanReadableByteCountBin(0x3_fff3_3333_3333L + 1, Locale.ROOT));
		assertEquals("1023.9 PiB", Utils.humanReadableByteCountBin(0xfff_cccc_cccc_ccccL, Locale.ROOT));
		assertEquals("1.0 EiB", Utils.humanReadableByteCountBin(0xfff_cccc_cccc_ccccL + 1, Locale.ROOT));
		assertEquals("-8.0 EiB", Utils.humanReadableByteCountBin(Long.MIN_VALUE, Locale.ROOT));
		assertEquals("8.0 EiB", Utils.humanReadableByteCountBin(Long.MAX_VALUE, Locale.ROOT));
	}
}

package app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UtilsTest {

	@Test
	void timeLeft() {
		assertEquals("0s", Utils.timeLeft(0));
		assertEquals("9s", Utils.timeLeft(9));
		assertEquals("59s", Utils.timeLeft(59));
		assertEquals("1m 0s", Utils.timeLeft(60));
		assertEquals("1m 9s", Utils.timeLeft(69));
		assertEquals("1m 59s", Utils.timeLeft(119));
		assertEquals("2m 0s", Utils.timeLeft(120));
		assertEquals("59m 59s", Utils.timeLeft(3599));
		assertEquals("1h 0m 0s", Utils.timeLeft(3600));
		assertEquals("1h 0m 59s", Utils.timeLeft(3659));
		assertEquals("1h 1m 0s", Utils.timeLeft(3660));
		assertEquals("10h 0m 0s", Utils.timeLeft(36000));
	}

	@Test
	void timeElapsed() {
		assertEquals("0:00", Utils.timeElapsed(0));
		assertEquals("0:09", Utils.timeElapsed(9));
		assertEquals("0:59", Utils.timeElapsed(59));
		assertEquals("1:00", Utils.timeElapsed(60));
		assertEquals("1:09", Utils.timeElapsed(69));
		assertEquals("1:59", Utils.timeElapsed(119));
		assertEquals("2:00", Utils.timeElapsed(120));
		assertEquals("59:59", Utils.timeElapsed(3599));
		assertEquals("1:00:00", Utils.timeElapsed(3600));
		assertEquals("1:00:59", Utils.timeElapsed(3659));
		assertEquals("1:01:00", Utils.timeElapsed(3660));
		assertEquals("10:00:00", Utils.timeElapsed(36000));
	}
}

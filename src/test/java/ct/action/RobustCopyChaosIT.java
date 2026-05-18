package ct.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ct.action.type.CopyTask;
import ct.support.chaos.ChaosIO;
import ct.util.TestUtils;

public class RobustCopyChaosIT extends RobustCopyIT {

	private long seed;

	@BeforeEach
	void genSeed() {
		seed = System.nanoTime();
	}

	private void chaosAssert(String sha256) throws IOException {
		assertEquals(sha256, TestUtils.sha256(tempFile().path()), "Seed to reproduce: " + seed);
	}

	private void testChaos(int chance, int rollback) throws IOException {
		RobustCopy rc = createRobustCopy(new ChaosIO(chance, seed), rollback);
		rc.copy(new CopyTask(file2999b(), tempFile()));
		chaosAssert(SHA_256_2999B_FILE);
		// Will truncate
		rc.copy(new CopyTask(file1999b(), tempFile()));
		chaosAssert(SHA_256_1999B_FILE);
	}

	@Test
	void highFailChanceST() throws IOException {
		testChaos(5000, 0);
	}

	@Test
	void midFailChanceSTWithRollback() throws IOException {
		testChaos(3300, 2);
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void lowFailChanceST() throws IOException {
		testChaos(500, 0);
	}
}

package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ct.files.types.CopyTask;
import ct.tools.chaos.ChaosIO;
import ct.utils.TestUtils;

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
		rc.copy(new CopyTask(largeFile(), tempFile()));
		chaosAssert(SHA_256_LARGE_FILE);
		// Will truncate
		rc.copy(new CopyTask(smallFile(), tempFile()));
		chaosAssert(SHA_256_SMALL_FILE);
	}

	@Test
	void highFailChance() throws IOException {
		testChaos(5000, 0);
	}

	@Test
	void midFailChanceWithRollback() throws IOException {
		testChaos(3300, 2);
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void lowFailChance() throws IOException {
		testChaos(500, 0);
	}
}

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

	private void testChaos(int chance, int rollback, boolean multiThreaded) throws IOException {
		RobustCopy rc = createRobustCopy(new ChaosIO(chance, seed), rollback, multiThreaded);
		rc.copy(new CopyTask(file2999b(), tempFile()));
		chaosAssert(SHA_256_2999B_FILE);
		// Will truncate
		rc.copy(new CopyTask(file1999b(), tempFile()));
		chaosAssert(SHA_256_1999B_FILE);
	}

	@Test
	void highFailChanceST() throws IOException {
		testChaos(5000, 0, false);
	}

	@Test
	void midFailChanceSTWithRollback() throws IOException {
		testChaos(3300, 2, false);
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void lowFailChanceST() throws IOException {
		testChaos(500, 0, false);
	}

	@Test
	void highFailChanceMT() throws IOException {
		testChaos(5000, 0, true);
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void lowFailChanceMT() throws IOException {
		testChaos(500, 0, true);
	}
}

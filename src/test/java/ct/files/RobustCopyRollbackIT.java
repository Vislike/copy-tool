package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ct.files.io.IOWrapper;
import ct.files.io.IOWrapper.WT;
import ct.files.types.CopyTask;

public class RobustCopyRollbackIT extends RobustCopyIT {

	private void testRollback(IOWrapper io, int rollback, boolean equals) throws IOException {
		createRobustCopy(io, rollback).copy(new CopyTask(largeFile(), tempFile()));
		verifySha256Temp(SHA_256_LARGE_FILE, equals);
	}

	@Test
	void rollback() throws IOException {
		TestFailableIO io = new TestFailableIO();
		// 2999 bytes completes in 6 cycles, fail at write 6 throws away two buffers
		// 5+4, restart at 3 completed, fail at read 8 throws away buffers 4+3, restart
		// at 2 completed, read 9-12 for 6 completed
		io.failAt(WT.write, 6).failAt(WT.read, 8);
		testRollback(io, 2, true);
		assertEquals(12, io.count(WT.read));
		assertEquals(11, io.count(WT.write));
	}

	@Test
	void corruptedRead() throws IOException {
		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5), 0, false);

		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5), 1, false);

		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5), 2, true);
	}

	@Test
	void corruptedWrite() throws IOException {
		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.write, 1).failAt(WT.write, 3), 0, false);

		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.write, 1).failAt(WT.write, 3), 1, false);

		subTestStart();
		testRollback(new TestFailableIO().corruptAt(WT.write, 1).failAt(WT.write, 3), 2, true);
	}

	@Test
	void corruptedReadAndWrite() throws IOException {
		TestFailableIO io;

		// Write fail rolls back to read corruption, lucky
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5).corruptAt(WT.write, 4).failAt(WT.write, 5);
		testRollback(io, 1, true);

		// Not lucky here so file is corrupt
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5).corruptAt(WT.write, 5).failAt(WT.write, 6);
		testRollback(io, 1, false);

		// Read fail rolls back to write corruption, lucky
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5).corruptAt(WT.write, 4).failAt(WT.write, 7);
		testRollback(io, 2, true);

		// Not lucky here so file is corrupt
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5).corruptAt(WT.write, 5).failAt(WT.write, 8);
		testRollback(io, 2, false);

		// Rolls back enough buffers, all is ok
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.read, 3).failAt(WT.read, 5).corruptAt(WT.write, 5).failAt(WT.write, 8);
		testRollback(io, 3, true);
	}

	@Test
	void corruptedWriteAndRead() throws IOException {
		TestFailableIO io;

		// Read fail rolls back to write corruption, lucky
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.write, 3).failAt(WT.write, 5).corruptAt(WT.read, 5).failAt(WT.read, 6);
		testRollback(io, 1, true);

		// Not lucky here so file is corrupt
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.write, 3).failAt(WT.write, 5).corruptAt(WT.read, 6).failAt(WT.read, 7);
		testRollback(io, 1, false);

		// Write fail rolls back to read corruption, lucky
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.write, 3).failAt(WT.write, 5).corruptAt(WT.read, 5).failAt(WT.read, 8);
		testRollback(io, 2, true);

		// Not lucky here so file is corrupt
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.write, 3).failAt(WT.write, 5).corruptAt(WT.read, 6).failAt(WT.read, 9);
		testRollback(io, 2, false);

		// Rolls back enough buffers, all is ok
		subTestStart();
		io = new TestFailableIO().corruptAt(WT.write, 3).failAt(WT.write, 5).corruptAt(WT.read, 6).failAt(WT.read, 9);
		testRollback(io, 3, true);
	}
}

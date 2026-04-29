package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import ct.app.App;
import ct.app.Settings;
import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;
import ct.files.io.IOWrapper.WT;
import ct.files.progress.IProgressReport;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;
import ct.tui.StdoutPrinter;
import ct.utils.TestUtils;

public class RobustCopyIT {

	// Add "-Dshow=true" as VM argument
	static final boolean OUTPUT_VISIBLE = Boolean.parseBoolean(System.getProperty("show"));
	private static final int TEST_BUFFER_SIZE = 512;
	private static final String SHA_256_SMALL_FILE = "ca40ee83ed80d2f85a606289c0e71863a0ab1da7c347198ed761226b1e760670";
	private static final String SHA_256_LARGE_FILE = "ad3b72ea83803bf178855f7e41e29d4e0bff02b3b69c470d7edaf4459f7157f3";

	Path tempFile;
	int subTest;

	@BeforeEach
	void createTemp(TestInfo testInfo) throws IOException {
		if (OUTPUT_VISIBLE) {
			App.highlight("Test", testInfo.getDisplayName());
		}
		Settings.verbose = true;
		subTest = 0;
		tempFile = Files.createTempFile("ct-test-", null);
	}

	@AfterEach
	void deleteTemp() throws IOException {
		Files.deleteIfExists(tempFile);
		if (OUTPUT_VISIBLE) {
			App.info();
		}
	}

	private FileRecord tempFile() {
		return FileRecord.targetFile(tempFile);
	}

	private static FileRecord smallFile() {
		Path relative = Paths.get("src/test/resources", "1999.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 1999, relative);
	}

	private static FileRecord largeFile() {
		Path relative = Paths.get("src/test/resources", "2999.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 2999, relative);
	}

	private void verifySha256Temp(String expectedSha256, boolean equals) throws IOException {
		if (equals) {
			assertEquals(expectedSha256, TestUtils.sha256(tempFile().path()));
		} else {
			assertNotEquals(expectedSha256, TestUtils.sha256(tempFile().path()));
		}
	}

	private IProgressReport createMessageProducer() {
		return OUTPUT_VISIBLE ? new StdoutPrinter() : new TestVoidProgress();
	}

	private RobustCopy createRobustCopy(IOWrapper wrapper, int rollback) {
		return new RobustCopy(wrapper, Settings.rollback(TEST_BUFFER_SIZE, rollback), createMessageProducer());
	}

	private void copyAndVerifySmallFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper, 0).copy(new CopyTask(smallFile(), tempFile()));
		verifySha256Temp(SHA_256_SMALL_FILE, true);
	}

	private void copyAndVerifyLargeFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper, 0).copy(new CopyTask(largeFile(), tempFile()));
		verifySha256Temp(SHA_256_LARGE_FILE, true);
	}

	private void testFailAt1Count2(WT t) throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(t, 1));
		assertEquals(2, io.count(t));
	}

	private void testRollback(IOWrapper io, int rollback, boolean equals) throws IOException {
		createRobustCopy(io, rollback).copy(new CopyTask(largeFile(), tempFile()));
		verifySha256Temp(SHA_256_LARGE_FILE, equals);
	}

	private void subTestStart() {
		if (OUTPUT_VISIBLE) {
			if (subTest > 0) {
				App.info();
			}
			subTest++;
			App.highlight("Sub Test", subTest);
		}
	}

	@Test
	void canary() throws IOException {
		copyAndVerifySmallFile(new FilesIO());
	}

	@Test
	void interruptRead() throws IOException {
		Thread.currentThread().interrupt();
		copyAndVerifySmallFile(new FilesIO());
	}

	@Test
	void truncate() throws IOException {
		copyAndVerifyLargeFile(new FilesIO());
		assertEquals(2999, Files.size(tempFile().path()));
		copyAndVerifySmallFile(new FilesIO());
		assertEquals(1999, Files.size(tempFile().path()));
	}

	@Test
	void createDirectoriesFail() throws IOException {
		testFailAt1Count2(WT.createDirectories);
	}

	@Test
	void getLastModifiedTimeFail() throws IOException {
		testFailAt1Count2(WT.getLastModifiedTime);
	}

	@Test
	void setLastModifiedTimeFail() throws IOException {
		testFailAt1Count2(WT.setLastModifiedTime);
	}

	@Test
	void openAndCloseFail() throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(WT.open, 2).failAt(WT.close, 3));
		assertEquals(4, io.count(WT.open));
		// Only close opened, and do not retry close
		assertEquals(3, io.count(WT.close));
	}

	@Test
	void readWriteAndDiffFail() throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(WT.read, 2).failAt(WT.write, 4));
		assertEquals(6, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		io = new TestFailableIO();
		copyAndVerifySmallFile(io.writeOneLessAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));
	}

	@Test
	void sizeTruncateAndResumeFail() throws IOException {
		copyAndVerifyLargeFile(new FilesIO());
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(WT.truncate, 1).failAt(WT.size, 2).failAt(WT.position, 4));
		assertEquals(4, io.count(WT.read));
		assertEquals(4, io.count(WT.write));
		assertEquals(6, io.count(WT.position));
		assertEquals(3, io.count(WT.size));
		assertEquals(2, io.count(WT.truncate));
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

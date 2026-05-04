package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;
import ct.files.io.IOWrapper.WT;
import ct.files.types.CopyTask;

public class RobustCopyBasicIT extends RobustCopyIT {

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
}

package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ct.files.TestFailableIO.TT;
import ct.files.io.FilesIO;
import ct.files.io.IProgress;
import ct.files.io.IOWrapper;
import ct.files.meta.FileRecord;
import ct.files.meta.Settings;
import ct.utils.TestUtils;

public class RobustCopyIT {

	private static final int TEST_BUFFER_SIZE = 512;
	private static final String SHA_256_SMALL_FILE = "ca40ee83ed80d2f85a606289c0e71863a0ab1da7c347198ed761226b1e760670";
	private static final String SHA_256_LARGE_FILE = "ad3b72ea83803bf178855f7e41e29d4e0bff02b3b69c470d7edaf4459f7157f3";

	Path tempFile;

	@BeforeEach
	void createTemp() throws IOException {
		tempFile = Files.createTempFile("ct-test-", null);
	}

	@AfterEach
	void deleteTemp() throws IOException {
		Files.deleteIfExists(tempFile);
	}

	private FileRecord tempFile() {
		return FileRecord.targetFile(tempFile);
	}

	private static FileRecord smallFile() {
		return FileRecord.sourceFile(Paths.get("src/test/resources", "1999.bin"), 1999);
	}

	private static FileRecord largeFile() {
		return FileRecord.sourceFile(Paths.get("src/test/resources", "2999.bin"), 2999);
	}

	private void verifySha256Temp(String expectedSha256) throws IOException {
		assertEquals(expectedSha256, TestUtils.sha256(tempFile().path()));
	}

	private IProgress createMessageProducer() {
		return new TestVoidProgress();
	}

	private RobustCopy createRobustCopy(IOWrapper wrapper) {
		return new RobustCopy(wrapper, Settings.bufferSize(TEST_BUFFER_SIZE), createMessageProducer());
	}

	private void copyAndVerifySmallFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper).copy(smallFile(), tempFile());
		verifySha256Temp(SHA_256_SMALL_FILE);
	}

	private void copyAndVerifyLargeFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper).copy(largeFile(), tempFile());
		verifySha256Temp(SHA_256_LARGE_FILE);
	}

	private void testFailAt1Count2(TT t) throws IOException {
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
		testFailAt1Count2(TT.createDirectories);
	}

	@Test
	void getLastModifiedTimeFail() throws IOException {
		testFailAt1Count2(TT.getLastModifiedTime);
	}

	@Test
	void setLastModifiedTimeFail() throws IOException {
		testFailAt1Count2(TT.setLastModifiedTime);
	}

	@Test
	void openFail() throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(TT.open, 2));
		assertEquals(4, io.count(TT.open));
	}

	@Test
	void readWriteAndDiffFail() throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(TT.read, 2).failAt(TT.write, 4));
		assertEquals(6, io.count(TT.read));
		assertEquals(5, io.count(TT.write));

		io = new TestFailableIO();
		copyAndVerifySmallFile(io.writeOneLessAt(3));
		assertEquals(5, io.count(TT.read));
		assertEquals(5, io.count(TT.write));
	}

	@Test
	void sizeTruncateAndResumeFail() throws IOException {
		copyAndVerifyLargeFile(new FilesIO());
		TestFailableIO io = new TestFailableIO();
		copyAndVerifySmallFile(io.failAt(TT.truncate, 1).failAt(TT.size, 2).failAt(TT.position, 4));
		assertEquals(4, io.count(TT.read));
		assertEquals(4, io.count(TT.write));
		assertEquals(6, io.count(TT.position));
		assertEquals(3, io.count(TT.size));
		assertEquals(2, io.count(TT.truncate));
	}

	@Test
	void rollback() throws IOException {
		TestFailableIO io = new TestFailableIO();
		// 2999 bytes completes in 6 cycles, fail at write 6 throws away two buffers
		// 5+4, restart at 3 completed, fail at read 8 throws away buffers 4+3, restart
		// at 2 completed, read 9-12 for 6 completed
		io.failAt(TT.write, 6).failAt(TT.read, 8);
		RobustCopy rc = new RobustCopy(io, Settings.rollback(TEST_BUFFER_SIZE, 2), createMessageProducer());
		rc.copy(largeFile(), tempFile());
		verifySha256Temp(SHA_256_LARGE_FILE);
		assertEquals(12, io.count(TT.read));
		assertEquals(11, io.count(TT.write));
	}
}

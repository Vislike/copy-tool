package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;

import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;
import ct.files.io.IOWrapper.WT;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;

public class RobustCopyMultiThreadedIT extends RobustCopyIT {

	private void copyAndVerify1999bFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper, true).copy(new CopyTask(file1999b(), tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	private void copyAndVerify2999bFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper, true).copy(new CopyTask(file2999b(), tempFile()));
		verifySha256Temp(SHA_256_2999B_FILE, true);
	}

	private void testResume(IOWrapper wrapper, long pos) throws IOException {
		FileRecord fr = file1999b();
		FileRecord rr = FileRecord.resumeSource(fr.path(), fr.size(), pos, fr.relativeFromSource());
		createRobustCopy(wrapper, true).copy(new CopyTask(rr, tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	@Test
	void canary() throws IOException {
		copyAndVerify1999bFile(new FilesIO());
	}

	@Test
	void sanity0bCopy() throws IOException {
		createRobustCopy(new FilesIO(), true).copy(new CopyTask(file0b(), tempFile()));
		verifySha256Temp(SHA_256_0B_FILE, true);
	}

	@Test
	void sanity1bCopy() throws IOException {
		createRobustCopy(new FilesIO(), true).copy(new CopyTask(file1b(), tempFile()));
		verifySha256Temp(SHA_256_1B_FILE, true);
	}

	@Test
	void readWriteFails() throws IOException {
		subTestStart();
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(WT.read, 2).failAt(WT.write, 4));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.writeOneLessAt(3));
		assertEquals(4, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.writeZeroAt(3));
		assertEquals(4, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.readZeroAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(4, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.readEofAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(4, io.count(WT.write));
	}

	@Test
	void sizeTruncateAndRestartFail() throws IOException {
		copyAndVerify2999bFile(new FilesIO());
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(WT.truncate, 1).failAt(WT.size, 2).failAt(WT.position, 2));
		assertEquals(4, io.count(WT.read));
		assertEquals(4, io.count(WT.write));
		assertEquals(3, io.count(WT.position));
		assertEquals(3, io.count(WT.size));
		assertEquals(2, io.count(WT.truncate));
	}

	@Test
	void resume() throws IOException {
		// Copy base
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io);
		assertEquals(4, io.count(WT.read));
		assertEquals(4, io.count(WT.write));

		// Truncate it
		FileChannel.open(tempFile().path(), StandardOpenOption.WRITE).truncate(1024).close();
		verifySha256Temp(SHA_256_1999B_FILE, false);

		// Copy with resume
		subTestStart();
		io = new TestFailableIO();
		testResume(io, 1024);
		assertEquals(2, io.count(WT.read));
		assertEquals(2, io.count(WT.write));
		assertEquals(2, io.count(WT.position));
		assertEquals(0, io.count(WT.truncate));

		// Append bytes
		try (FileChannel fileChannel = FileChannel.open(tempFile().path(), StandardOpenOption.APPEND)) {
			fileChannel.write(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));
		}
		verifySha256Temp(SHA_256_1999B_FILE, false);

		// File is already complete, make sure it is truncated
		subTestStart();
		io = new TestFailableIO();
		testResume(io, 1999);
		assertEquals(0, io.count(WT.read));
		assertEquals(0, io.count(WT.write));
		assertEquals(2, io.count(WT.position));
		assertEquals(1, io.count(WT.truncate));
	}
}

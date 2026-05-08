package ct.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;

import ct.files.io.FilesIO;
import ct.files.io.IOWrapper;
import ct.files.io.IOWrapper.WT;
import ct.files.types.CopyTask;
import ct.files.types.FileRecord;

public class RobustCopyBasicIT extends RobustCopyIT {

	private void copyAndVerify1999bFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper).copy(new CopyTask(file1999b(), tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	private void copyAndVerify2999bFile(IOWrapper wrapper) throws IOException {
		createRobustCopy(wrapper).copy(new CopyTask(file2999b(), tempFile()));
		verifySha256Temp(SHA_256_2999B_FILE, true);
	}

	private void testFailAt1Count2(WT t) throws IOException {
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(t, 1));
		assertEquals(2, io.count(t));
	}

	private void testResume(IOWrapper wrapper, long pos, int rollback) throws IOException {
		FileRecord fr = file1999b();
		FileRecord rr = FileRecord.resumeSource(fr.path(), fr.size(), pos, fr.relativeFromSource());
		createRobustCopy(wrapper, rollback).copy(new CopyTask(rr, tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	@Test
	void canary() throws IOException {
		copyAndVerify1999bFile(new FilesIO());
	}

	@Test
	void sanity0bCopy() throws IOException {
		createRobustCopy(new FilesIO()).copy(new CopyTask(file0b(), tempFile()));
		verifySha256Temp(SHA_256_0B_FILE, true);
	}

	@Test
	void sanity1bCopy() throws IOException {
		createRobustCopy(new FilesIO()).copy(new CopyTask(file1b(), tempFile()));
		verifySha256Temp(SHA_256_1B_FILE, true);
	}

	@Test
	void interruptRead() throws IOException {
		Thread.currentThread().interrupt();
		copyAndVerify1999bFile(new FilesIO());
	}

	@Test
	void truncate() throws IOException {
		copyAndVerify2999bFile(new FilesIO());
		assertEquals(2999, Files.size(tempFile().path()));
		copyAndVerify1999bFile(new FilesIO());
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
		copyAndVerify1999bFile(io.failAt(WT.open, 2).failAt(WT.close, 3));
		assertEquals(4, io.count(WT.open));
		// Only close opened, and do not retry close
		assertEquals(3, io.count(WT.close));
	}

	@Test
	void readWriteFails() throws IOException {
		subTestStart();
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(WT.read, 2).failAt(WT.write, 4));
		assertEquals(6, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.writeOneLessAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.writeZeroAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.readZeroAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));

		subTestStart();
		io = new TestFailableIO();
		copyAndVerify1999bFile(io.readEofAt(3));
		assertEquals(5, io.count(WT.read));
		assertEquals(5, io.count(WT.write));
	}

	@Test
	void sizeTruncateAndRestartFail() throws IOException {
		copyAndVerify2999bFile(new FilesIO());
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(WT.truncate, 1).failAt(WT.size, 2).failAt(WT.position, 4));
		assertEquals(4, io.count(WT.read));
		assertEquals(4, io.count(WT.write));
		assertEquals(6, io.count(WT.position));
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
		testResume(io, 1024, 0);
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
		testResume(io, 1999, 0);
		assertEquals(0, io.count(WT.read));
		assertEquals(0, io.count(WT.write));
		assertEquals(2, io.count(WT.position));
		assertEquals(1, io.count(WT.truncate));
	}

	@Test
	void resumeUnaligned() throws IOException {
		// Copy base and truncate
		copyAndVerify1999bFile(new TestFailableIO());
		int pos = 512 * 3 - 2;
		FileChannel.open(tempFile().path(), StandardOpenOption.WRITE).truncate(pos).close();
		assertEquals(pos, Files.size(tempFile().path()));

		// Copy with resume
		subTestStart();
		TestFailableIO io = new TestFailableIO();
		testResume(io, pos, 0);
		assertEquals(2, io.count(WT.read));
		assertEquals(2, io.count(WT.write));

		// Copy and corrupt
		createRobustCopy(new TestFailableIO().corruptAt(WT.write, 3), 0).copy(new CopyTask(file1999b(), tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, false);
		pos = 512 * 2 + 2;
		FileChannel.open(tempFile().path(), StandardOpenOption.WRITE).truncate(pos).close();
		assertEquals(pos, Files.size(tempFile().path()));

		// Copy with resume
		subTestStart();
		io = new TestFailableIO();
		testResume(io, pos, 0);
		assertEquals(2, io.count(WT.read));
		assertEquals(2, io.count(WT.write));
	}

	@Test
	void resumeWithRollback() throws IOException {
		createRobustCopy(new TestFailableIO().corruptAt(WT.write, 2), 0).copy(new CopyTask(file1999b(), tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, false);
		TestFailableIO io = new TestFailableIO();
		testResume(io, 512 * 2, 1);
		assertEquals(3, io.count(WT.read));
		assertEquals(3, io.count(WT.write));
	}
}

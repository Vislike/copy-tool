package ct.action.copy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;

import ct.action.copy.io.FilesIO;
import ct.action.copy.io.IOWrapper;
import ct.action.copy.io.IOWrapper.WT;
import ct.action.copy.model.CopyTask;
import ct.action.copy.model.FileRecord;

public class RobustCopyBasicIT extends RobustCopyIT {

	private void copyAndVerify1999bFile(IOWrapper wrapper) throws Exception {
		createRobustCopy(wrapper).copy(new CopyTask(file1999b(), tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	private void testFailAt1Count2(WT t) throws Exception {
		TestFailableIO io = new TestFailableIO();
		copyAndVerify1999bFile(io.failAt(t, 1));
		assertEquals(2, io.count(t));
	}

	private void testResume(IOWrapper wrapper, long pos) throws Exception {
		FileRecord fr = file1999b();
		FileRecord rr = FileRecord.resumeSource(fr.path(), fr.size(), pos, fr.relativeFromSource());
		createRobustCopy(wrapper).copy(new CopyTask(rr, tempFile()));
		verifySha256Temp(SHA_256_1999B_FILE, true);
	}

	@Test
	void canary() throws Exception {
		copyAndVerify1999bFile(new FilesIO());
	}

	@Test
	void sanity0bCopy() throws Exception {
		createRobustCopy(new FilesIO()).copy(new CopyTask(file0b(), tempFile()));
		verifySha256Temp(SHA_256_0B_FILE, true);
	}

	@Test
	void sanity1bCopy() throws Exception {
		createRobustCopy(new FilesIO()).copy(new CopyTask(file1b(), tempFile()));
		verifySha256Temp(SHA_256_1B_FILE, true);
	}

	@Test
	void createDirectoriesFail() throws Exception {
		testFailAt1Count2(WT.createDirectories);
	}

	@Test
	void getLastModifiedTimeFail() throws Exception {
		testFailAt1Count2(WT.getLastModifiedTime);
	}

	@Test
	void setLastModifiedTimeFail() throws Exception {
		testFailAt1Count2(WT.setLastModifiedTime);
	}

	@Test
	void resumeUnaligned() throws Exception {
		// Copy base and truncate
		copyAndVerify1999bFile(new TestFailableIO());
		int pos = 1700;
		FileChannel.open(tempFile().path(), StandardOpenOption.WRITE).truncate(pos).close();
		assertEquals(pos, Files.size(tempFile().path()));

		// Copy with resume
		TestFailableIO io = new TestFailableIO();
		testResume(io, pos);
		assertEquals(1, io.count(WT.read));
		assertEquals(1, io.count(WT.write));
	}
}

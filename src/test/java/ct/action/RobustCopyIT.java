package ct.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import ct.action.io.IOWrapper;
import ct.action.progress.IProgressReport;
import ct.action.type.FileRecord;
import ct.app.App;
import ct.app.Settings;
import ct.tui.StdoutPrinter;
import ct.util.TestUtils;

public abstract class RobustCopyIT {

	// Add "-Dshow=true" as VM argument
	static final boolean OUTPUT_VISIBLE = Boolean.parseBoolean(System.getProperty("show"));
	protected static final String SHA_256_0B_FILE = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
	protected static final String SHA_256_1B_FILE = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
	protected static final String SHA_256_1999B_FILE = "ca40ee83ed80d2f85a606289c0e71863a0ab1da7c347198ed761226b1e760670";
	protected static final String SHA_256_2999B_FILE = "ad3b72ea83803bf178855f7e41e29d4e0bff02b3b69c470d7edaf4459f7157f3";
	private static final int TEST_BUFFER_SIZE = 512;

	private Path tempFile;
	private int subTest;

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
		// For some reason windows sometimes thinks file is busy, even then fileChannels
		// is closed in finally block. Try up to 10 times to delete the file.
		boolean fileExits = true;
		for (int i = 0; fileExits && i < 10; i++) {
			try {
				Files.deleteIfExists(tempFile);
				fileExits = false;
			} catch (IOException e) {
				// Ignore
			}
		}
		if (fileExits) {
			App.warning("Failed to delete", tempFile);
		}

		if (OUTPUT_VISIBLE) {
			App.info();
		}
	}

	protected FileRecord tempFile() {
		return FileRecord.targetFile(tempFile);
	}

	protected static FileRecord file0b() {
		Path relative = Paths.get("src/test/resources", "0.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 0, relative);
	}

	protected static FileRecord file1b() {
		Path relative = Paths.get("src/test/resources", "1.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 1, relative);
	}

	protected static FileRecord file1999b() {
		Path relative = Paths.get("src/test/resources", "1999.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 1999, relative);
	}

	protected static FileRecord file2999b() {
		Path relative = Paths.get("src/test/resources", "2999.bin");
		return FileRecord.sourceFile(relative.toAbsolutePath(), 2999, relative);
	}

	protected void verifySha256Temp(String expectedSha256, boolean equals) throws IOException {
		if (equals) {
			assertEquals(expectedSha256, TestUtils.sha256(tempFile().path()));
		} else {
			assertNotEquals(expectedSha256, TestUtils.sha256(tempFile().path()));
		}
	}

	private IProgressReport createMessageProducer() {
		return OUTPUT_VISIBLE ? new StdoutPrinter() : new TestVoidProgress();
	}

	protected RobustCopy createRobustCopy(IOWrapper wrapper) {
		return createRobustCopy(wrapper, 0);
	}

	protected RobustCopy createRobustCopy(IOWrapper wrapper, int rollback) {
		return new RobustCopy(wrapper, Settings.testRobustCopy(TEST_BUFFER_SIZE, rollback).robustCopy(),
				createMessageProducer());
	}

	protected void subTestStart() {
		if (OUTPUT_VISIBLE) {
			subTest++;
			App.info();
			App.highlight("Sub Test", subTest);
		}
	}
}

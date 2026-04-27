package ct.files.types;

public record CopyTask(FileRecord sourceFile, FileRecord targetFile) {

	@Override
	public String toString() {
		return sourceFile.toString();
	}
}
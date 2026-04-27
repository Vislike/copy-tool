package ct.files.types;

import java.util.ArrayList;
import java.util.List;

public record AnalyseResult(List<CopyTask> copy, List<FileRecord> match, List<FileRecord> mismatch) {

	public AnalyseResult() {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}
}
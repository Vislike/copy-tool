package ct.action;

import java.util.ArrayList;
import java.util.List;

import ct.action.copy.model.CopyTask;
import ct.action.copy.model.FileRecord;

public record AnalyseResult(List<CopyTask> copy, List<FileRecord> match, List<FileRecord> mismatch) {

	public AnalyseResult() {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}
}
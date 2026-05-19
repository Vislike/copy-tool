package ct.action.progress;

import java.nio.file.attribute.FileTime;

import ct.action.type.CopyTask;

public sealed interface IProgressEvent {

	enum RestartType {
		read, write, copy;
	}

	record CopyStartEvent(CopyTask ct) implements IProgressEvent {
	}

	record CopyEndEvent(CopyTask ct) implements IProgressEvent {
	}

	record CopyProgressEvent(long size) implements IProgressEvent {
	}

	record ResumeEvent(long pos) implements IProgressEvent {
	}

	record RestartEvent(long pos, RestartType type) implements IProgressEvent {
	}

	record TruncateEvent(long size) implements IProgressEvent {
	}

	record ModifiedTimeEvent(FileTime time) implements IProgressEvent {
	}

	record WaitStartEvent(int seconds) implements IProgressEvent {
	}

	record WaitEndEvent() implements IProgressEvent {
	}

	record WarningEvent(String description, String cause) implements IProgressEvent {
	}

	record ErrorEvent(String description, String cause) implements IProgressEvent {
	}

	record AbortEvent(CopyTask ct) implements IProgressEvent {
	}
}

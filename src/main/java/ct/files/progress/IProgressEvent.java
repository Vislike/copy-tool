package ct.files.progress;

import java.nio.file.attribute.FileTime;

import ct.files.types.CopyTask;

public sealed interface IProgressEvent {

	record CopyStartEvent(CopyTask ct) implements IProgressEvent {
	}

	record CopyEndEvent(CopyTask ct) implements IProgressEvent {
	}

	record CopyProgressEvent(long size) implements IProgressEvent {
	}

	record RestartEvent(long pos) implements IProgressEvent {
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
}

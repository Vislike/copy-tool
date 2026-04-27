package ct.tui.types;

import ct.files.progress.IProgressEvent;

public record ProgressUpdate(int threadId, IProgressEvent event, boolean eof) {
}
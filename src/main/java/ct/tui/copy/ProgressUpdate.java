package ct.tui.copy;

import ct.action.progress.IProgressEvent;

public record ProgressUpdate(int threadId, IProgressEvent event, boolean eof) {
}
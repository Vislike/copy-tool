package ct.tui.type;

import ct.action.progress.IProgressEvent;

public record ProgressUpdate(int threadId, IProgressEvent event, boolean eof) {
}
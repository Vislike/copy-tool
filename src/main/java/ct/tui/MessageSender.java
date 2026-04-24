package ct.tui;

import java.util.concurrent.BlockingQueue;

import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.files.progress.IProgressReport;
import ct.tui.Message.Status;
import ct.tui.PrintBufferedProgress.DeBounce;

public class MessageSender implements IProgressReport {

	private static final long DEBOUNCE_TIME = 900;

	private final int threadId;
	private final BlockingQueue<Message> mq;
	private DeBounce db;

	public MessageSender(int threadId, BlockingQueue<Message> mq) {
		this.threadId = threadId;
		this.mq = mq;
	}

	public void sendMessage(String str) {
		try {
			mq.put(new Message(threadId, Status.MESSAGE, str));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupt not implemented yet", e);
		}
	}

	public void done() {
		try {
			mq.put(new Message(threadId, Status.EOF, null));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupt not implemented yet", e);
		}
	}

	@Override
	public void raise(IProgressEvent event) {
		switch (event) {
		case CopyStartEvent e -> {
			db = new DeBounce(e.ct().sourceFile().size());
			sendMessage(PrintBufferedProgress.stringMessage(event, db));
		}
		case CopyProgressEvent e -> {
			if (db.shouldUpdate(e.size(), DEBOUNCE_TIME)) {
				sendMessage(PrintBufferedProgress.stringMessage(event, db));
			}
		}
		default -> sendMessage(PrintBufferedProgress.stringMessage(event, db));
		}

	}
}

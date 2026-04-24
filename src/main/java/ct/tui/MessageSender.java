package ct.tui;

import java.util.concurrent.BlockingQueue;

import ct.files.progress.IProgressEvent;
import ct.files.progress.IProgressEvent.CopyProgressEvent;
import ct.files.progress.IProgressEvent.CopyStartEvent;
import ct.tui.Message.Status;
import ct.files.progress.IProgressReport;
import ct.files.progress.StdoutProgress;

public class MessageSender implements IProgressReport {

	private final int threadId;
	private final BlockingQueue<Message> mq;

	private final StdoutProgress out = new StdoutProgress();

	public MessageSender(int threadId, BlockingQueue<Message> mq) {
		this.threadId = threadId;
		this.mq = mq;
	}

	public void message(String str) {
		try {
			mq.put(new Message(threadId, Status.MESSAGE, str));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void done() {
		try {
			mq.put(new Message(threadId, Status.EOF, null));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void raise(IProgressEvent event) {
		switch (event) {
		case CopyStartEvent e -> {
			out.progressStart(e.ct().sourceFile().size());
			message(out.stringMessage(event));
		}
		case CopyProgressEvent e -> {
			if (out.progressUpdate(e.size(), 900)) {
				message(out.stringMessage(event));
			}
		}
		default -> message(out.stringMessage(event));
		}

	}
}

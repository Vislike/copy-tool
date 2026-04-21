package ct.app.tui;

import java.util.concurrent.BlockingQueue;

import ct.app.tui.Message.Status;
import ct.files.io.IProgress;

public class MessageSender implements IProgress {

	private final int threadId;
	private final BlockingQueue<Message> mq;

	public MessageSender(int threadId, BlockingQueue<Message> mq) {
		this.threadId = threadId;
		this.mq = mq;
	}

	@Override
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
}

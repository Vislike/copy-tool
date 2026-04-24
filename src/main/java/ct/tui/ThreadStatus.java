package ct.tui;

public class ThreadStatus {

	private final int threadId;
	final Thread thread;

	boolean eof = false;
	String message = "";

	public ThreadStatus(int threadId, Thread thread) {
		this.threadId = threadId;
		this.thread = thread;
	}

	public void update(Message message) {
		if (message.threadId() != threadId) {
			throw new AssertionError();
		}
		switch (message.status()) {
		case MESSAGE -> this.message = message.msg();
		case EOF -> this.eof = true;
		}
	}
}

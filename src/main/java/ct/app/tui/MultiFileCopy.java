package ct.app.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.metadata.CopyTask;
import ct.files.metadata.Settings;

public class MultiFileCopy {

	private final Settings settings;

	public MultiFileCopy(Settings settings) {
		this.settings = settings;
	}

	public void copyAll(List<CopyTask> fileList) {
		BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<Message>(settings.filesSimultaneously() * 4);
		BlockingQueue<CopyTask> queue = new ArrayBlockingQueue<>(fileList.size(), false, fileList);

		List<ThreadStatus> threads = new ArrayList<>();

		for (int i = 0; i < settings.filesSimultaneously(); i++) {
			final int threadId = i;

			Thread thread = Thread.ofVirtual().start(() -> {
				MessageSender messageSender = new MessageSender(threadId, messageQueue);
				RobustCopy rc = new RobustCopy(new FilesIO(), settings, messageSender);
				CopyTask c;
				while ((c = queue.poll()) != null) {
					rc.copy(c.sourceFile(), c.targetFile());
				}
				messageSender.done();
			});

			threads.add(new ThreadStatus(threadId, thread));
		}

		Tui tui = new Tui();

		try {
			while (!allEof(threads)) {
				Message message = messageQueue.take();
				threads.get(message.threadId()).update(message);

				tui.update(threads);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean allEof(List<ThreadStatus> threads) {
		for (ThreadStatus threadStatus : threads) {
			if (!threadStatus.eof) {
				return false;
			}
		}
		return true;
	}
}

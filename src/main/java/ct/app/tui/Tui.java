package ct.app.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.files.Analyse.Copy;
import ct.app.App;
import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.meta.Settings;
import ct.utils.AnsiEscapeCodes;
import ct.utils.Utils;

public class Tui {

	private final Settings settings;

	public Tui(Settings settings) {
		this.settings = settings;
	}

	public void copyAll(List<Copy> fileList) {
		long startTime = System.currentTimeMillis();

		BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<Message>(settings.filesSimultaneously() * 4);
		BlockingQueue<Copy> queue = new ArrayBlockingQueue<>(fileList.size(), false, fileList);

		List<ThreadStatus> threads = new ArrayList<>();

		for (int i = 0; i < settings.filesSimultaneously(); i++) {
			final int threadId = i;

			Thread thread = Thread.ofVirtual().start(() -> {
				MessageSender messageSender = new MessageSender(threadId, messageQueue);
				RobustCopy rc = new RobustCopy(new FilesIO(), settings, messageSender);
				Copy c;
				while ((c = queue.poll()) != null) {
					rc.copy(c.sourceFile(), c.targetFile());
				}
				messageSender.done();
			});

			threads.add(new ThreadStatus(threadId, thread));
		}

		App.infonb("Copy status:");
		for (ThreadStatus _ : threads) {
			App.info("Starting...");
		}

		try {
			while (!allEof(threads)) {
				Message message = messageQueue.take();
				threads.get(message.threadId()).update(message);

				StringBuilder sb = new StringBuilder();

				AnsiEscapeCodes.moveUpAndErase(sb, threads.size() + 2);
				sb.append(System.lineSeparator() + "Copy status:" + System.lineSeparator());
				for (ThreadStatus threadStatus : threads) {
					sb.append(threadStatus.message + System.lineSeparator());
				}
				App.infonn(sb.toString());
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		App.info("Copy Complete in: " + Utils.timeLeft((System.currentTimeMillis() - startTime) / 1000));
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

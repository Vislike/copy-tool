package ct.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ct.files.Analyse.Copy;
import ct.files.RobustCopy;
import ct.files.io.FilesWrapper;
import ct.files.meta.Settings;
import ct.utils.Utils;

public class Tui {

	private final Settings settings;

	public Tui(Settings settings) {
		this.settings = settings;
	}

	public void copyAll(List<Copy> fileList) {
		long startTime = System.currentTimeMillis();

		BlockingQueue<Copy> queue = new ArrayBlockingQueue<>(fileList.size(), false, fileList);

		List<Thread> threads = new ArrayList<>();

		for (int i = 0; i < settings.filesSimultaneously(); i++) {
			threads.add(Thread.ofVirtual().start(() -> {
				RobustCopy rc = new RobustCopy(new FilesWrapper(), settings);
				Copy c;
				while ((c = queue.poll()) != null) {
					rc.copy(c.sourceFile(), c.targetFile());
					App.info();
				}
			}));
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
		}

		App.info("Copy Complete in: " + Utils.timeLeft((System.currentTimeMillis() - startTime) / 1000));
	}
}

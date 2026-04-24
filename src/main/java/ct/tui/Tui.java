package ct.tui;

import java.util.List;

import ct.app.App;
import ct.utils.AnsiEscapeCodes;

public class Tui {

	private int rows = 0;
	private StringBuilder sb = new StringBuilder();

	public void update(List<ThreadStatus> threads) {
		// Clear old
		clear();

		sb.append(nl()).append("Copy status:").append(nl());
		for (ThreadStatus threadStatus : threads) {
			if (!threadStatus.eof) {
				sb.append(threadStatus.message).append(nl());
			}
		}
		App.infonn(sb.toString());
	}

	private String nl() {
		rows++;
		return System.lineSeparator();
	}

	private void clear() {
		sb.setLength(0);
		AnsiEscapeCodes.moveUpAndErase(sb, rows);
		rows = 0;
	}
}

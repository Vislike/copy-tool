package ct.utils;

import ct.app.App;
import ct.files.metadata.Settings;

public class AnsiEscapeCodes {

	private AnsiEscapeCodes() {
	}

	public static enum Color {
		RESET("\u001B[m"), RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m"), MAGENTA("\u001B[35m"),
		CYAN("\u001B[36m"), CYAN_INTENSE("\u001B[96m"), WHITE_INTENSE("\u001B[97m");

		private final String color;

		Color(String color) {
			this.color = color;
		}

		public StringBuilder append(StringBuilder sb) {
			if (Settings.terminalColor) {
				sb.append(color);
			}
			return sb;
		}

		public void emit() {
			if (Settings.terminalColor) {
				App.infonn(color);
			}
		}
	}

	private static final String PREV_LINE = "\u001B[%dF";
	private static final String ERASE_DOWN = "\u001B[J";

	public static StringBuilder moveUpAndErase(StringBuilder sb, int lines) {
		if (lines > 0) {
			sb.append(PREV_LINE.formatted(lines)).append(ERASE_DOWN);
		}
		return sb;
	}
}

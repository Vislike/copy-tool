package ct.utils;

import ct.app.App;
import ct.files.meta.Settings;

public class AnsiEscapeCodes {

	public static enum Color {
		RESET("\u001B[m"), RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m");

		private final String color;

		Color(String color) {
			this.color = color;
		}

		public StringBuilder append(StringBuilder sb) {
			if (Settings.virtualTerminalProcessing) {
				sb.append(color);
			}
			return sb;
		}

		public void emit() {
			if (Settings.virtualTerminalProcessing) {
				App.infonn(color);
			}
		}
	}
}

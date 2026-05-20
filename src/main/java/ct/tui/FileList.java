package ct.tui;

import ct.action.AnalyseResult;
import ct.app.App;
import ct.app.Settings.AnalyseSettings;
import ct.util.AnsiEscapeCodes.Color;

public class FileList {

	public static void show(AnalyseResult files, AnalyseSettings settings) {
		if (!files.match().isEmpty()) {
			App.infolb(textMatch(files.match().size()));
			Color.GREEN.emit();
			files.match().forEach(App::info);
			Color.RESET.emit();
			App.info(textMatch(files.match().size()));
		}

		if (!files.mismatch().isEmpty()) {
			App.infolb(textMismatch(files.mismatch().size(), settings));
			Color.YELLOW.emit();
			files.mismatch().forEach(App::info);
			Color.RESET.emit();
			App.info(textMismatch(files.mismatch().size(), settings));
		}

		if (!files.copy().isEmpty()) {
			App.infolb(textCopy(files.copy().size()));
			files.copy().forEach(App::info);
			App.info(textCopy(files.copy().size()));
		}
	}

	private static String textMismatch(int num, AnalyseSettings s) {
		StringBuilder sb = new StringBuilder();
		Color.WHITE_INTENSE.append(sb).append("- - - Existing mismatching files (");
		if (s.overwrite()) {
			Color.RED.append(sb).append("overwriting");
		} else if (s.resume()) {
			Color.MAGENTA.append(sb).append("resuming");
		} else {
			Color.YELLOW.append(sb).append("skipping");
		}
		Color.WHITE_INTENSE.append(sb).append("): ");
		return Color.RESET.append(sb).append(num).toString();
	}

	private static String textMatch(int num) {
		return Color.WHITE_INTENSE.highlight("* * * Existing matching files (size and modify date)", num);
	}

	private static String textCopy(int num) {
		return Color.WHITE_INTENSE.highlight("+ + + Files to Copy", num);
	}
}

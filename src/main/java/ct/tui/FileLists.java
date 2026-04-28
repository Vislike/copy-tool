package ct.tui;

import java.util.List;

import ct.app.App;
import ct.app.Settings;
import ct.files.Analyse;
import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.types.AnalyseResult;
import ct.files.types.CopyTask;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Utils;
import ct.utils.Utils.Timer;

public class FileLists {

	public static void analyseAllFiles(Settings settings) {
		App.devModeCheck();
		App.infolb(textFrom(settings));
		App.info(textTo(settings));
		App.info();

		App.infonn("Analysing files...");

		AnalyseResult files = Analyse.findAllFiles(settings);

		App.info("complete");

		if (!files.match().isEmpty()) {
			App.infolb(textMatch());
			Color.GREEN.emit();
			files.match().forEach(App::info);
			Color.RESET.emit();
		}

		if (!files.mismatch().isEmpty()) {
			App.infolb(textMismatch(settings));
			Color.YELLOW.emit();
			files.mismatch().forEach(App::info);
			Color.RESET.emit();
		}

		if (!files.copy().isEmpty()) {
			App.infolb(textCopy());
			files.copy().forEach(App::info);
		}

		App.infolb(textFrom(settings));
		App.info(textTo(settings));
		App.devModeCheck();

		if (settings.dryRun()) {
			App.infolb("Dry Run Complete");
		} else if (files.copy().isEmpty()) {
			App.infolb("Up to date");
		} else {
			Timer timer = Utils.timer();
			if (Settings.devMode) {
				new MultiFileCopy(settings).copyAll(files.copy());
			} else {
				simpleMode(settings, files.copy());
			}
			App.infolb(timer.elapsedSeconds("Copy Complete in"));
		}
	}

	private static void simpleMode(Settings settings, List<CopyTask> tasks) {
		RobustCopy rc = new RobustCopy(new FilesIO(), settings, new StdoutPrinter());
		tasks.forEach(ct -> {
			App.info();
			rc.copy(ct);
		});
	}

	private static String textMismatch(Settings settings) {
		StringBuilder sb = new StringBuilder();
		Color.WHITE_INTENSE.append(sb).append("- - - - Existing mismatching files (");
		if (settings.overwrite()) {
			Color.RED.append(sb).append("overwriting");
		} else {
			Color.YELLOW.append(sb).append("skipping");
		}
		Color.WHITE_INTENSE.append(sb).append(") - - - -");
		return Color.RESET.append(sb).toString();
	}

	private static String textMatch() {
		return Color.WHITE_INTENSE.highlight("+ + + + Existing matching files (size and modify date) + + + +");
	}

	private static String textCopy() {
		return Color.WHITE_INTENSE.highlight("* * * * Files to Copy * * * *");
	}

	private static String textFrom(Settings settings) {
		return Color.CYAN.highlight("Copy from", settings.sourceDir());
	}

	private static String textTo(Settings settings) {
		return Color.CYAN_INTENSE.highlight("Copy to",
				settings.targetDir().resolve(settings.sourceDir().getFileName()));
	}
}

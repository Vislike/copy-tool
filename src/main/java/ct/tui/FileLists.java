package ct.tui;

import java.util.List;

import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.AnalyseSettings;
import ct.app.Settings.RobustCopySettings;
import ct.files.Analyse;
import ct.files.RobustCopy;
import ct.files.io.FilesIO;
import ct.files.types.AnalyseResult;
import ct.files.types.CopyTask;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Utils;
import ct.utils.Utils.Timer;

public class FileLists {

	public static void analyseAllFiles(Settings globalSettings) {
		AnalyseSettings settings = globalSettings.analyse();

		App.devModeCheck();
		App.infolb(textFrom(settings));
		App.info(textTo(settings));
		App.info();

		App.infonn("Analysing files...");

		AnalyseResult files = Analyse.findAllFiles(settings);

		App.info("complete");

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

		App.infolb(textFrom(settings));
		App.info(textTo(settings));
		App.devModeCheck();

		if (settings.dryRun()) {
			App.infolb("Dry Run Complete");
		} else if (files.copy().isEmpty()) {
			App.infolb("Up to date");
		} else {
			Timer timer = Utils.timer();
			if (globalSettings.multiFile().logMode()) {
				logMode(globalSettings.robustCopy(), files.copy());
			} else {
				new MultiFileCopy(globalSettings, new FilesIO()).copyAll(files.copy());
			}
			App.infolb(timer.elapsedSeconds("Copy Complete in"));
		}
	}

	private static void logMode(RobustCopySettings settings, List<CopyTask> tasks) {
		RobustCopy rc = new RobustCopy(new FilesIO(), settings, new StdoutPrinter());
		tasks.forEach(ct -> {
			App.info();
			rc.copy(ct);
		});
	}

	private static String textMismatch(int num, AnalyseSettings s) {
		StringBuilder sb = new StringBuilder();
		Color.WHITE_INTENSE.append(sb).append("- - - Existing mismatching files (");
		if (s.overwrite()) {
			Color.RED.append(sb).append("overwriting");
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

	private static String textFrom(AnalyseSettings s) {
		return Color.CYAN.highlight("Copy from", s.sourceDir());
	}

	private static String textTo(AnalyseSettings s) {
		return Color.CYAN_INTENSE.highlight("Copy to", s.targetDir().resolve(s.sourceDir().getFileName()));
	}
}

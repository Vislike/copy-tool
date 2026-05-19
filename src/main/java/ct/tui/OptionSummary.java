package ct.tui;

import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.AnalyseSettings;
import ct.util.AnsiEscapeCodes.Color;

public class OptionSummary {

	public static void show(Settings settings) {
		App.infolb(textFrom(settings.analyse()));
		App.info(textTo(settings.analyse()));
		App.configCheck(settings);
	}

	private static String textFrom(AnalyseSettings s) {
		return Color.CYAN.highlight("Copy from", s.sourceDir());
	}

	private static String textTo(AnalyseSettings s) {
		return Color.CYAN_INTENSE.highlight("Copy to", s.targetDir().resolve(s.sourceDir().getFileName()));
	}
}

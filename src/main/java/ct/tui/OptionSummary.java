package ct.tui;

import ct.app.App;
import ct.app.Settings;
import ct.app.Settings.AnalyseSettings;
import ct.app.Settings.RobustCopySettings;
import ct.util.AnsiEscapeCodes.Color;
import ct.util.Utils;

public class OptionSummary {

	public static void show(Settings settings) {
		App.infolb(textFrom(settings.analyse()));
		App.info(textTo(settings.analyse()));
		App.verbose("Copy mode", modeText(settings.robustCopy()));
		App.verbose("Copy buffer size", Utils.size(settings.robustCopy().bufferSize()));
		App.configCheck(settings);
	}

	private static String textFrom(AnalyseSettings s) {
		return Color.CYAN.highlight("Copy from", s.sourceDir());
	}

	private static String textTo(AnalyseSettings s) {
		return Color.CYAN_INTENSE.highlight("Copy to", s.targetDir().resolve(s.sourceDir().getFileName()));
	}

	private static String modeText(RobustCopySettings s) {
		return s.zeroCopy() ? "Zero-Copy" : "Direct Buffer";
	}
}

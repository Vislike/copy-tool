package ct.runner;

import ct.action.AnalyseAction;
import ct.action.type.AnalyseResult;
import ct.app.App;
import ct.app.Settings;
import ct.tui.FileList;
import ct.tui.OptionSummary;

public class AnalyseRunner {

	public static void execute(Settings settings) {
		OptionSummary.show(settings);

		App.info();
		App.infonn("Analysing files...");
		AnalyseResult files = AnalyseAction.findAllFiles(settings.analyse());
		App.info("complete");

		FileList.show(files, settings.analyse());

		OptionSummary.show(settings);

		if (settings.analyse().dryRun()) {
			App.infolb("Dry Run Complete");
		} else if (files.copy().isEmpty()) {
			App.infolb("Up to date");
		} else {
			CopyRunner.execute(files, settings);
		}
	}
}

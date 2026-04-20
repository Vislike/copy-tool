package ct.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import ct.files.Analyse;
import ct.files.Analyse.FoundFiles;
import ct.files.meta.Settings;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Native;

public class App {

	static final int BUFF_SIZE = 1024 * 1024 * 1;
	static final int WAIT_TIME = 10;
	static final int ROLLBACK_BUFFERS = 1;

	public static void main(String[] args) throws IOException {
		infona("= = = = Copy Tool v" + version() + " = = = =");

		CommandLine.parseOutputArgs(args);

		if (Settings.terminalColor) {
			boolean vtp = Native.enableVirtualTerminalProcessing();
			verbose("EnableVirtualTerminalProcessing", vtp);
			verbose();
		}

		CommandLine.parseArgs(args).ifPresentOrElse(App::copyAllFiles, CommandLine::printHelp);
	}

	private static String version() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = App.class.getResourceAsStream("/ct.properties")) {
			properties.load(in);
		}
		return properties.getProperty("ct.version", "0-dev");
	}

	private static void copyAllFiles(Settings settings) {
		info("Copy from: " + settings.sourceDir());
		infona("Copy to: " + settings.targetDir().resolve(settings.sourceDir().getFileName()));

		infonn("Finding files...");

		FoundFiles files = Analyse.findAllFiles(settings);

		info("complete");

		if (!files.match().isEmpty()) {
			infonb("+ + + + Existing matching files (size and modify date) + + + +");
			Color.GREEN.emit();
			files.match().forEach(App::info);
			Color.RESET.emit();
		}

		if (!files.missmatch().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("- - - - Existing mismatching files, ");
			if (settings.overwrite()) {
				Color.RESET.append(Color.RED.append(sb).append("overwriting"));
			} else {
				sb.append("skipping");
			}
			infonb(sb.append(" - - - -").toString());
			Color.YELLOW.emit();
			files.missmatch().forEach(App::info);
			Color.RESET.emit();
		}

		if (!files.copy().isEmpty()) {
			infonb("* * * * Files to Copy * * * *");
			files.copy().forEach(App::info);
		}

		if (settings.dryRun()) {
			infonb("Dry Run Complete");
		} else {
			info();
			new Tui(settings).copyAll(files.copy());
		}
	}

	public static void info() {
		System.out.println();
	}

	public static void info(String str) {
		System.out.println(str);
	}

	public static void info(Object obj) {
		System.out.println(obj);
	}

	public static void infonn(String str) {
		System.out.print(str);
	}

	public static void infona(String str) {
		System.out.println(str);
		System.out.println();
	}

	public static void infonb(String str) {
		System.out.println();
		System.out.println(str);
	}

	public static void verbose() {
		if (Settings.verbose) {
			System.out.println();
		}
	}

	public static void verbose(String str, Object... args) {
		if (Settings.verbose) {
			printCommon(Color.GREEN, "<V> ", false, str, args);
		}
	}

	public static void warning(String str, Object... args) {
		printCommon(Color.YELLOW, "<WARNING> ", true, str, args);
	}

	public static void error(String str, Object... args) {
		printCommon(Color.RED, "<ERROR> ", true, str, args);
	}

	private static void printCommon(Color color, String tag, boolean extraNl, String str, Object... args) {
		StringBuilder sb = color.append(new StringBuilder(128)).append(tag).append(str);
		if (args.length > 0) {
			Color.RESET.append(sb.append(": ")).append(args[0]);
			for (int i = 1; i < args.length; i++) {
				Color.RESET.append(color.append(sb).append(", ")).append(args[i]);
			}
		} else {
			Color.RESET.append(sb);
		}
		if (extraNl) {
			sb.append(System.lineSeparator());
		}
		System.out.println(sb.toString());
	}
}

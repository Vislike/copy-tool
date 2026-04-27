package ct.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import ct.tui.FileLists;
import ct.utils.AnsiEscapeCodes.Color;
import ct.utils.Native;

public class App {

	static final int BUFF_SIZE = 1024 * 1024 * 1;
	static final int WAIT_TIME = 10;
	static final int ROLLBACK_BUFFERS = 1;
	static final int NUM_FILES_SIMULTANEOUSLY = 4;
	static final int TERMINAL_WIDTH = 120;

	public static void main(String[] args) throws IOException {
		infona("= = = = Copy Tool v" + version() + " = = = =");

		CommandLine.parseOutputArgs(args);

		if (Settings.terminalColor) {
			boolean vtp = Native.enableVirtualTerminalProcessing();
			verbose("EnableVirtualTerminalProcessing", vtp);
			verbose();
		}

		CommandLine.parseArgs(args).ifPresentOrElse(FileLists::analyseAllFiles, CommandLine::help);
	}

	private static String version() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = App.class.getResourceAsStream("/ct.properties")) {
			properties.load(in);
		}
		return properties.getProperty("ct.version", "0-dev");
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

	public static void highlight(String str, Object... args) {
		printCommon(Color.YELLOW, "", false, str, args);
	}

	public static void verbose(String str, Object... args) {
		if (Settings.verbose) {
			printCommon(Color.GREEN, "<V> ", false, str, args);
		}
	}

	public static void warning(String str, Object... args) {
		printCommon(Color.MAGENTA, "<WARNING> ", true, str, args);
	}

	public static void recoverWarning(String str, Object... args) {
		printCommon(Color.MAGENTA, "", false, str, args);
	}

	public static void error(String str, Object... args) {
		printCommon(Color.RED, "<ERROR> ", true, str, args);
	}

	public static void recoverError(String str, Object... args) {
		printCommon(Color.RED, "", false, str, args);
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

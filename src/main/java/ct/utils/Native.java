package ct.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Locale;
import java.util.Optional;

public class Native {

	private Native() {
	}

	private static final int STD_OUTPUT_HANDLE = -11;
	private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x04;
	private static final long INVALID_HANDLE_VALUE = -1;

	public static boolean enableVirtualTerminalProcessing() {
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
			try (Arena arena = Arena.ofConfined()) {
				// Linker
				Linker linker = Linker.nativeLinker();

				// Lookup functions
				SymbolLookup sl = SymbolLookup.libraryLookup("kernel32.dll", arena);
				Optional<MemorySegment> getStdHandleLookup = sl.find("GetStdHandle");
				Optional<MemorySegment> getConsoleModeLookup = sl.find("GetConsoleMode");
				Optional<MemorySegment> setConsoleModeLookup = sl.find("SetConsoleMode");
				if (getStdHandleLookup.isEmpty()) {
					return false;
				}
				if (getConsoleModeLookup.isEmpty()) {
					return false;
				}
				if (setConsoleModeLookup.isEmpty()) {
					return false;
				}

				// Downcall method handles
				MethodHandle getStdHandle = linker.downcallHandle(getStdHandleLookup.get(),
						FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
				MethodHandle getConsoleMode = linker.downcallHandle(getConsoleModeLookup.get(),
						FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
				MethodHandle setConsoleMode = linker.downcallHandle(setConsoleModeLookup.get(),
						FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

				// Get stdout handle
				MemorySegment handle = (MemorySegment) getStdHandle.invokeExact(STD_OUTPUT_HANDLE);
				if (handle.address() == INVALID_HANDLE_VALUE) {
					return false;
				}

				// Get current mode
				MemorySegment lpMode = arena.allocateFrom(ValueLayout.JAVA_INT, 0);
				boolean success = (boolean) getConsoleMode.invokeExact(handle, lpMode);
				if (!success) {
					return false;
				}
				int mode = lpMode.get(ValueLayout.JAVA_INT, 0);

				// Check if Virtual Terminal Processing is enabled
				if ((mode & ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
					return true;
				}

				// Enable
				success = (boolean) setConsoleMode.invokeExact(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
				if (!success) {
					return false;
				}
			} catch (RuntimeException | Error e) {
				throw e;
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
		}
		return true;
	}
}

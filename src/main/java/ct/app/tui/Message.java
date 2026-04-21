package ct.app.tui;

public record Message(int threadId, Status status, String msg) {

	public static enum Status {
		MESSAGE, EOF
	}
}

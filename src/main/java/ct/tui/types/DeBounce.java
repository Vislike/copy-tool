package ct.tui.types;

public class DeBounce {
	private final long deBounceTime;
	private final long size;
	private final long startTime;

	private long time;
	private long bytes;
	private long resumePos;

	public DeBounce(long deBounceTime, long size) {
		this.deBounceTime = deBounceTime;
		this.size = size;
		this.startTime = System.currentTimeMillis();
	}

	public boolean shouldUpdate(long bytes) {
		return time + deBounceTime <= System.currentTimeMillis() || bytes >= size;
	}

	public void update(long time, long bytes) {
		this.time = time;
		this.bytes = bytes;
	}

	public void setResumePos(long resumePos) {
		this.resumePos = resumePos;
	}

	public long size() {
		return size;
	}

	public long startTime() {
		return startTime;
	}

	public long time() {
		return time;
	}

	public long bytes() {
		return bytes;
	}

	public long resumePos() {
		return resumePos;
	}

	public long copySize() {
		return size - resumePos;
	}
}

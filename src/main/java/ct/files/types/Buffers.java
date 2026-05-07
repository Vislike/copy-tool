package ct.files.types;

import java.nio.ByteBuffer;

public class Buffers {

	private final ByteBuffer[] buffers;

	private int index = 0;

	public Buffers(int num, int size) {
		buffers = new ByteBuffer[num];

		allocate(size);
	}

	private void allocate(int size) {
		for (int i = 0; i < buffers.length; i++) {
			buffers[i] = ByteBuffer.allocateDirect(size);
		}
	}

	public ByteBuffer next() {
		ByteBuffer res = buffers[index];
		index = (index + 1) % buffers.length;
		return res;
	}
}

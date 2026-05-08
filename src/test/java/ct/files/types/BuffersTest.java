package ct.files.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

public class BuffersTest {

	@Test
	void nextTest() {
		Buffers buffers = new Buffers(3, 1024);
		ByteBuffer b1 = buffers.next();
		b1.put((byte) 1).flip();
		ByteBuffer b2 = buffers.next();
		b2.put((byte) 2).flip();
		ByteBuffer b3 = buffers.next();
		b3.put((byte) 3).flip();
		assertNotEquals(b1, b2);
		assertNotEquals(b1, b3);
		assertNotEquals(b2, b3);
		for (int i = 0; i < 10; i++) {
			assertEquals(b1, buffers.next());
			assertEquals(b2, buffers.next());
			assertEquals(b3, buffers.next());
		}
	}

	@Test
	void currentTest() {
		Buffers buffers = new Buffers(3, 1024);

		buffers.current().put((byte) 1).flip();
		assertEquals(1, buffers.current().limit());
		assertEquals(1, buffers.next().limit());
		assertNotEquals(1, buffers.current().limit());
		assertNotEquals(1, buffers.next().limit());
		assertNotEquals(1, buffers.current().limit());
		assertNotEquals(1, buffers.next().limit());
		assertEquals(1, buffers.current().limit());
		assertEquals(1, buffers.next().limit());
	}
}

package au.edu.usyd.corona.compression;


import java.util.Random;

import junit.framework.TestCase;

public class JZlibCompressorTest extends TestCase {
	
	private final Compressor c = new JZlibCompressor();
	
	private void assertArraysEqual(byte[] original) {
		byte[] compressed = c.compress(original);
		byte[] decompressed = c.decompress(compressed);
		assertEquals(original.length, decompressed.length);
		for (int i = 0; i != original.length; i++)
			assertTrue("index " + i + " not equal", original[i] == decompressed[i]);
	}
	
	public void testDefaultConstructor() {
		assertArraysEqual(new byte[]{});
		assertArraysEqual(new byte[]{-1, Byte.MAX_VALUE, Byte.MIN_VALUE});
		assertArraysEqual(new byte[]{1, 2, 3, 4, 5});
		assertArraysEqual(new byte[1024]);
	}
	
	public void testLargeData() {
		final int N = Short.MAX_VALUE;
		byte[] data = new byte[N];
		for (int i = 0; i < N; i += 3)
			data[i] = (byte) (i % Byte.MAX_VALUE);
		assertArraysEqual(data);
	}
	
	public void testRandomData() {
		final int N = 512;
		final Random r = new Random();
		byte[] data = new byte[N];
		for (int i = 0; i != N; i++)
			data[i] = (byte) (r.nextInt() % Byte.MAX_VALUE);
		assertArraysEqual(data);
	}
}

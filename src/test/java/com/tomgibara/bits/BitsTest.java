package com.tomgibara.bits;

import java.util.Random;

import junit.framework.TestCase;

public class BitsTest extends TestCase {

	private final Random random = new Random(0L);
	
	public void testNewBitStoreFromString() {
		for (int i = 0; i < 1000; i++) {
			int size = i / 4;
			BitVector v = new BitVector(random, size);
			String s = v.toString();
			BitStore u = Bits.newBitStore(s);
			assertEquals(v, u);
			assertEquals(u, v);
		}
	}
	
}

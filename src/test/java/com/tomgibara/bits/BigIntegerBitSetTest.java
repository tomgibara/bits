package com.tomgibara.bits;

import java.math.BigInteger;
import java.util.Random;

import junit.framework.TestCase;

public class BigIntegerBitSetTest extends TestCase {

	private final Random random = new Random(0L);
	
	public void testGeneral() {
		for (int i = 0; i < 1000; i++) {
			int size = random.nextInt(500);
			BigInteger b = new BigInteger(size, random);
			BitStore s = new BigIntegerBitStore(b);
			assertFalse(s.isMutable());
			BitVector v = BitVector.fromBigInteger(b);
			assertEquals(s.size(), v.size());
			assertEquals(s,v);
			assertEquals(v,s);
			assertEquals(v, s.mutableCopy());
			size = s.size(); // may be shorter due to leading zeros
			for (int j = 0; j < size; j++) {
				assertEquals(v.getBit(j), s.getBit(j));
			}
			BitVector u = new BitVector(random, random.nextInt(500) );
			assertEquals(v.compareNumericallyTo(u), s.compareNumericallyTo(u));
			assertEquals(v.asNumber().doubleValue(), s.asNumber().doubleValue());
		}
	}
	
}

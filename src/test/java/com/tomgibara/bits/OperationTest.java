package com.tomgibara.bits;

import java.util.Random;

import junit.framework.TestCase;

public class OperationTest extends TestCase {

	public void testStores() {
		BitVector u = new BitVector("11001010");
		BitVector v = new BitVector("00001111");
		assertEquals(new BitVector("00001111"), Operation.SET.stores(u, v));
		assertEquals(new BitVector("00001010"), Operation.AND.stores(u, v));
		assertEquals(new BitVector("11001111"), Operation.OR.stores(u, v));
		assertEquals(new BitVector("11000101"), Operation.XOR.stores(u, v));
	}
	
	public void testReaders() {
		Random random = new Random(0L);
		for (int i = 0; i < 10000; i++) {
			BitVector a = new BitVector(random, random.nextInt(1000));
			BitVector b = new BitVector(random, random.nextInt(1000));
			for (Operation operation : Operation.values) {
				BitReader reader = operation.readers(a.openReader(), b.openReader());
				BitVector c = new BitVector(Math.min(a.size(), b.size()));
				Bits.transfer(reader, c.openWriter(), c.size());
				try {
					reader.readBoolean();
					fail();
				} catch (EndOfBitStreamException e) {
					// expected
				}
				BitVector d = a.resizedCopy(c.size(), true);
				BitVector e = b.resizedCopy(c.size(), true);
				d.op(operation).withStore(e);
				assertEquals(d, c);
			}
		}
	}
}

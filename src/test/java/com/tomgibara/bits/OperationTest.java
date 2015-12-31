/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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

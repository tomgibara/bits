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

public class BitsTest extends TestCase {

	private final Random random = new Random(0L);
	
	public void testNewBitStoreFromString() {
		for (int i = 0; i < 1000; i++) {
			int size = i / 4;
			BitVector v = new BitVector(random, size);
			String s = v.toString();
			BitStore u = Bits.storeFromChars(s);
			assertEquals(v, u);
			assertEquals(u, v);
		}
	}
	
	public void testResizedCopyOf() {
		for (int i = 0; i < 1000; i++) {
			int size = random.nextInt(500);
			BitStore store = Bits.store(size);
			store.range(0, size / 2).fillWithOnes();
			store.permute().shuffle(random);
			testResizedCopyOf(store);
		}
	}

	private void testResizedCopyOf(BitStore v) {
		int size = v.size();
		int a = size == 0 ? 0 : random.nextInt(size);

		BitStore w = Bits.resizedCopyOf(v, a, false);
		assertEquals(a, w.size());
		assertEquals(v.rangeTo(a), w);
		
		w = Bits.resizedCopyOf(v, a, true);
		assertEquals(a, w.size());
		assertEquals(v.rangeFrom(size - a), w);

		w = Bits.resizedCopyOf(v, size, false);
		assertEquals(v, w);

		w = Bits.resizedCopyOf(v, size, true);
		assertEquals(v, w);

		a = size == 0 ? 1 : size + random.nextInt(size);

		w = Bits.resizedCopyOf(v, a, false);
		assertEquals(a, w.size());
		assertEquals(v, w.rangeTo(size));
		assertTrue( w.rangeFrom(size).zeros().isAll() );

		w = Bits.resizedCopyOf(v, a, true);
		assertEquals(a, w.size());
		assertEquals(v, w.rangeFrom(a - size));
		assertTrue( w.rangeTo(a - size).zeros().isAll() );
	}

}

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
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

public class BitsTest extends TestCase {

	private final Random random = new Random(0L);

	public void testNewBitStoreFromString() {
		for (int i = 0; i < 1000; i++) {
			int size = i / 4;
			BitVector v = new BitVector(random, size);
			String s = v.toString();
			BitStore u = Bits.toStore(s);
			assertEquals(v, u);
			assertEquals(u, v);
		}
	}

	public void testResizedCopyOf() {
		for (int i = 0; i < 1000; i++) {
			int size = random.nextInt(500);
			BitStore store = Bits.store(size);
			store.range(0, size / 2).fill();
			store.permute().shuffle(random);
			testResizedCopyOf(store);
		}
	}

	public void testSetAsStore() {
		SortedSet<Integer> set = new TreeSet<Integer>();
		set.add(-1);
		set.add(50);

		BitStore store = Bits.asStore(set, 0, 50, true);
		assertTrue(store.zeros().isAll());
		assertEquals(50, store.size());

		Bits.asStore(set.subSet(0, 50), 0, 50, true);

		try {
			Bits.asStore(set.subSet(0, 5), 0, 10, true);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testFreeRangeOf() {
		assertEquals("0001111111000", Bits.freeRangeOver(Bits.oneBits(7), -3, 10).toString());
		assertEquals("1111000", Bits.freeRangeOver(Bits.oneBits(7), 3, 10).toString());
		assertEquals("0001111", Bits.freeRangeOver(Bits.oneBits(7), -3, 4).toString());
		assertEquals("1", Bits.freeRangeOver(Bits.oneBits(7), 3, 4).toString());
		assertEquals("", Bits.freeRangeOver(Bits.oneBits(7), -1, -1).toString());
		assertEquals("", Bits.freeRangeOver(Bits.oneBits(7), 4, 4).toString());
		assertEquals("", Bits.freeRangeOver(Bits.oneBits(7), 8, 8).toString());

		BitStore alt = Bits.asStore("010101");
		assertEquals("010101", Bits.freeRangeOver(alt, 0, 6).toString());
		assertEquals("101010", Bits.freeRangeOver(alt, 6, 0).toString());
		assertEquals("101", Bits.freeRangeOver(alt, 0, 3).toString());
		assertEquals("010", Bits.freeRangeOver(alt, 3, 6).toString());
		assertEquals("0101010", Bits.freeRangeOver(alt, 0, 7).toString());

		BitStore store = new BitVector(5);
		BitStore view = Bits.freeRangeOver(store, -1, 6);
		assertEquals(7, view.size());
		assertEquals("0000000", view.toString());
		store.setAll(true);
		assertEquals("0111110", view.toString());
		assertEquals(store, view.range(1, 6));
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

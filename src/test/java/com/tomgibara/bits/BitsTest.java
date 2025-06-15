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

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BitsTest {

	private final Random random = new Random(0L);

	@Test
	public void testNewBitStoreFromString() {
		for (int i = 0; i < 1000; i++) {
			int size = i / 4;
			BitStore v = Bits.toStore(size, random);
			String s = v.toString();
			BitStore u = Bits.toStore(s);
			assertEquals(v, u);
			assertEquals(u, v);
		}
	}

	@Test
	public void testResizedCopyOf() {
		for (int i = 0; i < 1000; i++) {
			int size = random.nextInt(500);
			BitStore store = Bits.store(size);
			store.range(0, size / 2).fill();
			store.permute().shuffle(random);
			testResizedCopyOf(store);
		}
	}

	@Test
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

	@Test
	public void testFreeRangeOf() {
		testExtendedStore("0001111111000", Bits.freeRangeViewOf(Bits.oneBits(7), -3, 10, false));
		testExtendedStore("1111000", Bits.freeRangeViewOf(Bits.oneBits(7), 3, 10, false));
		testExtendedStore("0001111", Bits.freeRangeViewOf(Bits.oneBits(7), -3, 4, false));
		testExtendedStore("1", Bits.freeRangeViewOf(Bits.oneBits(7), 3, 4, false));
		testExtendedStore("", Bits.freeRangeViewOf(Bits.oneBits(7), -1, -1, false));
		testExtendedStore("", Bits.freeRangeViewOf(Bits.oneBits(7), 4, 4, false));
		testExtendedStore("", Bits.freeRangeViewOf(Bits.oneBits(7), 8, 8, false));

		BitStore alt = Bits.asStore("010101");
		testExtendedStore("010101", Bits.freeRangeViewOf(alt, 0, 6, false));
		testExtendedStore("101010", Bits.freeRangeViewOf(alt, 6, 0, false));
		testExtendedStore("101", Bits.freeRangeViewOf(alt, 0, 3, false));
		testExtendedStore("010", Bits.freeRangeViewOf(alt, 3, 6, false));
		testExtendedStore("0101010", Bits.freeRangeViewOf(alt, 0, 7, false));

		BitStore store = new BitVector(5);
		BitStore view = Bits.freeRangeViewOf(store, -1, 6, false);
		assertEquals(7, view.size());
		testExtendedStore("0000000", view);
		store.setAll(true);
		testExtendedStore("0111110", view);
		assertEquals(store, view.range(1, 6));

		testExtendedStore("1110000000111", Bits.freeRangeViewOf(Bits.zeroBits(7), -3, 10, true));
		testExtendedStore("00000", Bits.freeRangeViewOf(Bits.oneBits(5), -5, 0, false));
		testExtendedStore("00000", Bits.freeRangeViewOf(Bits.oneBits(5), 5, 10, false));
		testExtendedStore("11111", Bits.freeRangeViewOf(Bits.oneBits(5), -5, 0, true));
		testExtendedStore("11111", Bits.freeRangeViewOf(Bits.oneBits(5), 5, 10, true));
	}

	private void testExtendedStore(String expected, BitStore store) {
		GrowableBits bits = Bits.growableBits();
		store.writeTo(bits.writer());
		assertEquals(bits.toMutableBitVector(), store);
		if (expected != null) {
			BitStore exp = Bits.asStore(expected);
			assertEquals(exp, store);
			assertEquals(expected, store.toString());
			assertEquals(exp.flipped(), store.flipped());
			assertEquals(exp.reversed(), store.reversed());
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

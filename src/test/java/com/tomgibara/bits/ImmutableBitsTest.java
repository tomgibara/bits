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

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class ImmutableBitsTest {

	abstract boolean isOnes();

	private final Random random = new Random(0L);

	BitStore newBits(int size) {
		return Bits.bits(isOnes(), size);
	}

	@Test
	public void testGetBit() {
		for (int i = 0; i < 100; i++) {
			int size = random.nextInt(1000);
			BitStore s = newBits(size);
			for (int j = 0; j < size; j++) {
				assertEquals(isOnes(), s.getBit(j));
			}
		}
	}

	@Test
	public void testImmutable() {
		try {
			newBits(2).setBit(0, !isOnes());
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
	}

	@Test
	public void testReader() {
		for (int i = 0; i < 100; i++) {
			int size = random.nextInt(1000);
			int from = random.nextInt(size + 1);
			int to = random.nextInt(size - from + 1) + from;
			BitStore s = newBits(size);
			BitVector v = new BitVector(size);
			BitReader r = s.openReader(from, to);
			BitWriter w = v.openWriter(from, to);
			int length = to - from;
			Bits.transfer(r, w, length);
			assertEquals(v.range(from, to), s.range(from, to));
		}
	}

	@Test
	public void testMatches() {
		BitStore bits = newBits(10);
		{
			BitMatches matches = bits.match(isOnes());
			SortedSet<Integer> set = matches.asSet();
			assertEquals(10, set.size());
			assertEquals(new TreeSet<Integer>(Arrays.asList(new Integer[] {0,1,2,3,4,5,6,7,8,9})), set);
			Iterator<Integer> it = set.iterator();
			for (int i = 0; i < 10; i++) {
				int p = it.next().intValue();
				assertEquals(i, p);
			}
			Positions ps = matches.positions(10);
			assertEquals(10, ps.nextIndex());
			for (int i = 9; i >= 0; i--) {
				assertEquals(i + 1, ps.nextIndex());
				assertEquals(i, ps.previousIndex());
				int p = ps.previous().intValue();
				assertEquals(i, p);
			}
			assertEquals(-1, ps.previousIndex());
			assertEquals(-1, ps.previousIndex());
		}
		{
			BitMatches matches = bits.match(!isOnes());
			assertTrue(matches.asSet().isEmpty());
			assertEquals(10, matches.positions().nextPosition());
			assertEquals(-1, matches.positions().previousPosition());
			assertFalse(matches.positions().hasPrevious());
			assertFalse(matches.positions().hasNext());
		}
	}

	@Test
	public void testEquals() {
		for (int i = 0; i < 1000; i++) {
			BitVector v = new BitVector(random, 0.8f, random.nextInt(12));
			assertEquals(v.match(isOnes()).isAll(), newBits(v.size()).equals(v));
		}
	}
}

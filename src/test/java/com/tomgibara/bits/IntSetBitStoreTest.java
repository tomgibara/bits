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

import java.util.TreeSet;

public class IntSetBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return new IntSetBitStore(new TreeSet<Integer>().subSet(0, size), 0, size, true);
	}

	public void testSparsity() {
		int size = 10000000;
		BitStore s = newStore(size);
		BitStore t = newStore(size);
		s.setBit(0, true);
		t.setBit(size - 1, true);
		s.or().withStore(t);
		assertTrue(s.getBit(0));
		assertTrue(s.getBit(size - 1));
	}

	public void testVerySimpleBits() {
		BitStore s = newStore(8);
		s.setBit(0, true);
		s.setBit(2, true);
		s.setBit(7, true);
		assertEquals(0b10000101L, s.getBits(0, 8));
	}
}

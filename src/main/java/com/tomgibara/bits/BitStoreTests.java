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

import com.tomgibara.bits.BitStore.Test;
import com.tomgibara.bits.BitStore.Tests;

abstract class BitStoreTests implements Tests {

	static long mask(int size) {
		return ~(-1L << size);
	}

	final BitStore s;

	BitStoreTests(BitStore store) {
		s = store;
	}

	@Override
	public boolean bits(long bits) {
		int size = s.size();
		checkThisSize(size);
		return testBits(s.getBits(0, size), bits, size);
	}

	@Override
	public boolean store(BitStore store) {
		int size = store.size();
		checkThatSize(size);
		int limit = size & ~63;
		for (int i = 0; i < limit; i += 64) {
			boolean result = testBits(s.getBits(i, 64), store.getBits(i, 64));
			if (!result) return false;
		}
		int rem = size - limit;
		return rem == 0 || testBits(s.getBits(limit, rem), store.getBits(limit, rem), rem);
	}

	abstract boolean testBits(long ourBits, long theirBits);

	abstract boolean testBits(long ourBits, long theirBits, int size);

	private void checkThisSize(int size) {
		if (size > 64) throw new IllegalArgumentException("store size greater than 64 bits");

	}

	private void checkThatSize(int size) {
		if (s.size() != size) throw new IllegalArgumentException("mismatched size");
	}

	static final class Equals extends BitStoreTests {

		Equals(BitStore store) {
			super(store);
		}

		@Override
		public Test getTest() {
			return Test.EQUALS;
		}

		@Override
		boolean testBits(long ourBits, long theirBits) {
			return ourBits == theirBits;
		}

		@Override
		boolean testBits(long ourBits, long theirBits, int size) {
			long mask = mask(size);
			return (ourBits & mask) == (theirBits & mask);
		}
	}

	static final class Excludes extends BitStoreTests {

		Excludes(BitStore store) {
			super(store);
		}

		@Override
		public Test getTest() {
			return Test.EXCLUDES;
		}

		@Override
		boolean testBits(long ourBits, long theirBits) {
			return (ourBits & theirBits) == 0L;
		}

		@Override
		boolean testBits(long ourBits, long theirBits, int size) {
			return (ourBits & theirBits & mask(size)) == 0L;
		}
	}

	static final class Contains extends BitStoreTests {

		Contains(BitStore store) {
			super(store);
		}

		@Override
		public Test getTest() {
			return Test.CONTAINS;
		}

		@Override
		boolean testBits(long ourBits, long theirBits) {
			return (~ourBits & theirBits) == 0L;
		}

		@Override
		boolean testBits(long ourBits, long theirBits, int size) {
			return (~ourBits & theirBits & mask(size)) == 0L;
		}
	}

	static final class Complements extends BitStoreTests {

		Complements(BitStore store) {
			super(store);
		}

		@Override
		public Test getTest() {
			return Test.COMPLEMENTS;
		}

		@Override
		boolean testBits(long ourBits, long theirBits) {
			return (ourBits ^ theirBits) == -1L;
		}

		@Override
		boolean testBits(long ourBits, long theirBits, int size) {
			long mask = mask(size);
			return ((ourBits ^ theirBits) & mask) == mask;
		}
	}

}

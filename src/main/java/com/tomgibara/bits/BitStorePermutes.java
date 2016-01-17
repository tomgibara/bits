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

import com.tomgibara.bits.BitStore.Permutes;

final class BitStorePermutes implements Permutes {

	private final BitStore store;
	
	BitStorePermutes(BitStore store) {
		this.store = store;
	}

	@Override
	public void transpose(int i, int j) {
		checkMutable();
		if (i == j) return;
		boolean a = store.getBit(i);
		boolean b = store.getBit(j);
		if (a == b) return;
		store.flipBit(i);
		store.flipBit(j);
	}

	@Override
	public void rotate(int distance) {
		checkMutable();
		int size = store.size();
		if (size < 2) return;
		distance = distance % size;
		if (distance < 0) distance += size;
		if (distance == 0) return;

		//TODO is this capable of optimization in some cases?
		final int cycles = Bits.gcd(distance, size);
		for (int i = cycles - 1; i >= 0; i--) {
			boolean m = store.getBit(i); // the previously overwritten value
			int j = i; // the index that is to be overwritten next
			do {
				j += distance;
				if (j >= size) j -= size;
				m = store.getThenSetBit(j, m);
			} while (j != i);
		}
	}

	@Override
	public void reverse() {
		checkMutable();
		int from = 0;
		int to = store.size() - 1;
		while (from < to) {
			transpose(from++, to--);
		}
	}

	@Override
	public void shuffle(Random random) {
		if (random == null) throw new IllegalArgumentException("null random");
		checkMutable();
		int size = store.size();
		int length = size;
		int ones = store.ones().count();
		// simple case - all bits identical, nothing to do
		if (ones == 0 || ones == length) return;
		// relocate one-bits
		//TODO could set multiple bits at once for better performance
		for (int i = 0; ones < length && ones > 0; length--) {
			boolean one = random.nextInt(length) < ones;
			store.setBit(i++, one);
			if (one) ones--;
		}
		// fill remaining definites
		if (length > 0) {
			store.range(size - length, size).setAll(ones > 0);
		}
	}

	private void checkMutable() {
		if (!store.isMutable()) throw new IllegalStateException("immutable");
	}

}

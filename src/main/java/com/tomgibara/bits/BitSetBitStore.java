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

import java.util.BitSet;

final class BitSetBitStore extends AbstractBitStore {

	private final BitSet set;
	private final int start;
	private final int finish;
	private final boolean mutable;

	BitSetBitStore(BitSet set, int start, int finish, boolean mutable) {
		this.set = set;
		this.start = start;
		this.finish = finish;
		this.mutable = mutable;
	}

	@Override
	public int size() {
		return finish - start;
	}
	
	@Override
	public boolean getBit(int index) {
		return set.get(adjIndex(index));
	}
	
	@Override
	public void fillWith(boolean value) {
		checkMutable();
		set.set(start, finish, value);
	}
	
	@Override
	public void setBit(int index, boolean value) {
		checkMutable();
		set.set(adjIndex(index), value);
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		checkMutable();
		index = adjIndex(index);
		boolean previous = set.get(index);
		set.set(index, value);
		return previous;
	}

//  TODO reimplement using a sparse variant?
//	@Override
//	public int countOnes() {
//		if (finish >= set.length() && start == 0) return set.cardinality();
//		return super.countOnes();
//	}
//	
//	@Override
//	public boolean isAllOnes() {
//		return set.nextClearBit(start) >= finish;
//	}
//	
//	@Override
//	public boolean isAllZeros() {
//		int i = set.nextSetBit(start);
//		return i < 0 || i >= finish;
//	}

//	@Override
//	public int writeTo(BitWriter writer) {
//		if (writer == null) throw new IllegalArgumentException("null writer");
//		// heuristic - don't bother to copy for small bit sets
//		if (size() < 64) return BitStore.super.writeTo(writer);
//		//TODO should we risk copying potentially large bitsets?
//		int size = Math.max(set.size(), to);
//		return BitVector.fromBitSet(set, size).range(from, to).writeTo(writer);
//	}

	@Override
	public void flip() {
		checkMutable();
		set.flip(start, finish);
	}
	
	@Override
	public BitSetBitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BitSetBitStore(set, from, to, mutable);
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public BitSetBitStore mutableCopy() {
		return new BitSetBitStore(set.get(start, finish), 0, finish - start, true);
	}
	
	@Override
	public BitStore immutableCopy() {
		return new BitSetBitStore(set.get(start, finish), 0, finish - start, false);
	}
	
	@Override
	public BitStore immutableView() {
		return new BitSetBitStore(set, start, finish, false);
	}
	
	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}
	
	private int adjIndex(int index) {
		if (index < 0) throw new IllegalArgumentException();
		index += start;
		if (index >= finish) throw new IllegalArgumentException();
		return index;
	}
}

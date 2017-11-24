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

import java.util.Arrays;

class BooleansBitStore extends AbstractBitStore implements BitStore {

	private final boolean[] bits;
	private final int start;
	private final int finish;
	private final boolean mutable;

	BooleansBitStore(boolean[] bits, int start, int finish, boolean mutable) {
		this.bits = bits;
		this.start = start;
		this.finish = finish;
		this.mutable = mutable;
	}

	// fundamental methods

	@Override
	public int size() {
		return finish - start;
	}

	@Override
	public boolean getBit(int index) {
		return bits[adjIndex(index)];
	}

	@Override
	public void setBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutability();
		bits[index] = value;
	}

	// acceleration methods

	@Override
	public void flipBit(int index) {
		index = adjIndex(index);
		checkMutability();
		bits[index] = !bits[index];
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutability();
		boolean previous = bits[index];
		bits[index] = value;
		return previous;
	}

	@Override
	public void setBits(int position, long value, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (length < 0) throw new IllegalArgumentException();
		if (length > 64) throw new IllegalArgumentException();
		int from = start + position;
		int to = from + length;
		if (to > finish) throw new IllegalArgumentException();
		checkMutability();
		for (int i = from; i < to; i++, value >>= 1) {
			bits[i] = (value & 1) != 0;
		}
	}

	@Override
	public void setBitsAsInt(int position, int value, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (length < 0) throw new IllegalArgumentException();
		if (length > 32) throw new IllegalArgumentException();
		int from = start + position;
		int to = from + length;
		if (to > finish) throw new IllegalArgumentException();
		checkMutability();
		for (int i = from; i < to; i++, value >>= 1) {
			bits[i] = (value & 1) != 0;
		}
	}

	@Override
	public void setStore(int position, BitStore store) {
		if (position < 0) throw new IllegalArgumentException();
		if (store == null) throw new IllegalArgumentException("null store");
		int from = start + position;
		int to = from + store.size();
		if (to > finish) throw new IllegalArgumentException();
		checkMutability();
		for (int i = from; i < to; i++) {
			bits[i] =  store.getBit(i - from);
		}
	}

	@Override
	public void fill() {
		checkMutability();
		Arrays.fill(bits, start, finish, true);
	}

	@Override
	public void clear() {
		checkMutability();
		Arrays.fill(bits, start, finish, false);
	}

	@Override
	public void flip() {
		checkMutability();
		for (int i = start; i < finish; i++) {
			bits[i] = !bits[i];
		}
	}

	// views

	@Override
	public BitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BooleansBitStore(bits, from, to, mutable);
	}

	// mutability

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public BitStore mutableCopy() {
		return copyAdj(start, finish, true);
	}

	@Override
	public BitStore immutableCopy() {
		return copyAdj(start, finish, false);
	}

	@Override
	public BitStore immutableView() {
		return new BooleansBitStore(bits, start, finish, false);
	}

	// private helper methods

	private int adjIndex(int index) {
		if (index < 0) throw new IllegalArgumentException();
		index += start;
		if (index >= finish) throw new IllegalArgumentException();
		return index;
	}

	private void checkMutability() {
		if (!mutable) throw new IllegalStateException("immutable");
	}

	private BooleansBitStore copyAdj(int from, int to, boolean mutable) {
		boolean[] subBits = Arrays.copyOfRange(bits, from, to);
		return new BooleansBitStore(subBits, 0, to - from, mutable);
	}
}

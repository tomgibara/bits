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

//TODO could provide accelerated implementations of more methods
class Bit extends SingleBitStore {

	// fields

	private boolean bit;

	// constructors

	Bit() {
		bit = false;
	}

	Bit(boolean bit) {
		this.bit = bit;
	}

	// fundamental methods

	@Override
	public boolean getBit(int index) {
		checkIndex(index);
		return bit;
	}

	// accelerating methods

	@Override
	public long getBits(int position, int length) {
		switch (position) {
		case 0:
			switch (length) {
			case 0: return 0L;
			case 1: return bit ? 1L : 0L;
			}
			break;
		case 1:
			if (length == 0L) return 0L;
			break;
		}
		throw new IllegalArgumentException();
	}

	// accelerating mutation methods

	@Override
	public void flipBit(int index) {
		checkIndex(index);
		bit = !bit;
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		checkIndex(index);
		boolean previous = bit;
		bit = value;
		return previous;
	}

	@Override
	public void setBits(int position, long value, int length) {
		switch (length) {
		case 0:
			checkPosition(position);
			return;
		case 1:
			if (position != 0) throw new IllegalArgumentException();
			bit = (value & 1) != 0;
			return;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void setStore(int position, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		switch (size) {
		case 0:
			checkPosition(position);
			return;
		case 1:
			if (position != 0) throw new IllegalArgumentException();
			bit = store.getBit(0);
			return;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void fill() {
		bit = true;
	}

	@Override
	public void clear() {
		bit = false;
	}

	@Override
	public void flip() {
		bit = !bit;
	}

	// shifting

	@Override
	public void shift(int distance, boolean fill) {
		if (distance == 0) return;
		bit = fill;
	}

	// fundamental mutation methods

	@Override
	public void setBit(int index, boolean value) {
		checkIndex(index);
		bit = value;
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return true;
	}

	// comparable methods

	@Override
	public int compareNumericallyTo(BitStore that) {
		return ImmutableBit.instanceOf(bit).compareNumericallyTo(that);
	}

	// package scoped methods

	@Override
	boolean getBit() {
		return bit;
	}

	// private utility methods

	private void checkPosition(int position) {
		if (position != 0 && position != 1) throw new IllegalArgumentException("invalid position");
	}

}

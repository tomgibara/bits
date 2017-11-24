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

class FlippedBitStore extends AbstractBitStore {

	private final BitStore store;
	private final int size;

	public FlippedBitStore(BitStore store) {
		this.store = store;
		size = store.size();
	}

	// fundamentals

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean getBit(int index) {
		return !store.getBit(index);
	}

	@Override
	public void setBit(int index, boolean value) {
		store.setBit(index, !value);
	}

	// acceleration

	@Override
	public long getBits(int position, int length) {
		return ~store.getBits(position, length);
	}

	@Override
	public int getBitsAsInt(int position, int length) {
		return ~store.getBitsAsInt(position, length);
	}

	@Override
	public void setBits(int position, long value, int length) {
		store.setBits(position, ~value, length);
	}

	@Override
	public void setBitsAsInt(int position, int value, int length) {
		store.setBitsAsInt(position, ~value, length);
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		return !store.getThenSetBit(index, !value);
	}

	@Override
	public void flipBit(int index) {
		store.flipBit(index);
	}

	@Override
	public void fill() {
		store.clear();
	}

	@Override
	public void clear() {
		store.fill();
	}

	@Override
	public void flip() {
		store.flip();
	}

	// views

	@Override
	public BitStore range(int from, int to) {
		return store.range(from, to).flipped();
	}

	@Override
	public BitStore flipped() {
		return store;
	}

	// mutability

	@Override
	public boolean isMutable() {
		return store.isMutable();
	}

}

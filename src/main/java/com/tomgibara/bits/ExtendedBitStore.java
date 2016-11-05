/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tomgibara.bits;

final class ExtendedBitStore extends AbstractBitStore {

	private final BitStore store;

	private final boolean extension;
	private final int finish; // of first zeros
	private final int start; // of second zeros
	private final int size; // total size

	ExtendedBitStore(BitStore store, boolean extension, int left, int right) {
		this.store = store;
		this.extension = extension;
		finish = right;
		start = finish + store.size();
		size = start + left;
	}

	// fundamental methods

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean getBit(int index) {
		if (index < 0 || index > size) throw new IllegalArgumentException("invalid index");
		return index >= finish && index < start ? store.getBit(index - finish) : extension;
	}

	// accelerating methods

	@Override
	public long getBits(int position, int length) {
		if (position < 0) throw new IllegalArgumentException("negative position");
		int limit = position + length;
		if (limit > size) throw new IllegalArgumentException("length too great");
		// contained in extensions
		if (position >= start || limit < finish) return extensionBits(length);
		// contained in store
		if (position >= finish && limit <= start) return store.getBits(position - finish, length);
		// overlaps extensions and store
		return super.getBits(position, length);
	}

	// view methods

	@Override
	public BitStore range(int from, int to) {
		if (from <= finish && to <= finish) return Bits.zeroBits(size).range(from, to);
		if (from >= start && to >= start) return Bits.zeroBits(size).range(from, to);
		if (from >= finish && to <= start) return store.range(from - finish, to - finish);
		return super.range(from, to);
	}

	@Override
	public ExtendedBitStore reversed() {
		return new ExtendedBitStore(store.reversed(), extension, finish, size - start);
	}

	@Override
	public ExtendedBitStore flipped() {
		return new ExtendedBitStore(store.flipped(), !extension, size - start, finish);
	}

	// I/O methods

	@Override
	public int writeTo(BitWriter writer) {
		int count = 0;
		count += writer.writeBooleans(false, size - start);
		count += store.writeTo(writer);
		count += writer.writeBooleans(false, finish);
		return count;
	}

	// private helper methods
	private long extensionBits(int length) {
		if (length < 0) throw new IllegalArgumentException("negative length");
		if (length > 64) throw new IllegalArgumentException("length exceeds 64");
		return !extension || length == 0 ? 0L : -1L >>> (64 - length);
	}

}

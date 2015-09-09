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

import java.math.BigInteger;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.streams.ByteWriteStream;
import com.tomgibara.streams.WriteStream;

public interface BitStore extends Mutability<BitStore> {

	int size();

	// accessing
	
	boolean getBit(int index);

	default byte getByte(int position) {
		return (byte) getBits(position, 8);
	}

	default short getShort(int position) {
		return (byte) getBits(position, 16);
	}

	default int getInt(int position) {
		return (int) getBits(position, 32);
	}

	default long getLong(int position) {
		return (int) getBits(position, 64);
	}
	
	default long getBits(int position, int length) {
		long bits = 0L;
		for (int i = position + length - 1; i >= position; i--) {
			bits <<= 1;
			if (getBit(i)) bits |= 1L;
		}
		return bits;
	}

	//TODO consider another name for this
	// perhaps one that matches existing Java method name
	// bitCount, cardinality?
	default int countOnes() {
		int size = size();
		int count = 0;
		for (int i = 0; i < size; i++) {
			if (getBit(i)) count++;
		}
		return count;
	}

	default boolean isAll(boolean value) {
		int size = size();
		for (int i = 0; i < size; i++) {
			if (getBit(i) != value) return false;
		}
		return true;
	}

	// mutating
	
	default void setBit(int index, boolean value) {
		throw new IllegalStateException("immutable");
	}

	default boolean getThenSetBit(int index, boolean value) {
		boolean previous = getBit(index);
		if (previous != value) setBit(index, value);
		return previous;
	}

	default void clear(boolean value) {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, value);
		}
	}

	default void setStore(int index, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int to = index + store.size();
		if (to > size()) throw new IllegalArgumentException("store size too great");
		for (int i = index; i < to; i++) {
			setBit(index, store.getBit(i - index));
		}
	}

	default void flip() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, !getBit(i));
		}
	}

	// testing
	
	default boolean testEquals(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (this.getBit(i) != store.getBit(i)) return false;
		}
		return true;
	}

	default boolean testIntersects(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (this.getBit(i) && store.getBit(i)) return true;
		}
		return false;
	}

	default boolean testContains(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (!this.getBit(i) && store.getBit(i)) return false;
		}
		return true;
	}

	default boolean testComplements(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (this.getBit(i) == store.getBit(i)) return false;
		}
		return true;
	}

	// io
	
	default int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		int size = size();
		for (int i = size - 1; i >= 0; i--) {
			writer.writeBoolean(getBit(i));
		}
		return size;
	}

	default void writeTo(WriteStream writer) {
		int start = size();
		int head = start & 7;
		if (head != 0) {
			byte value = (byte) getBits(start - head, head);
			writer.writeByte(value);
			start -= head;
		}
		for (start -=8; start >= 0; start -= 8) {
			writer.writeByte(getByte(start));
		}
	}
	
	default void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		int size = size();
		for (int i = size - 1; i >= 0; i--) {
			setBit(i, reader.readBoolean());
		}
	}

	//TODO implement readFrom(WriteStream writer)
	
	// views
	
	default BitStore range(int from, int to) {
		return Bits.newRangedView(this, from, to);
	}
	
	default byte[] toByteArray() {
		try (ByteWriteStream writer = new ByteWriteStream((size() + 7) >> 3)) {
			this.writeTo(writer);
			return writer.getBytes(false);
		}
	}
	
	default BigInteger toBigInteger() {
		return size() == 0 ? BigInteger.ZERO : new BigInteger(1, toByteArray());
	}

	default Number asNumber() {
		return Bits.asNumber(this);
	}

	// mutability methods
	
	@Override
	default boolean isMutable() {
		return false;
	}

	@Override
	default BitStore mutableCopy() {
		return BitVector.fromStore(this);
	}

	@Override
	default BitStore immutableCopy() {
		return mutableCopy().immutableView();
	}

	@Override
	default BitStore immutableView() {
		return Bits.newImmutableView(this);
	}
}

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
import java.util.ListIterator;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.streams.ByteWriteStream;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

public interface BitStore extends Mutability<BitStore> {

	// statics
	
	/**
	 * An operation that can modify one bit (the destination) based on the value
	 * of another (the source).
	 */

	public enum Operation {

		/**
		 * The destination bit is set to the value of the source bit.
		 */

		SET,

		/**
		 * The destination bit is set to true if and only if both the source and destination bits are true.
		 */

		AND,

		/**
		 * The destination bit is set to true if and only if the source and destination bits are not both false.
		 */

		OR,

		/**
		 * The destination bit is set to true if and only if exactly one of the source and destination bits is true.
		 */

		XOR;

		static final Operation[] values = values();
	}

	public static abstract class Op {

		abstract Operation getOperation();

		abstract void with(boolean value);

		abstract void withBit(int position, boolean value);

		abstract boolean getThenWithBit(int position, boolean value);

		abstract void withByte(int position, byte value);

		abstract void withShort(int position, short value);

		abstract void withInt(int position, short value);

		abstract void withLong(int position, short value);

		abstract void withBits(int position, long value, int length);

		abstract void withStore(BitStore store);

		abstract void withStore(int position, BitStore store);

		abstract void withBytes(int position, byte[] bytes, int offset, int length);

	}

	//TODO could extend to general sequences
	public abstract class Matches {

		public abstract BitStore store();
		
		public abstract BitStore sequence();
		
		public abstract Matches range(int from, int to);
		
		public abstract int count();

		public abstract int first();

		public abstract int last();

		public abstract int next(int position);

		public abstract int previous(int position);

	}
	
	public abstract class BitMatches extends Matches {

		public abstract boolean bit();
		
		//TODO is there a better name for this?
		public abstract boolean isAll();
		
		public abstract ListIterator<Integer> positions();

		public abstract ListIterator<Integer> positions(int position);

	}

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

	// mutating
	
	default void setBit(int index, boolean value) {
		throw new IllegalStateException("immutable");
	}

	default void flipBit(int index) {
		setBit(index, !getBit(index));
	}

	default boolean getThenSetBit(int index, boolean value) {
		boolean previous = getBit(index);
		if (previous != value) setBit(index, value);
		return previous;
	}
	
	default void setBits(int position, long value, int length) {
		int to = position + length;
		if (to > size()) throw new IllegalArgumentException("length too great");
		for (int i = position; i < to; i++, value >>= 1) {
			setBit(i, (value & 1) != 0);
		}
	}

	default void setStore(int position, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int to = position + store.size();
		if (to > size()) throw new IllegalArgumentException("store size too great");
		for (int i = position; i < to; i++) {
			setBit(i, store.getBit(i - position));
		}
	}

	default void clearWithOnes() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, true);
		}
	}

	default void clearWithZeros() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, false);
		}
	}

	default void flip() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, !getBit(i));
		}
	}
	
	// operations

	default Op op(Operation operation) {
		if (operation == null) throw new IllegalArgumentException("null operation");
		switch (operation) {
		case SET: return set();
		case AND: return and();
		case OR:  return or();
		case XOR: return xor();
		default:
			throw new IllegalArgumentException("Unsupported operation");
		}
	}

	default Op set() {
		return new BitStoreOp.Set(this);
	}

	default Op and() {
		return new BitStoreOp.And(this);
	}

	default Op or() {
		return new BitStoreOp.Or(this);
	}

	default Op xor() {
		return new BitStoreOp.Xor(this);
	}

	// matching

	default Matches match(BitStore sequence) {
		if (sequence == null) throw new IllegalArgumentException("null sequence");
		if (sequence.size() != 1) throw new UnsupportedOperationException("only single bit sequences are currently supported");
		return match(sequence.getBit(0));
	}
	
	default BitMatches ones() {
		return new BitStoreMatches.Ones(this);
	}

	default BitMatches zeros() {
		return new BitStoreMatches.Zeros(this);
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

	// note: bit writer writes backwards from the most significant bit
	default BitWriter openWriter() {
		return openWriter(size());
	}

	// note: bit writer writes backwards from the specified position
	// the first bit written has index position - 1.
	default BitWriter openWriter(int position) {
		return Bits.newBitWriter(this, position);
	}
	
	default BitWriter openWriter(Operation operation, int position) {
		return Bits.newBitWriter(operation, this, position);
	}
	
	default BitReader openReader() {
		return openReader(size());
	}
	
	default BitReader openReader(int position) {
		return Bits.newBitReader(this, position);
	}

	default int writeTo(BitWriter writer) {
		int size = size();
		Bits.transfer(openReader(), writer, size);
		return size;
	}

	default void readFrom(BitReader reader) {
		Bits.transfer(reader, openWriter(), size());
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
	
	default void readFrom(ReadStream reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		BitWriter writer = openWriter();
		int start = size();
		int head = start & 7;
		if (head != 0) {
			writer.write(reader.readByte(), head);
		}
		for (int i = (start >> 3) - 1; i >= 0; i--) {
			writer.write(reader.readByte(), 8);
		}
	}

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

	default String toString(int radix) {
		return toBigInteger().toString(radix);
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
		return new ImmutableBitStore(this);
	}

	// convenience methods

	default BitMatches match(boolean bit) {
		return bit ? ones() : zeros();
	}

	default void clearWith(boolean value) {
		if (value) {
			clearWithOnes();
		} else {
			clearWithZeros();
		}
	}

}

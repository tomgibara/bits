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
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.SortedSet;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.streams.ByteWriteStream;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

/**
 * <p>
 * An abstraction that provides multiple forms of bit manipulation over fixed
 * length bit sequences. The interface provides a large number of methods but
 * most implementations are actually very lightweight, typically being small
 * wrappers around primitives or primitive arrays.
 * 
 * <p>
 * Default implementations are provided for every method other than the
 * 'fundamental methods' (of which there are two). Whether or not other method
 * implementations are provided for a class will depend on its intended use. The
 * methods are grouped as follows:
 * 
 * <ul>
 * <dt>Fundamental methods
 * <dd>These methods ({@link #setBit(int, boolean)} and {@link #size()} must be
 * implemented since they form the irreducible basis of all other functionality
 * provided by the interface.
 * 
 * <dt>Fundamental mutation methods
 * <dd>Implementing this single method {@link #setBit(int, boolean)} makes
 * mutation possible; ({@link #isMutable()} should also be overridden to return
 * <code>true</code in this case.
 * 
 * <dt>Accelerating methods
 * <dd>These methods provide functions that higher-level functions depend on for
 * basic data access. Replace default implementations of these methods to make
 * easy performance improvements.
 * 
 * <dt>Accelerating mutation methods
 * <dd>These methods provide functions that higher-level functions depend on for
 * performing basic operations. Replace default implementations of these methods
 * to make easy performance improvements.
 * 
 * <dt>Operations
 * <dd>These methods provide {@link Op} implementations for performing logical
 * operations against the {@link BitStore}.
 * 
 * <dt>Matches
 * <dd>These methods provide {@link Matches} and {@link BitMatches}
 * implementations through which bit patterns may be analyzed.
 * 
 * <dt>Tests
 * <dd>These methods allow bit stores of the same size to be compared.
 * 
 * <dt>I/O
 * <dd>These methods provide several different ways to read and write the binary
 * contents of a {@link BitStore}. Performance improvements can be expected by
 * implementing these methods and matching the read/write strategies to the
 * underlying data store.
 * 
 * <dt>View
 * <dd>These methods allow the {@link BitSet} to be viewed-as (
 * <code>as<code> methods) or copied-into (<code>to</code> methods) other
 * classes.
 * 
 * <dt>Mutability
 * <dd>These methods provide a standard mechanism for client code to control the
 * mutability of a {@link BitStore}. Default implementations are provided but
 * natural alternatives may provide much greater efficiency.
 * 
 * <dt>Comparable
 * <dd>These methods can order bit stores numerically or lexically.
 * 
 * <dt>Convenience
 * <dd>These are methods with obvious implementations in the presence of the
 * other methods on this interface but which are nevertheless commonly useful to
 * client-code. This includes the {@link #compareTo(BitStore)} method inherited
 * from the <code>Comparable</code> interface which is synonymous with
 * {@link #compareNumericallyTo(BitStore)}.
 * 
 * @author Tom Gibara
 *
 */

public interface BitStore extends Mutability<BitStore>, Comparable<BitStore> {

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

	/**
	 * A test that can be made of one {@link BitStore} against another.
	 */

	public enum Test {

		/**
		 * Whether two {@link BitStore} have the same pattern of true/false-bits.
		 */

		EQUALS,

		/**
		 * Whether there is no position at which both {@link BitStore}s have
		 * a true-bit.
		 */

		EXCLUDES,

		/**
		 * Whether one {@link BitStore} has true-bits at every position that
		 * another does.
		 */

		CONTAINS,

		/**
		 * Whether one {@link BitStore} is has zero bits at exactly every
		 * position that another has one bits.
		 */

		COMPLEMENTS;

		static final Test[] values = values();
	}

	/**
	 * Operations that can be conducted on a {@link BitStore} using one of the
	 * logical operations. The supported operations are {@link Operation#SET},
	 * {@link Operation#AND}, {@link Operation#OR} and {@link Operation#XOR}.
	 * Instances of this class are obtained via the {@link BitStore#set()},
	 * {@link BitStore#and()}, {@link BitStore#or()}, {@link BitStore#xor()} and
	 * {@link BitStore#op(Operation)} methods.
	 * 
	 * @author Tom Gibara
	 *
	 */

	public static abstract class Op {

		public abstract Operation getOperation();

		public abstract void with(boolean value);

		public abstract void withBit(int position, boolean value);

		public abstract boolean getThenWithBit(int position, boolean value);

		public abstract void withByte(int position, byte value);

		public abstract void withShort(int position, short value);

		public abstract void withInt(int position, short value);

		public abstract void withLong(int position, short value);

		public abstract void withBits(int position, long value, int length);

		public abstract void withStore(BitStore store);

		public abstract void withStore(int position, BitStore store);

		public abstract void withBytes(int position, byte[] bytes, int offset, int length);

		public abstract BitWriter openWriter(int position);
		
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

		public abstract BitMatches range(int from, int to);
		
		public abstract boolean bit();
		
		//TODO is there a better name for this?
		public abstract boolean isAll();

		public abstract boolean isNone();
		
		public abstract ListIterator<Integer> positions();

		public abstract ListIterator<Integer> positions(int position);

		public abstract SortedSet<Integer> asSet();
	}
	
	public abstract class Tests {
		
		public abstract Test getTest();
		
		public abstract boolean store(BitStore store);
		
		public abstract boolean bits(long bits);

	}
	
	public abstract class Permutes {
		
		//TODO inherit
		public abstract void transpose (int i, int j);
	
		public abstract void rotate(int distance);
		
		public abstract void reverse();
		
		public abstract void shift(int distance, boolean fill);
		
		public abstract void shuffle(Random random);
	}
	
	// fundamental methods

	int size();

	boolean getBit(int index);

	// fundamental mutation methods
	
	default void setBit(int index, boolean value) {
		throw new IllegalStateException("immutable");
	}
	
	// accelerating methods

	default long getBits(int position, int length) {
		long bits = 0L;
		for (int i = position + length - 1; i >= position; i--) {
			bits <<= 1;
			if (getBit(i)) bits |= 1L;
		}
		return bits;
	}

	// accelerating mutation methods

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
		if (sequence.size() == 1) return match(sequence.getBit(0));
		return new BitStoreMatches(this, sequence);
	}
	
	default BitMatches ones() {
		return new BitStoreBitMatches.Ones(this);
	}

	default BitMatches zeros() {
		return new BitStoreBitMatches.Zeros(this);
	}

	// testing

	default Tests equals() {
		return new BitStoreTests.Equals(this);
	}
	
	default Tests excludes() {
		return new BitStoreTests.Excludes(this);
	}
	
	default Tests contains() {
		return new BitStoreTests.Contains(this);
	}
	
	default Tests complements() {
		return new BitStoreTests.Complements(this);
	}

	// I/O

	// note: bit writer writes backwards from the specified position
	// the first bit written has index position - 1.
	default BitWriter openWriter(int position) {
		return Bits.newBitWriter(this, position);
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
		if (writer == null) throw new IllegalArgumentException("null writer");
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
	
	default Permutes permute() {
		return new BitStorePermutes(this);
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

	default BitSet toBitSet() {
		BitMatches ones = ones();
		int previous = ones.last();
		final BitSet bitSet = new BitSet(previous + 1);
		while (previous > -1) {
			bitSet.set(previous);
			previous = ones.previous(previous);
		}
		return bitSet;
	}

	default Number asNumber() {
		return Bits.asNumber(this);
	}
	
	default List<Boolean> asList() {
		return new BitStoreList(this);
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

	// comparable methods
	
	default int compareNumericallyTo(BitStore that) {
		if (this == that) return 0; // cheap check
		if (that == null) throw new IllegalArgumentException("that");
		return Bits.compareNumeric(this, that);
	}
	
	default int compareLexicallyTo(BitStore that) {
		if (this == that) return 0; // cheap check
		if (that == null) throw new IllegalArgumentException("that");
		int s1 = this.size();
		int s2 = that.size();
		if (s1 == s2) return Bits.compareNumeric(this, that);
		return s1 > s2 ?
				  Bits.compareLexical(this, that) :
				- Bits.compareLexical(that, this);
	}

	// convenience methods

	default void clearWith(boolean value) {
		if (value) {
			clearWithOnes();
		} else {
			clearWithZeros();
		}
	}

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

	default BitMatches match(boolean bit) {
		return bit ? ones() : zeros();
	}

	default Tests test(Test test) {
		if (test == null) throw new IllegalArgumentException("null test");
		switch (test) {
		case EQUALS:      return equals();
		case CONTAINS:    return contains();
		case EXCLUDES:    return excludes();
		case COMPLEMENTS: return complements();
		default: throw new IllegalStateException("Unexpected test");
		}
	}
	
	default BitWriter openWriter() {
		return openWriter(size());
	}

	default BitReader openReader() {
		return openReader(size());
	}
	
	default BitStore rangeFrom(int from) {
		return range(from, size());
	}
	
	default BitStore rangeTo(int to) {
		return range(0, to);
	}
	
	default int compareTo(BitStore that) {
		if (that == null) throw new NullPointerException(); // as per compareTo() contract
		return compareNumericallyTo(that);
	}
	
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
	
}

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
import com.tomgibara.fundament.Transposable;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
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
 * <dd>These methods ({@link #getBit(int)} and {@link #size()} must be
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
	 * A test that can be made of one {@link BitStore} against another.
	 */

	enum Test {

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
	 */

	interface Op {

		/**
		 * The operation applied by these methods.
		 * 
		 * @return the operation
		 */

		Operation getOperation();

		/**
		 * Applies the operation to every bit using the supplied bit value.
		 * 
		 * @param value the bit value to apply
		 */

		void with(boolean value);

		/**
		 * Applies the operation to a single bit using the supplied bit value.
		 * 
		 * @param position
		 *            the index of the bit to be operated on
		 * @param value
		 *            the bit value to apply
		 */

		void withBit(int position, boolean value);

		/**
		 * Returns an existing bit before applying the supplied value using the
		 * operation specified by this object.
		 * 
		 * @param position
		 *            the index of the bit to be returned and then operated on
		 * @param value
		 *            the bit value to apply
		 * @return the bit value prior to the operation
		 */

		boolean getThenWithBit(int position, boolean value);

		/**
		 * Applies the operation to a range of 8 bits using bits of the supplied
		 * byte.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param value
		 *            the bits to apply
		 */

		void withByte(int position, byte value);

		/**
		 * Applies the operation to a range of 16 bits using bits of the
		 * supplied short.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param value
		 *            the bits to apply
		 */

		void withShort(int position, short value);

		/**
		 * Applies the operation to a range of 32 bits using bits of the
		 * supplied int.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param value
		 *            the bits to apply
		 */

		void withInt(int position, short value);

		/**
		 * Applies the operation to a range of 64 bits using bits of the
		 * supplied long.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param value
		 *            the bits to apply
		 */

		void withLong(int position, short value);

		/**
		 * Applies the operation to a range of bits using bits from the supplied
		 * long. When only a subset of the long bits is required (when the
		 * length is less than 64) the least-significant bits are used.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param length
		 *            the number of bits in the range
		 * @param value
		 *            the bits to apply
		 */

		void withBits(int position, long value, int length);

		/**
		 * Applies the operation all bits, using the bits of another
		 * {@link BitStore}. The bits applied must equal
		 * 
		 * @param store
		 *            a {@link BitStore} of the same size
		 */
		
		void withStore(BitStore store);

		/**
		 * Applies the operation to a range of bits using bits from the supplied
		 * {@link BitStore}.
		 * 
		 * @param position
		 *            the smallest index in the range
		 * @param store
		 *            contains the bits to apply
		 */

		void withStore(int position, BitStore store);

		/**
		 * Applies bits sourced from a byte array. The bit ordering applied to
		 * the byte array is that specified by
		 * {@link Bits#asStore(byte[], int, int)}
		 * 
		 * @param position
		 *            the position at which the bits will be applied
		 * @param bytes
		 *            a byte containing the bit values to apply
		 * @param offset
		 *            the index of the first bit from byte array to be applied
		 * @param length
		 *            the number of bits in the range
		 */

		void withBytes(int position, byte[] bytes, int offset, int length);

		/**
		 * Opens a new writer that writes bits into the underlying
		 * {@link BitStore} using operation specified by this object. Note that
		 * the {@link BitWriter} will <em>write in big-endian order</em> which
		 * this means that the first bit is written at the largest index,
		 * working downwards to the least index.
		 * 
		 * @param finalPos
		 *            the (exclusive) index at which the writer stops; less than
		 *            <code>initialPos</code>
		 * @param initialPos
		 *            the (inclusive) index at which the writer starts; greater
		 *            than <code>finalPos</code>
		 * @return a {@link BitWriter} into the store
		 */

		BitWriter openWriter(int finalPos, int initialPos);
		
	}
	
	/**
	 * An iterator over the positions of a bit sequence in a {@link BitStore}.
	 * This interface extends the <code>ListIterator</code> interface and
	 * honours all of its semantics with the caveat that the
	 * {@link #add(Integer)} and {@link #remove()} methods are only honoured for
	 * iterators over matched single-bit sequences
	 * 
	 * @see Matches
	 */

	interface Positions extends ListIterator<Integer> {

		/**
		 * Indicates whether the iterator moves over pattern sequences such that
		 * successive matched ranges do not over overlap.
		 * 
		 * @return true if position matches do not overlap, false otherwise
		 * 
		 * @see Matches#disjointPositions()
		 */

		boolean isDisjoint();
		
		/**
		 * <p>
		 * The next matched position. This is equivalent to {@link #next()} but
		 * may be expected to be slightly more efficient, with the differences
		 * being that:
		 * 
		 * <ul>
		 * <li>The position is returned as a primitive is returned, not a boxed
		 * primitive.
		 * <li>If there's no position, the value of {@link BitStore#size()} is
		 * returned instead of an exception being raised.
		 * </ul>
		 * 
		 * <p>
		 * Otherwise, the effect of calling the method is the same.
		 * 
		 * @return the next position, or the first invalid index if there is no
		 *         further position
		 */

		int nextPosition();

		/**
		 * <p>
		 * The previous matched position. This is equivalent to
		 * {@link #previous()} but may be expected to be slightly more
		 * efficient, with the differences being that:
		 * 
		 * <ul>
		 * <li>The position is returned as a primitive is returned, not a boxed
		 * primitive.
		 * <li>If there's no position, the value of {@link BitStore#size()} is
		 * returned instead of an exception being raised.
		 * </ul>
		 * 
		 * <p>
		 * Otherwise, the effect of calling the method is the same.
		 * 
		 * @return the previous position, or -1 if there is no further position
		 */

		int previousPosition();

		/**
		 * Replaces the last match returned by the {@link #next()}/
		 * {@link #nextPosition()} and {@link #previous()}/
		 * {@link #previousPosition()} methods, with the supplied bits.
		 * 
		 * @param replacement
		 *            the bits to replace the matched sequence
		 * @throws IllegalArgumentException
		 *             if the replacement size does not match the matched
		 *             sequence size
		 * @throws IllegalStateException
		 *             if the underlying bit store is immutable, or if no call
		 *             to {@link #previous()} or {@link #next()} has been made
		 * @see Matches#replaceAll(BitStore)
		 */

		void replace(BitStore replacement);

		/**
		 * Uniformly replaces the bits of last match returned by the
		 * {@link #next()}/ {@link #nextPosition()} and {@link #previous()}/
		 * {@link #previousPosition()} methods, with the specified bit value.
		 * 
		 * @param bit
		 *            the bit value with which to replace the matched bits
		 * @throws IllegalStateException
		 *             if the underlying bit store is immutable, or if no call
		 *             to {@link #previous()} or {@link #next()} has been made
		 * @see Matches#replaceAll(BitStore)
		 */

		void replace(boolean bits);
	}

	/**
	 * Provides information about the positions at which a fixed sequence of
	 * bits occurs.
	 * 
	 * @see BitStore#match(BitStore)
	 * @see BitStore.BitMatches
	 */

	interface Matches {

		/**
		 * The store over which the matches are being reported.
		 * 
		 * @return the bit store being matched over
		 */

		BitStore store();

		/**
		 * A {@link BitStore} containing the bit sequence being matched
		 * 
		 * @return the bit sequence being matched
		 */

		BitStore sequence();
		
		/**
		 * Returns an {@link Matches} object that is limited to a subrange. This
		 * is logically equivalent to
		 * <code>store().range(from, to).matches(sequence())</code>. The store
		 * reported by the returned matches reflects the specified range.
		 * 
		 * @param from
		 *            the position at which the range starts
		 * @param to
		 *            the position at which the range ends
		 * @return a {@link Matches} object over the specified range.
		 */

		Matches range(int from, int to);

		/**
		 * The number of matches over the entire store. Overlapping matches are
		 * included in the count, for example, the count of the sequence "101"
		 * over "10101" would be 2.
		 * 
		 * @return the number of available matches
		 */

		int count();

		/**
		 * The position of the first match. If there is no match, the store size
		 * is returned.
		 * 
		 * @return the position of the first match, or the store sizes if there
		 *         is no match.
		 */

		int first();

		/**
		 * The position of the last match. If there is no match, -1 is returned.
		 * 
		 * @return the position of the last match or -1 if there is no match.
		 */

		int last();

		/**
		 * The position of the first match that occurs at an index greater than
		 * or equal to the specified position. If there is no match, the store
		 * size is returned.
		 * 
		 * @return the position of the first subsequent match, or the store
		 *         sizes if there is no match.
		 */

		int next(int position);

		/**
		 * The position of the first match that occurs at an index less than the
		 * specified position. If there is no match, -1 is returned.
		 * 
		 * @return the position of the first prior match, or -1 if there is no
		 *         match.
		 */

		int previous(int position);

		/**
		 * Provides iteration over the positions at which matches occur.
		 * Iteration begins at the start of the matched range. The matches
		 * included in this iteration may overlap.
		 * 
		 * @return the match positions
		 */

		Positions positions();

		/**
		 * Provides iteration over the positions at which matches occur.
		 * Iteration begins at the specified position. The matches included in
		 * this iteration may overlap.
		 * 
		 * @param position
		 *            the position at which iteration begins
		 * @return the match positions
		 */

		Positions positions(int position);

		/**
		 * Provides iteration over the positions at which matches occur.
		 * Iteration begins at the start of the matched range. The matches
		 * included in this iteration will not overlap.
		 * 
		 * @return the match positions that do not overlap
		 */

		Positions disjointPositions();

		/**
		 * Replaces all non-overlapping matches of the sequence with a new
		 * sequence of the same size.
		 * 
		 * @param replacement
		 *            a replacement bit sequence
		 * @throws IllegalArgumentException
		 *             if the replacement size does not match the sequence size.
		 * @throws IllegalStateException
		 *             if the underlying store is immutable
		 * @see #disjointPositions()
		 */

		void replaceAll(BitStore replacement);

		/**
		 * Replaces all the bits of every non-overlapping match with the
		 * specified bit.
		 * 
		 * @param bits
		 *            the replacement for matched bits
		 * @throws IllegalStateException
		 *             if the underlying store is immutable
		 * @see #disjointPositions()
		 */

		void replaceAll(boolean bits);
	}

	/**
	 * Provides information about the positions of 1s or 0s in a {@link BitStore}.
	 */

	interface BitMatches extends Matches {

		@Override
		BitMatches range(int from, int to);
		
		/**
		 * Whether 1s are being matched.
		 * 
		 * @return bit value being matched
		 */

		boolean bit();

		/**
		 * Whether the {@link BitStore} consists entirely
		 * of the matched bit value.
		 * 
		 * @return true if and only if all bits in the store have the value of
		 *         {@link #bit()}
		 */

		boolean isAll();

		/**
		 * Whether none of the bits in the {@link BitStore} have the matched bit
		 * value.
		 * 
		 * @return true if and only if none of the bits in the store have the
		 *         value of {@link #bit()}
		 */

		boolean isNone();

		/**
		 * The matched bit positions as a sorted set. The returned set is a live
		 * view over the {@link BitStore} with mutations of the store being
		 * reflected in the set and vice versa. Attempting to add integers to
		 * the set that lie outside the range of valid bit indexes in the store
		 * will fail.
		 * 
		 * @return a set view of the matched bit positions
		 */

		SortedSet<Integer> asSet();

	}
	
	interface Tests {
		
		Test getTest();
		
		boolean store(BitStore store);
		
		boolean bits(long bits);

	}
	
	interface Permutes extends Transposable {
		
		void rotate(int distance);
		
		void reverse();
		
		void shuffle(Random random);
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

	default void fillWithOnes() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, true);
		}
	}

	default void fillWithZeros() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, false);
		}
	}

	default void flip() {
		int size = size();
		for (int i = 0; i < size; i++) {
			flipBit(i);
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

	// shifting

	default void shift(int distance, boolean fill) {
		int size = size();
		if (size == 0) return;
		if (distance == 0) return;

		//TODO have separate methods for true/false fill?
		//TODO this capable of optimization in some cases
		if (distance > 0) {
			int j = size - 1;
			for (int i = j - distance; i >= 0; i--, j--) {
				setBit(j, getBit(i));
			}
			range(0, j + 1).fillWith(fill);
		} else {
			int j = 0;
			for (int i = j - distance; i < size; i++, j++) {
				setBit(j, getBit(i));
			}
			range(j, size).fillWith(fill);
		}
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
	default BitWriter openWriter(int finalPos, int initialPos) {
		return Bits.newBitWriter(this, finalPos, initialPos);
	}
	
	default BitReader openReader(int finalPos, int initialPos) {
		return Bits.newBitReader(this, finalPos, initialPos);
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
	
	default BitStore flipped() {
		return new FlippedBitStore(this);
	}
	
	default BitStore reversed() {
		return new ReversedBitStore(this);
	}
	
	default Permutes permute() {
		return new BitStorePermutes(this);
	}
	
	default byte[] toByteArray() {
		StreamBytes bytes = Streams.bytes((size() + 7) >> 3);
		writeTo(bytes.writeStream());
		return bytes.directBytes();
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

	default void fillWith(boolean value) {
		if (value) {
			fillWithOnes();
		} else {
			fillWithZeros();
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
		return openWriter(0, size());
	}

	default BitReader openReader() {
		return openReader(0, size());
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
		return getBits(position, 64);
	}

}

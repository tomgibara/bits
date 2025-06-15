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
 * <dl>
 * <dt>Fundamental methods
 * <dd>These methods ({@link #getBit(int)} and {@link #size()} must be
 * implemented since they form the irreducible basis of all other functionality
 * provided by the interface.
 *
 * <dt>Fundamental mutation methods
 * <dd>Implementing this single method {@link #setBit(int, boolean)} makes
 * mutation possible; ({@link #isMutable()} should also be overridden to return
 * <code>true</code> in this case.
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
 * <dt>Shifting
 * <dd>A single method that provides the ability to shift bits left or right
 * within the {@link BitStore}. Note that rotations are provided via
 * {@link #permute()}.
 *
 * <dt>Matches
 * <dd>These methods provide {@link Matches} and {@link BitMatches}
 * implementations through which bit patterns may be analyzed.
 *
 * <dt>Tests
 * <dd>These methods allow bit stores of the same size to be compared logically.
 *
 * <dt>I/O
 * <dd>These methods provide several different ways to read and write the binary
 * contents of a {@link BitStore}. Performance improvements can be expected by
 * implementing these methods and matching the read/write strategies to the
 * underlying data store.
 *
 * <dt>View
 * <dd>These methods allow the {@link BitSet} to be viewed-as ( <code>as</code>
 * methods) or copied-into (<code>to</code> methods) other classes.
 *
 * <dt>Mutability
 * <dd>These methods are inherited from the <code>Mutability</code> interface
 * and provide a standard mechanism for client code to control the mutability of
 * a {@link BitStore}. Default implementations are provided but natural
 * alternatives may provide much greater efficiency.
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
 * </dl>
 *
 * <p>
 * All methods on the {@link BitStore} interface are documented with the group
 * to which they belong, allowing implementors to choose methods for overriding
 * accordingly.
 *
 * <p>
 * In general all single-bit valued method parameters and returns are typed as
 * booleans with true corresponding to <code>1</code> and false corresponding to
 * <code>0</code>.
 *
 * <p>
 * In keeping with Java standards, bits are operated-on and exposed-as
 * <em>big-endian</em>. This means that, where bit sequences are input/output
 * from methods, the least-significant bit is always on the right and the most
 * significant bit is on the left. So, for example, the <code>toString()</code>
 * method will contain the most significant bit in the character at index 0 in
 * the string. Naturally, In the cases where this class is used without
 * externalizing the bit representation, this distinction is irrelevant.
 * </p>
 *
 * <p>
 * A consequence of this is that, in methods that are defined over ranges of
 * bits, the <em>from</em> and <em>to</em> parameters define the rightmost and
 * leftmost indices respectively, when the {@link BitStore} is viewed as a
 * binary number. As per Java conventions, all <em>from</em> parameters are
 * inclusive and all <em>to</em> parameters are exclusive.
 * </p>
 *
 * <p>
 * Implementations of this interface may also be expected to provide the
 * following:
 *
 * <ul>
 *
 * <li>
 * A <code>hashCode()</code> implementation that computes a hash using the 32
 * bit Murmur3 hash algorithm over byte sequence generated by
 * {@link #writeTo(WriteStream)}. A convenient way of meeting this requirement
 * is to use {@link Bits#bitStoreHasher()}.
 *
 * <li>
 * A <code>equals()</code> implementation in which equality holds if and only if
 * the supplied object is a {@link BitStore} implementation with equal size and
 * bits.
 *
 * <li>
 * A <code>toString()</code> implementation that returns the {@link BitStore} as
 * an unsigned binary number, consisting only of zeros and ones. Exceptionally
 * (as a numeric representation), bit stores of zero size should return an empty
 * string. The library provides a convenient method for producing this string
 * representation via the {@link Bits#toString(BitStore)} method.
 *
 * </ul>
 *
 * <p>
 * A convenient means of satisfying these requirements is to extend the
 * {@link AbstractBitStore} class.
 *
 * @author Tom Gibara
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

		void withInt(int position, int value);

		/**
		 * Applies the operation to a range of 64 bits using bits of the
		 * supplied long.
		 *
		 * @param position
		 *            the smallest index in the range
		 * @param value
		 *            the bits to apply
		 */

		void withLong(int position, long value);

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
		 * @see #openWriter(int, int)
		 */

		BitWriter openWriter(int finalPos, int initialPos);

	}

	/**
	 * An iterator over the positions of a bit sequence in a {@link BitStore}.
	 * This interface extends the <code>ListIterator</code> interface and
	 * honours all of its semantics with the caveat that the
	 * {@link #add(Object)} and {@link #remove()} methods are only honoured for
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
		 * @see DisjointMatches#replaceAll(BitStore)
		 */

		void replace(BitStore replacement);

		/**
		 * Uniformly replaces the bits of last match returned by the
		 * {@link #next()}/ {@link #nextPosition()} and {@link #previous()}/
		 * {@link #previousPosition()} methods, with the specified bit value.
		 *
		 * @param bits
		 *            the bit value with which to replace the matched bits
		 * @throws IllegalStateException
		 *             if the underlying bit store is immutable, or if no call
		 *             to {@link #previous()} or {@link #next()} has been made
		 * @see DisjointMatches#replaceAll(boolean)
		 */

		void replace(boolean bits);
	}

	/**
	 * Provides information about the positions at which a fixed sequence of
	 * bits occurs.
	 *
	 * @see BitStore#match(BitStore)
	 * @see BitStore.OverlappingMatches
	 * @see BitStore.DisjointMatches
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
		 * The number of matches over the entire store. Depending on whether the
		 * {@link Matches} is {@link OverlappingMatches} (resp.
		 * {@link DisjointMatches}), overlapped sequences will (resp. will not)
		 * be included in the count.
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
		 * @param position
		 *            position from which the next match should be found
		 * @return the position of the first subsequent match, or the store
		 *         sizes if there is no match.
		 */

		int next(int position);

		/**
		 * The position of the first match that occurs at an index less than the
		 * specified position. If there is no match, -1 is returned.
		 *
		 * @param position
		 *            position from which the previous match should be found
		 * @return the position of the first prior match, or -1 if there is no
		 *         match.
		 */

		int previous(int position);

		/**
		 * Provides iteration over the positions at which matches occur.
		 * Iteration begins at the start of the matched range. If the
		 * {@link Matches} is {@link OverlappingMatches} (resp.
		 * {@link DisjointMatches}) the matches included in this iteration may
		 * (resp. may not) overlap.
		 *
		 * @return the match positions
		 */

		Positions positions();

		/**
		 * Provides iteration over the positions at which matches occur.
		 * Iteration begins at the specified position. If the {@link Matches} is
		 * {@link OverlappingMatches} (resp. {@link DisjointMatches}) the
		 * matches included in this iteration may (resp. may not) overlap.
		 *
		 * @param position
		 *            the position at which iteration begins
		 * @return the match positions
		 */

		Positions positions(int position);

	}

	/**
	 * Matches a fixed bit sequences within a {@link BitStore} including
	 * overlapping subsequences. For example, the overlapped count of the
	 * sequence "101" over "10101" would be 2 with matches at indices 0 and 2.
	 */

	interface OverlappingMatches extends Matches {

		/**
		 * Disjoint matches of the same bit sequence over the same
		 * {@link BitStore}.
		 *
		 * @return disjoint matches
		 */

		DisjointMatches disjoint();

		@Override
		OverlappingMatches range(int from, int to);

	}

	/**
	 * Matches a fixed bit sequences within a {@link BitStore} including
	 * overlapping subsequences. For example, the disjoint count of the
	 * sequence "101" over "10101" would be 1, the single match at index 0.
	 */

	interface DisjointMatches extends Matches {

		/**
		 * Overlapping matches of the same bit sequence over the same
		 * {@link BitStore}.
		 *
		 * @return overlapping matches
		 */

		OverlappingMatches overlapping();

		@Override
		DisjointMatches range(int from, int to);

		/**
		 * Whether every bit in the {@link BitStore} is accommodated within a
		 * match of the bit sequence.
		 *
		 * @return whether the matches cover every bit in the {@link BitStore}
		 */

		boolean isAll();

		/**
		 * Whether there are no matched bits.
		 *
		 * @return whether there are no matches over the {@link BitStore}
		 */

		boolean isNone();

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
		 */

		void replaceAll(boolean bits);

	}

	/**
	 * Provides information about the positions of 1s or 0s in a
	 * {@link BitStore}.
	 */

	interface BitMatches extends OverlappingMatches, DisjointMatches {

		@Override
		BitMatches overlapping();

		@Override
		BitMatches disjoint();

		@Override
		BitMatches range(int from, int to);

		/**
		 * Whether the {@link BitStore} consists entirely
		 * of the matched bit value.
		 *
		 * @return true if and only if all bits in the store have the value of
		 *         {@link #bit()}
		 */

		@Override
		boolean isAll();

		/**
		 * Whether none of the bits in the {@link BitStore} have the matched bit
		 * value.
		 *
		 * @return true if and only if none of the bits in the store have the
		 *         value of {@link #bit()}
		 */

		@Override
		boolean isNone();

		/**
		 * Whether 1s are being matched.
		 *
		 * @return bit value being matched
		 */

		boolean bit();

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

	/**
	 * Performs tests of a fixed type against a {@link BitStore}; in this
	 * documentation, referred to as the <em>source</em>. To perform tests
	 * against a subrange of the source use the {@link BitStore#range(int, int)}
	 * method.
	 *
	 * @see Test
	 * @see BitStore#range(int, int)
	 */

	interface Tests {

		/**
		 * The type of tests performed by this object.
		 *
		 * @return the type of test
		 */

		Test getTest();

		/**
		 * Tests a {@link BitStore} against the source.
		 *
		 * @param store
		 *            the bits tested against
		 * @return whether the test succeeds
		 * @throws IllegalArgumentException
		 *             if the store size does not match the size of the source
		 */

		boolean store(BitStore store);

		/**
		 * Tests the bits of a long against the source {@link BitStore}. In
		 * cases where the size of the source is less than the full 64 bits of
		 * the long, the least-significant bits are used.
		 *
		 * @param bits
		 *            the bits tested against
		 * @return whether the test succeeds
		 * @throws IllegalArgumentException
		 *             if the size of the source exceeds 64 bits.
		 */

		boolean bits(long bits);

	}

	/**
	 * Permutes the bits of a {@link BitStore}.
	 */

	interface Permutes extends Transposable {

		/**
		 * <p>
		 * Rotates the bits in a {@link BitStore}. The distance is added to the
		 * index of a bit to give its new index. Bits whose indices would lie
		 * outside the range of the {@link BitStore} are mapped on to lower
		 * values modulo the size of the store.
		 *
		 * <p>
		 * Informally, positive distances correspond to a left rotation, and
		 * negative distances correspond to right rotation. Rotation through any
		 * distance which is a multiple of the store's size (including zero,
		 * naturally) leaves the bits of the {@link BitStore} unchanged.
		 *
		 * @param distance
		 *            the distance in bits through which the bit values are
		 *            rotated
		 */

		void rotate(int distance);

		/**
		 * Reverses the bits in the {@link BitStore}.
		 */

		void reverse();

		/**
		 * <p>
		 * Randomly shuffles the bits in the store.
		 *
		 * <p>
		 * <b>Note</b>: callers cannot assume that calling this method with the
		 * same randomization will always yield the same permutation. For
		 * greater control over permutations (of all natures) see
		 * <code>com.tomgibara.permute.Permute</code>
		 *
		 * @param random
		 *            a source of randomness
		 */

		void shuffle(Random random);
	}

	// fundamental methods

	/**
	 * <p>
	 * The size of the {@link BitStore} in bits.
	 *
	 * <p>This is a <b>fundamental method</b>.
	 *
	 * @return the size of the store, possibly zero, never negative
	 */

	int size();

	/**
	 * <p>
	 * Gets the value of a single bit in the {@link BitStore}.
	 *
	 * <p>This is a <b>fundamental method</b>.
	 *
	 * @param index
	 *            the index of the bit value to be returned
	 * @return whether the bit at the specified index is a one
	 */

	boolean getBit(int index);

	// fundamental mutation methods

	/**
	 * <p>
	 * Sets the value of a bit in the {@link BitStore}.
	 *
	 * <p>
	 * This is a <b>fundamental mutation method</b>.
	 *
	 * @param index
	 *            the index of the bit to be set
	 * @param value
	 *            the value to be assigned to the bit
	 */

	default void setBit(int index, boolean value) {
		throw new IllegalStateException("immutable");
	}

	// accelerating methods

	/**
	 * <p>
	 * Returns up to 64 bits of the {@link BitStore} starting from a specified
	 * position, packed in a long. The position specifies the index of the least
	 * significant bit.
	 * 
	 * <p>
	 * The bits will be returned in the least significant places. Any unused
	 * bits in the long will be zero.
	 *
	 * <p>
	 * This is an <b>acceleration method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @param length
	 *            the number of bits to be returned, from 0 to 64 inclusive
	 * @return a long containing the specified bits
	 * @see #getBitsAsInt(int, int)
	 * @see #getByte(int)
	 * @see #getShort(int)
	 * @see #getInt(int)
	 * @see #getLong(int)
	 */

	default long getBits(int position, int length) {
		Bits.checkBitsLength(length);
		long bits = 0L;
		for (int i = position + length - 1; i >= position; i--) {
			bits <<= 1;
			if (getBit(i)) bits |= 1L;
		}
		return bits;
	}

	/**
	 * <p>
	 * Returns up to 32 bits of the {@link BitStore} starting from a specified
	 * position, packed in an int. The position specifies the index of the least
	 * significant bit.
	 *
	 * <p>
	 * The bits will be returned in the least significant places. Any unused
	 * bits in the long will be zero.
	 *
	 * <p>
	 * This is an <b>acceleration method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @param length
	 *            the number of bits to be returned, from 0 to 32 inclusive
	 * @return an int containing the specified bits
	 * @see #getBits(int, int)
	 * @see #getByte(int)
	 * @see #getShort(int)
	 * @see #getInt(int)
	 * @see #getLong(int)
	 */

	default int getBitsAsInt(int position, int length) {
		Bits.checkIntBitsLength(length);
		int bits = 0;
		for (int i = position + length - 1; i >= position; i--) {
			bits <<= 1;
			if (getBit(i)) bits |= 1L;
		}
		return bits;
	}

	// accelerating mutation methods

	/**
	 * <p>
	 * Flips the bit at a specified index. If the bit has a value of
	 * <code>1</code> prior to the call, its value is set to <code>o</code>. If
	 * the bit has a value of <code>0</code> prior to the call, its value is set
	 * to <code>1</code>.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @param index
	 *            the bit to be flipped
	 */

	default void flipBit(int index) {
		setBit(index, !getBit(index));
	}

	/**
	 * <p>
	 * Sets the value of a bit and returns its value prior to any modification.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @param index
	 *            the index of the bit to be modified
	 * @param value
	 *            the value to be assigned to the bit
	 * @return the value of the bit before assignment
	 */

	default boolean getThenSetBit(int index, boolean value) {
		boolean previous = getBit(index);
		if (previous != value) setBit(index, value);
		return previous;
	}

	/**
	 * <p>
	 * Sets up to 64 bits of the {@link BitStore}, starting at a specified
	 * position, with bits packed in a long. The position specifies the index of
	 * the least significant bit.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @param position
	 *            the index of the least bit assigned to
	 * @param value
	 *            the values to be assigned to the bits
	 * @param length
	 *            the number of bits to be modified
	 * @see #setBitsAsInt(int, int, int)
	 */

	default void setBits(int position, long value, int length) {
		Bits.checkBitsLength(length);
		int to = position + length;
		if (to > size()) throw new IllegalArgumentException("length too great");
		for (int i = position; i < to; i++, value >>= 1) {
			setBit(i, (value & 1) != 0);
		}
	}

	/**
	 * <p>
	 * Sets up to 32 bits of the {@link BitStore}, starting at a specified
	 * position, with bits packed in an int. The position specifies the index of
	 * the least significant bit.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @param position
	 *            the index of the least bit assigned to
	 * @param value
	 *            the values to be assigned to the bits
	 * @param length
	 *            the number of bits to be modified
	 * @see #setBits(int, long, int)
	 */

	default void setBitsAsInt(int position, int value, int length) {
		Bits.checkIntBitsLength(length);
		int to = position + length;
		if (to > size()) throw new IllegalArgumentException("length too great");
		for (int i = position; i < to; i++, value >>= 1) {
			setBit(i, (value & 1) != 0);
		}
	}

	/**
	 * <p>
	 * Sets a range of bits in this {@link BitStore} with the bits in another.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @param position
	 *            the position to which the bits will be copied
	 * @param store
	 *            the source of the bits to be copied
	 */

	default void setStore(int position, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int to = position + store.size();
		if (to > size()) throw new IllegalArgumentException("store size too great");
		for (int i = position; i < to; i++) {
			setBit(i, store.getBit(i - position));
		}
	}

	/**
	 * <p>
	 * Sets every bit in the {@link BitStore}, assigning each bit a value of
	 * <code>1</code>.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 */

	default void fill() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, true);
		}
	}

	/**
	 * <p>
	 * Clears every bit in the {@link BitStore}, assigning each bit a value of
	 * <code>0</code>.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 */

	default void clear() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, false);
		}
	}

	/**
	 * <p>
	 * Flips every bit in the {@link BitStore}. Every <code>1</code> bit is
	 * assigned a value of <code>0</code> and every <code>0</code> bit is
	 * assigned a value of <code>1</code>.
	 *
	 * <p>
	 * This is an <b>accelerating mutation method</b>.
	 *
	 * @see #flipBit(int)
	 */

	default void flip() {
		int size = size();
		for (int i = 0; i < size; i++) {
			flipBit(i);
		}
	}

	// operations

	/**
	 * <p>
	 * An {@link Op} that uses {@link Operation#SET} to 'set' bits on the
	 * {@link BitStore}. This is equivalent to <code>op(Operation.SET)</code>.
	 *
	 * <p>
	 * This is an <b>operations method</b>.
	 *
	 * @return an object for <code>set</code>ting bit values on the
	 *         {@link BitStore}
	 * @see #op(Operation)
	 */

	default Op set() {
		return new BitStoreOp.Set(this);
	}

	/**
	 * An {@link Op} that uses {@link Operation#AND} to 'and' bits on the
	 * {@link BitStore}. This is equivalent to <code>op(Operation.AND)</code>.
	 *
	 * <p>
	 * This is an <b>operations method</b>.
	 *
	 * @return an object for <code>and</code>ing bit values on the
	 *         {@link BitStore}
	 * @see #op(Operation)
	 */

	default Op and() {
		return new BitStoreOp.And(this);
	}

	/**
	 * <p>
	 * An {@link Op} that uses {@link Operation#OR} to 'or' bits on the
	 * {@link BitStore}. This is equivalent to <code>op(Operation.OR)</code>.
	 *
	 * <p>
	 * This is an <b>operations method</b>.
	 *
	 * @return an object for <code>or</code>ing bit values on the
	 *         {@link BitStore}
	 * @see #op(Operation)
	 */

	default Op or() {
		return new BitStoreOp.Or(this);
	}

	/**
	 * <p>
	 * An {@link Op} that uses {@link Operation#OR} to 'xor' bits on the
	 * {@link BitStore}. This is equivalent to <code>op(Operation.XOR)</code>.
	 *
	 * <p>
	 * This is an <b>operations method</b>.
	 *
	 * @return an object for <code>xor</code>ing bit values on the
	 *         {@link BitStore}
	 * @see #op(Operation)
	 */

	default Op xor() {
		return new BitStoreOp.Xor(this);
	}

	// shifting

	/**
	 * <p>
	 * Translates the bits of the {@link BitStore} a fixed distance left or
	 * right. Vacated bits are filled with the specified value. A positive
	 * distance corresponds to moving bits left (from lower indices to higher
	 * indices). A negative distance corresponds to moving bits right (from
	 * higher indices to lower indices). Calling the method with a distance of
	 * zero has no effect.
	 *
	 * <p>
	 * This is an <b>shifting method</b>.
	 *
	 * @param distance
	 *            the number of indices through which the bits should be
	 *            translated.
	 * @param fill
	 *            the value that should be assigned to the bits at indices
	 *            unpopulated by the shift
	 */

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
			range(0, j + 1).setAll(fill);
		} else {
			int j = 0;
			for (int i = j - distance; i < size; i++, j++) {
				setBit(j, getBit(i));
			}
			range(j, size).setAll(fill);
		}
	}

	// matching

	/**
	 * <p>
	 * Returns an object can match the supplied bit sequence against the
	 * {@link BitStore}.
	 *
	 * <p>
	 * This is a <b>matches method</b>.
	 *
	 * @param sequence
	 *            the bit sequence to be matched
	 * @return the matches of the specified sequence
	 * @see OverlappingMatches#disjoint()
	 */

	default OverlappingMatches match(BitStore sequence) {
		if (sequence == null) throw new IllegalArgumentException("null sequence");
		if (sequence.size() == 1) return match(sequence.getBit(0));
		return new BitStoreOverlappingMatches(this, sequence);
	}

	/**
	 * <p>
	 * Returns an object that identifies the positions of each <code>1</code>
	 * bit in the {@link BitStore}.
	 *
	 * <p>
	 * This is a <b>matches method</b>.
	 *
	 * @return the locations of all <code>1</code> bits
	 * @see #match(boolean)
	 */

	default BitMatches ones() {
		return new BitStoreBitMatches.Ones(this);
	}

	/**
	 * <p>
	 * Returns an object that identifies the positions of each <code>0</code>
	 * bit in the {@link BitStore}.
	 *
	 * <p>
	 * This is a <b>matches method</b>.
	 *
	 * @return the locations of all <code>0</code> bits
	 * @see #match(boolean)
	 */

	default BitMatches zeros() {
		return new BitStoreBitMatches.Zeros(this);
	}

	// testing

	/**
	 * <p>
	 * Tests for equality.
	 *
	 * <p>
	 * This is a <b>tests method</b>.
	 *
	 * @return tests for equality
	 *
	 * @see Test#EQUALS
	 * @see #test(Test)
	 */

	default Tests equals() {
		return new BitStoreTests.Equals(this);
	}

	/**
	 * <p>
	 * Tests for exclusion.
	 *
	 * <p>
	 * This is a <b>tests method</b>.
	 *
	 * @return tests for exclusion
	 *
	 * @see Test#EXCLUDES
	 * @see #test(Test)
	 */

	default Tests excludes() {
		return new BitStoreTests.Excludes(this);
	}

	/**
	 * <p>
	 * Tests for containment.
	 *
	 * <p>
	 * This is a <b>tests method</b>.
	 *
	 * @return tests for containment
	 *
	 * @see Test#CONTAINS
	 * @see #test(Test)
	 */

	default Tests contains() {
		return new BitStoreTests.Contains(this);
	}

	/**
	 * <p>
	 * Tests for complement.
	 *
	 * <p>
	 * This is a <b>tests method</b>.
	 *
	 * @return tests for complement
	 *
	 * @see Test#COMPLEMENTS
	 * @see #test(Test)
	 */

	default Tests complements() {
		return new BitStoreTests.Complements(this);
	}

	// I/O

	/**
	 * <p>
	 * Opens a {@link BitWriter} that writes bits into the {@link BitStore}. The
	 * bit writer writes 'backwards' from the initial position, with the first
	 * bit written having an index of <code>initialPos - 1</code>. The last bit
	 * is written to <code>finalPos</code>. In other words, the writer writes to
	 * the range <code>[finalPos, initialPos)</code> in big-endian order. If the
	 * positions are equal, no bits are written.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param finalPos
	 *            the index at which the writer terminates, equ. the start of
	 *            the range to which bits are written
	 * @param initialPos
	 *            the position beyond the index at which the writer begins, equ.
	 *            the end of the range to which bits are written
	 * @return a writer over the range
	 * @see Op#openWriter(int, int)
	 */

	default BitWriter openWriter(int finalPos, int initialPos) {
		return Bits.newBitWriter(this, finalPos, initialPos);
	}

	/**
	 * <p>
	 * Opens a {@link BitReader} that reads bits from the {@link BitStore}. The
	 * bit reader reads 'backwards' from the initial position, with the first
	 * bit read having an index of <code>initialPos - 1</code>. The last bit is
	 * read from <code>finalPos</code>. In other words, the reader reads from
	 * the range <code>[finalPos, initialPos)</code> in big-endian order. If the
	 * positions are equal, no bits are read.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param finalPos
	 *            the index at which the reader terminates, equ. the start of
	 *            the range from which bits are read
	 * @param initialPos
	 *            the position beyond the index at which the reader begins, equ.
	 *            the end of the range from which bits are read
	 * @return a reader over the range
	 */

	default BitReader openReader(int finalPos, int initialPos) {
		return Bits.newBitReader(this, finalPos, initialPos);
	}

	/**
	 * <p>
	 * Writes the bits in the {@link BitStore} to the supplied writer, in
	 * big-endian order. The bit at <code>size() - 1</code> is the first bit
	 * written. The bit at <code>0</code> is the last bit written.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param writer
	 *            the writer to which the bits should be written
	 * @return the number of bits written
	 */

	default int writeTo(BitWriter writer) {
		int size = size();
		Bits.transfer(openReader(), writer, size);
		return size;
	}

	/**
	 * <p>
	 * Reads bits from the supplied reader into the {@link BitStore}, in
	 * big-endian order. The bit at <code>size() - 1</code> is the first bit
	 * written to. The bit at <code>0</code> is the last bit written to.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param reader
	 *            the reader from which the bits are read
	 */

	default void readFrom(BitReader reader) {
		Bits.transfer(reader, openWriter(), size());
	}

	/**
	 * <p>
	 * Writes the bits in the {@link BitStore} as bytes to the supplied writer,
	 * in big-endian order. If the size of the bit store is not a multiple of 8
	 * (ie. does not span a whole number of bytes), then the first byte written
	 * is padded with zeros in its most significant bits.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param writer
	 *            the writer to which the bits will be written
	 */

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

	/**
	 * <p>
	 * Populates the {@link BitStore} with bytes read from the supplied reader.
	 * If the size of the bit store is not a multiple of 8 (ie. does not span a
	 * whole number of bytes), then a number of the most-significant bits of the
	 * first byte are skipped so that exactly {@link #size()} bits are written
	 * to the {@link BitStore}.
	 *
	 * <p>
	 * This is an <b>I/O method</b>.
	 *
	 * @param reader
	 *            the reader from which the bits are read
	 */

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

	/**
	 * <p>
	 * A sub-range of this {@link BitStore}. The returned store is a view over
	 * this store, changes in either are reflected in the other. If this store
	 * is immutable, so too is the returned store.
	 *
	 * <p>
	 * The returned store as a size of <code>to - from</code> and
	 * has its own indexing, starting at zero (which maps to index
	 * <code>from</code> in this store).
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @param from
	 *            the start of the range (inclusive)
	 * @param to
	 *            the end of the range (exclusive)
	 * @return a sub range of the {@link BitStore}
	 * @see #rangeFrom(int)
	 * @see #rangeTo(int)
	 */

	default BitStore range(int from, int to) {
		return Bits.newRangedView(this, from, to);
	}

	/**
	 * <p>
	 * A view of this {@link BitStore} in which each bit is flipped. The
	 * returned store is a view over this store, changes in either are reflected
	 * in the other. If this store is immutable, so too is the returned store.
	 *
	 * <p>
	 * The returned store has the same size as this store.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return a store in which each bit value is flipped from its value in this
	 *         store
	 * @see #flip()
	 */

	default BitStore flipped() {
		return new FlippedBitStore(this);
	}

	/**
	 * <p>
	 * A view of this {@link BitStore} in which the bit indices are reversed.
	 * The returned store is a view over this store, changes in either are
	 * reflected in the other. If this store is immutable, so too is the
	 * returned store.
	 *
	 * <p>
	 * The returned store has the same size as this store with the bit at index
	 * <code>i</code> drawing its value from the bit at index
	 * <code>size - i - 1</code> from this store.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return a store in which each bit is reversed
	 * @see #flip()
	 */

	default BitStore reversed() {
		return new ReversedBitStore(this);
	}

	/**
	 * <p>
	 * A permutable view of this {@link BitStore} that allows the bits of this
	 * store to be permuted.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return a permutable view
	 */

	default Permutes permute() {
		return new BitStorePermutes(this);
	}

	/**
	 * <p>
	 * Copies the {@link BitStore} to a byte array. The most significant bits
	 * of the {@link BitStore} are written to the first byte in the array that
	 * is padded with zeros for each unused bit.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return the bits of the bit store in a byte array
	 */

	default byte[] toByteArray() {
		StreamBytes bytes = Streams.bytes((size() + 7) >> 3);
		writeTo(bytes.writeStream());
		return bytes.directBytes();
	}

	/**
	 * <p>
	 * Copies the {@link BitStore} into a <code>BigInteger</code>. The returned
	 * <code>BigInteger</code> is the least positive integer such that
	 * <code>bigint.testBit(i) == store.getBit(i)</code>.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return the value of the {@link BitStore} as a big integer
	 * @see Bits#asStore(BigInteger)
	 */

	default BigInteger toBigInteger() {
		return size() == 0 ? BigInteger.ZERO : new BigInteger(1, toByteArray());
	}

	/**
	 * <p>
	 * Returns the string representation of the {@link BitStore} in the given
	 * radix. The radix must be in the range <code>Character.MIN_RADIX</code>
	 * and <code>Character.MAX_RADIX</code> inclusive. The value is always
	 * positive.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @param radix
	 *            a valid radix
	 * @return the {@link BitStore} as a string in the specified radix
	 * @throws IllegalArgumentException
	 *             if the radix is invalid
	 */

	default String toString(int radix) {
		if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) throw new IllegalArgumentException("invalid radix");
		return toBigInteger().toString(radix);
	}

	/**
	 * <p>
	 * Copies the {@link BitStore} into a <code>BitSet</code>.
	 *
	 * <p>
	 * This is an <b>view method</b>.
	 *
	 * @return the {@link BitStore} as a <code>BitSet</code>
	 */

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

	/**
	 * <p>
	 * A view of this {@link BitStore} as a <code>Number</code>. The returned
	 * number is a view over this store so changes in the {@link BitStore}
	 * are reflected in the numeric view.
	 *
	 * <p>
	 * This is a <b>view method</b>.
	 *
	 * @return the {@link BitStore} as a <code>Number</code>.
	 */

	default Number asNumber() {
		return Bits.asNumber(this);
	}

	/**
	 * <p>
	 * A view of this {@link BitStore} as a <code>List</code> of boolean values.
	 * The returned list is a view over this store, changes in either are
	 * reflected in the other. If this store is immutable, the returned list is
	 * unmodifiable.
	 *
	 * <p>
	 * The size of the returned list matches the size of the {@link BitStore}.
	 * The list supports mutation if the the store is mutable, but does not
	 * support operations which would modify the length of the list (and
	 * therefore the store) such as <code>add</code> and <code>remove</code>.
	 *
	 * <p>
	 * This is an <b>view method</b>.
	 *
	 * @return the {@link BitStore} as a list
	 */

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

	/**
	 * <p>
	 * Compares the {@link BitStore} numerically to another {@link BitStore}. A
	 * numerical comparison treats the stores as positive binary integers and
	 * compares them using the natural ordering of the integers.
	 *
	 * <p>
	 * This is a <b>comparable method</b>.
	 *
	 * @param that
	 *            the store being compared against.
	 * @return zero if the stores are equal, a negative value if this store is
	 *         less than the given store, a positive value otherwise.
	 * @see #compareTo(BitStore)
	 * @see Bits#compareNumeric(BitStore, BitStore)
	 * @see #compareLexicallyTo(BitStore)
	 */

	default int compareNumericallyTo(BitStore that) {
		if (this == that) return 0; // cheap check
		if (that == null) throw new IllegalArgumentException("that");
		return Bits.compareNumeric(this, that);
	}

	/**
	 * <p>
	 * Compares the {@link BitStore} lexically to another {@link BitStore}. A
	 * lexical comparison treats the stores as binary strings and compares them
	 * using the conventional ordering of Java strings.
	 *
	 * <p>
	 * This is a <b>comparable method</b>.
	 *
	 * @param that
	 *            the store being compared against.
	 * @return zero if the stores are equal, a negative value if this store
	 *         precedes the given store in lexical order, a positive value
	 *         otherwise.
	 * @see Bits#compareLexical(BitStore, BitStore)
	 * @see #compareNumericallyTo(BitStore)
	 */

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

	/**
	 * <p>
	 * Sets all bits in the bit store to the specified value.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param value
	 *            the bit value to assign to all bits
	 * @see #fill()
	 * @see #clear()
	 */

	default void setAll(boolean value) {
		if (value) {
			fill();
		} else {
			clear();
		}
	}

	/**
	 * <p>
	 * Returns an {@link Op} that applies the specified {@link Operation} to the
	 * bits of this {@link BitStore}.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param operation
	 *            the operation to be applied to the bits of this store.
	 * @return an object that can applies the operation to this bit store.
	 * @see #set()
	 * @see #and()
	 * @see #or()
	 * @see #xor()
	 */

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

	/**
	 * <p>
	 * Returns an object that identifies each position with the specified bits
	 * value.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param bit
	 *            the bit value to match
	 * @return the locations of the matching bits
	 * @see #ones()
	 * @see #zeros()
	 */

	default BitMatches match(boolean bit) {
		return bit ? ones() : zeros();
	}

	/**
	 * <p>
	 * Returns tests of a specified nature.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param test
	 *            the test to be applied
	 * @return an object that applies the specified test
	 * @see #equals()
	 * @see #contains()
	 * @see #excludes()
	 * @see #complements()
	 */

	default Tests test(Test test) {
		if (test == null) throw new IllegalArgumentException("null test");
        return switch (test) {
            case EQUALS -> equals();
            case CONTAINS -> contains();
            case EXCLUDES -> excludes();
            case COMPLEMENTS -> complements();
            default -> throw new IllegalStateException("Unexpected test");
        };
	}

	/**
	 * <p>
	 * Opens a {@link BitWriter} that writes bits into the {@link BitStore}. The
	 * bit writer writes 'backwards' (in big-endian order) from the
	 * highest-indexed (most-significant) bit to the zero-indexed
	 * (least-significant) bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @return a writer over the bit store
	 * @see #openWriter(int, int)
	 */

	default BitWriter openWriter() {
		return openWriter(0, size());
	}

	/**
	 * <p>
	 * Opens a {@link BitReader} that reads bits from the {@link BitStore}. The
	 * bit reader reads 'backwards' (in big-endian order) from the
	 * highest-indexed (most-significant) bit to the zero-indexed
	 * (least-significant) bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @return a reader over the bit store
	 * @see #openReader(int, int)
	 */

	default BitReader openReader() {
		return openReader(0, size());
	}

	/**
	 * <p>
	 * A sub-range from a given position to the end of the {@link BitStore}.
	 * Equivalent to <code>range(from, size)</code> where <code>size</code> is
	 * the size of this store.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param from
	 *            the start of the range (inclusive)
	 * @return a sub range of the {@link BitStore}
	 * @see #range(int, int)
	 */

	default BitStore rangeFrom(int from) {
		return range(from, size());
	}

	/**
	 * <p>
	 * A sub-range the start of the {@link BitStore} to a given position.
	 * Equivalent to <code>range(0, to)</code>.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param to
	 *            the end of the range (exclusive)
	 * @return a sub range of the {@link BitStore}
	 * @see #range(int, int)
	 */

	default BitStore rangeTo(int to) {
		return range(0, to);
	}

	/**
	 * <p>
	 * Compares this {@link BitStore} to another. The comparison is numerical
	 * and equivalent to that performed by
	 * {@link #compareNumericallyTo(BitStore)}.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param that
	 *            the comparand
	 * @return zero if the stores are equal, a negative value if this store is
	 *         less than the given store, a positive value otherwise.
	 * @throws NullPointerException
	 *             if the supplied bit store is null as per the contract for
	 *             <code>Comparable</code>
	 * @see #compareNumericallyTo(BitStore)
	 */

	default int compareTo(BitStore that) {
		if (that == null) throw new NullPointerException(); // as per compareTo() contract
		return compareNumericallyTo(that);
	}

	/**
	 * <p>
	 * Returns 8 bits of the {@link BitStore} starting from a specified
	 * position, packed into a <code>byte</code>. The position specifies the
	 * index of the least significant bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @return a byte containing 8 bits of the {@link BitStore}
	 * @see #getBits(int, int)
	 */

	default byte getByte(int position) {
		return (byte) getBits(position, 8);
	}

	/**
	 * <p>
	 * Returns 16 bits of the {@link BitStore} starting from a specified
	 * position, packed into a <code>short</code>. The position specifies the
	 * index of the least significant bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @return a short containing 16 bits of the {@link BitStore}
	 * @see #getBits(int, int)
	 */

	default short getShort(int position) {
		return (short) getBits(position, 16);
	}

	/**
	 * <p>
	 * Returns 32 bits of the {@link BitStore} starting from a specified
	 * position, packed into an <code>int</code>. The position specifies the
	 * index of the least significant bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @return an int containing 32 bits of the {@link BitStore}
	 * @see #getBits(int, int)
	 */

	default int getInt(int position) {
		return (int) getBits(position, 32);
	}

	/**
	 * <p>
	 * Returns 64 bits of the {@link BitStore} starting from a specified
	 * position, packed into a <code>long</code>. The position specifies the
	 * index of the least significant bit.
	 *
	 * <p>
	 * This is a <b>convenience method</b>.
	 *
	 * @param position
	 *            the index of the least bit returned
	 * @return a long containing 64 bits of the {@link BitStore}
	 * @see #getBits(int, int)
	 */

	default long getLong(int position) {
		return getBits(position, 64);
	}

}

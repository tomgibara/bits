/*
 * Copyright 2010 Tom Gibara
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.PrivilegedAction;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedSet;

/**
 * <p>
 * Stores fixed-length bit sequences of arbitrary length and provides a number
 * of bit-wise operations, and methods for exposing the bit sequences as
 * established java types.
 * </p>
 *
 * <p>
 * In keeping with Java standards, bits are operated-on and exposed as
 * <em>big-endian</em>. This means that, where bit sequences are input/output
 * from methods, the least-significant bit is always on the right and the most
 * significant bit is on the left. So, for example, the {@link #toString()}
 * method will contain the most significant bit in the character at index 0 in
 * the string. Naturally, In the cases where this class is used without
 * externalizing the bit representation, this distinction is irrelevant.
 * </p>
 *
 * <p>
 * A consequence of this is that, in methods that are defined over ranges of
 * bits, the <em>from</em> and <em>to</em> parameters define the rightmost and
 * leftmost indices respectively. As per Java conventions, all <em>from</em>
 * parameters are inclusive and all <em>to</em> parameters are exclusive.
 * </p>
 *
 * <p>
 * Instances of this class may be aligned (see {@link #isAligned()} and
 * {@link #aligned()}. Many operations will execute more efficiently on aligned
 * instances. Instances may also be immutable (see {@link #isMutable()},
 * {@link #mutable()} and {@link #immutable()}). In addition to a number of
 * copying operations, the class also supports views; views create new
 * {@link BitVector} instances that are backed by the same bit data. Amongst
 * other things this allows applications to expose mutable {@link BitVector}
 * instances 'safely' via immutable views.
 * </p>
 *
 * <p>
 * The class extends {@link Number} which allows it to be treated as an extended
 * length numeric type (albeit, one that doesn't support any arithmetic
 * operations). In addition to the regular number value methods on the
 * interface, a {@link #toBigInteger()} method is available that returns the
 * BitVector as a positive, arbitrarily sized integer. Combined with the
 * fromBigInteger() method, this allows, with some loss of performance, a range
 * of arithmetic calculations to be performed.
 * </p>
 *
 * <p>
 * The class also implements {@link Iterable} and provides methods to obtain
 * {@link ListIterator} as one would for a {@link List}. This allows a
 * {@link BitVector} to be directly used with a range of Java language
 * constructs and standard library classes. Though the class stops short of
 * implementing the {@link List} interface, the {@link #asList()} method
 * provides this.
 * </p>
 *
 * <p>
 * In addition, the the {@link #positionIterator()} method exposes a
 * {@link ListIterator} that ranges over the indices of the bits that are set
 * within the {@link BitVector}. The class can also expose the bits as a
 * {@link SortedSet} of these indices via the {@link #asSet()} method.
 * </p>
 *
 * <p>
 * All iterators and collections are mutable if the underlying {@link BitVector}
 * is, though naturally, any operations that would modify the size cannot be
 * supported.
 * </p>
 *
 * <p>
 * The class is {@link Serializable} and {@link Cloneable} too (clones are
 * shallow and essentially behave as a view of the original instance). For
 * instances where more control over serialization is needed,
 * {@link #readFrom(InputStream)} and {@link #write(OutputStream)} methods are
 * available, though better performance may result from calling
 * {@link #toByteArray()} and managing the writing outside this class.
 * </p>
 *
 * <p>
 * In addition to all of the above methods outlined above, a full raft of
 * bitwise operations are available, including:
 * </p>
 *
 * <ul>
 * <li>set, and, or, xor operations over a variety of inputs</li>
 * <li>shifts, rotations and reversals</li>
 * <li>tests for bit-wise intersection, containment and equality</li>
 * <li>tests for all-zero and all-one ranges</li>
 * <li>counting the number ones/zeros in a range</li>
 * <li>searches for first/last ones/zeros in a given range</li>
 * <li>searches for next/previous ones/zeros from a given index</li>
 * <li>copying and viewing bit ranges</li>
 * <li>obtaining bits as Java primitive types</li>
 * </ul>
 *
 * <p>
 * Most such methods are available in both an operation specific version and
 * operation parameterized version to cater for different application needs.
 * </p>
 *
 * <p>
 * Performance should be adequate for most uses. There is scope to improve the
 * performance of many methods, but none of the methods operate in anything more
 * than linear time and inner loops are mostly 'tight'. An implementation detail
 * (which may be important on some platforms) is that, with few exceptions, none
 * of the methods perform any object creation. The exceptions are: methods that
 * require an object to be returned, {@link #floatValue()},
 * {@link #doubleValue()}, {@link #toString(int)}, and situations where an
 * operation is applied with overlapping ranges of bits, in which case it may be
 * necessary to create a temporary copy of the {@link BitVector}.
 * </p>
 *
 * <p>
 * The class is marked as final to ensure that immutable instances can be safely
 * used in security sensitive code (eg. within a {@link PrivilegedAction}).
 * </p>
 *
 * @author Tom Gibara
 *
 */

public final class BitVector extends Number implements BitStore, Cloneable, Iterable<Boolean>, Comparable<BitVector> {

	// statics

	private static final int ADDRESS_BITS = 6;
	private static final int ADDRESS_SIZE = 1 << ADDRESS_BITS;
	private static final int ADDRESS_MASK = ADDRESS_SIZE - 1;

	private static final int SET = 0;
	private static final int AND = 1;
	private static final int OR  = 2;
	private static final int XOR = 3;

	private static final int EQUALS = 0;
	private static final int INTERSECTS = 1;
	private static final int CONTAINS = 2;
	private static final int COMPLEMENTS = 3;

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
	 * A test that can be made of one {@link BitVector} against another.
	 */

	public enum Test {

		/**
		 * Whether two {@link BitVector} have the same pattern of true/false-bits.
		 */

		EQUALS,

		/**
		 * Whether there exists a position at which both {@link BitVector}s have
		 * a true-bit.
		 */

		INTERSECTS,

		/**
		 * Whether one {@link BitVector} has true-bits at every position that
		 * another does.
		 */

		CONTAINS,

		/**
		 * Whether one {@link BitVector} is has zero bits at exactly every
		 * position that another has one bits.
		 */

		COMPLEMENTS
	}

	public static final BitVector fromBigInteger(BigInteger bigInt) {
		if (bigInt == null) throw new IllegalArgumentException();
		if (bigInt.signum() < 0) throw new IllegalArgumentException();
		final int length = bigInt.bitLength();
		return fromBigIntegerImpl(bigInt, length, length);
	}

	public static BitVector fromBigInteger(BigInteger bigInt, int size) {
		if (bigInt == null) throw new IllegalArgumentException();
		if (bigInt.signum() < 0) throw new IllegalArgumentException();
		if (size < 0) throw new IllegalArgumentException();
		final int length = Math.min(size, bigInt.bitLength());
		return fromBigIntegerImpl(bigInt, size, length);

	}

	public static BitVector fromByteArray(byte[] bytes, int size) {
		//TODO provide a more efficient implementation
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (size < 0) throw new IllegalArgumentException("negative size");
		BigInteger bigInt = new BigInteger(1, bytes);
		final int length = Math.min(size, bigInt.bitLength());
		return fromBigIntegerImpl(bigInt, size, length);
	}

	private static BitVector fromBigIntegerImpl(BigInteger bigInt, int size, int length) {
		final BitVector vector = new BitVector(size);
		final long[] bits = vector.bits;
		long v = 0L;
		int i = 0;
		for (; i < length; i++) {
			if (bigInt.testBit(i)) {
				v = (v >>> 1) | Long.MIN_VALUE;
			} else {
				v >>>= 1;
			}
			if ((i & ADDRESS_MASK) == ADDRESS_MASK) {
				bits[i >> ADDRESS_BITS] = v;
				v = 0;
			}
		}
		if (v != 0L) bits[i >> ADDRESS_BITS] = v >>> (ADDRESS_SIZE - (i & ADDRESS_MASK));
		return vector;
	}

	public static BitVector fromBitSet(BitSet bitSet) {
		if (bitSet == null) throw new IllegalArgumentException();
		final int length = bitSet.length();
		return fromBitSetImpl(bitSet, length, length);
	}

	public static BitVector fromBitSet(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException();
		if (size < 0) throw new IllegalArgumentException();
		final int length = Math.min(size, bitSet.length());
		return fromBitSetImpl(bitSet, size, length);
	}

	private static BitVector fromBitSetImpl(BitSet bitSet, int size, int length) {
		final BitVector vector = new BitVector(size);
		final long[] bits = vector.bits;
		long v = 0L;
		int i = 0;
		for (; i < length; i++) {
			if (bitSet.get(i)) {
				v = (v >>> 1) | Long.MIN_VALUE;
			} else {
				v >>>= 1;
			}
			if ((i & ADDRESS_MASK) == ADDRESS_MASK) {
				bits[i >> ADDRESS_BITS] = v;
				v = 0;
			}
		}
		if (v != 0L) bits[i >> ADDRESS_BITS] = v >>> (ADDRESS_SIZE - (i & ADDRESS_MASK));
		return vector;
	}

	public static BitVector fromStore(BitStore store) {
		if (store instanceof BitVector) {
			return ((BitVector)store).mutableCopy();
		} else {
			if (store == null) throw new IllegalArgumentException("null store");
			return new BitVector(store);
		}
	}

	// used by LongBits

	static BitVector fromLong(long bits) {
		return new BitVector(0, 64, new long[] {bits}, false);
	}

	public static final Comparator<BitVector> sNumericComparator = new Comparator<BitVector>() {

		@Override
		public int compare(BitVector a, BitVector b) {
			if (a == null) throw new IllegalArgumentException("null a");
			if (b == null) throw new IllegalArgumentException("null b");
			if (a == b) return 0;
			return a.size() < b.size() ? compareNumeric(a, b) : - compareNumeric(b, a);
		}

	};

	public static final Comparator<BitVector> sLexicalComparator = new Comparator<BitVector>() {

		@Override
		public int compare(BitVector a, BitVector b) {
			if (a == null) throw new IllegalArgumentException("null a");
			if (b == null) throw new IllegalArgumentException("null b");
			if (a == b) return 0;
			return a.size() < b.size() ? compareLexical(a, b) : -compareLexical(b, a);
		}

	};

	//a, b not null a size not greater than b size
	private static int compareNumeric(BitVector a, BitVector b) {
		final int aSize = a.size();
		final int bSize = b.size();
		if (aSize != bSize && !b.isAllAdj(b.finish - bSize + aSize, b.finish, false)) return -1;
		// more optimizations are possible but probably not worthwhile
		if (a.isAligned() && b.isAligned()) {
			int pos = aSize & ~ADDRESS_MASK;
			if (pos != aSize) {
				int bits = aSize - pos;
				long aBits = a.getBitsAdj(pos, bits);
				long bBits = b.getBitsAdj(pos, bits);
				if (aBits != bBits) {
					return aBits < bBits ? -1 : 1;
				}
			}
			final long[] aArr = a.bits;
			final long[] bArr = b.bits;
			for (int i = (pos >> ADDRESS_BITS) - 1; i >= 0; i--) {
				long aBits = aArr[i];
				long bBits = bArr[i];
				if (aBits == bBits) continue;
				boolean aNeg = aBits < 0L;
				boolean bNeg = bBits < 0L;
				if (bNeg && !aNeg) return -1;
				if (aNeg && !bNeg) return 1;
				return aBits < bBits ? -1 : 1;
			}
			return 0;
		} else {
			final int aStart = a.start;
			final int bStart = b.start;
			int offset;
			for (offset = aSize - ADDRESS_SIZE; offset >= 0; offset -= ADDRESS_SIZE) {
				long aBits = a.getBitsAdj(offset + aStart, ADDRESS_SIZE);
				long bBits = b.getBitsAdj(offset + bStart, ADDRESS_SIZE);
				if (aBits == bBits) continue;
				boolean aNeg = aBits < 0L;
				boolean bNeg = bBits < 0L;
				if (bNeg && !aNeg) return -1;
				if (aNeg && !bNeg) return 1;
				return aBits < bBits ? -1 : 1;
			}
			if (offset != 0) {
				long aBits = a.getBitsAdj(aStart, ADDRESS_SIZE + offset);
				long bBits = b.getBitsAdj(bStart, ADDRESS_SIZE + offset);
				if (aBits == bBits) return 0;
				return aBits < bBits ? -1 : 1;
			}
			return 0;
		}
	}

	//a, b not null a size not greater than b size
	private static int compareLexical(BitVector a, BitVector b) {
		final int aSize = a.size();
		final int bSize = b.size();
		if (aSize == bSize) return compareNumeric(a, b); // more efficient
		final int size = Math.min(aSize, bSize);
		final int aStart = a.finish - size;
		final int bStart = b.finish - size;
		for (int i = size - 1; i >= 0; i--) {
			boolean aBit = a.getBitAdj(aStart + i);
			boolean bBit = b.getBitAdj(bStart + i);
			if (aBit != bBit) return bBit ? -1 : 1;
		}
		return aSize < bSize ? -1 : 1;
	}

	private static boolean overlapping(int thisFrom, int thisTo, int thatFrom, int thatTo) {
		return thisTo > thatFrom && thisFrom < thatTo;
	}

	//necessary for throwing an IAE
	private static int stringLength(String str) {
		if (str == null) throw new IllegalArgumentException();
		return str.length();
	}

	//duplicated here to avoid dependencies
	private static int gcd(int a, int b) {
		while (a != b) {
			if (a > b) {
				int na = a % b;
				if (na == 0) return b;
				a = na;
			} else {
				int nb = b % a;
				if (nb == 0) return a;
				b = nb;
			}
		}
		return a;
	}

	//used to avoid direct dependence on Java6 methods
	private final static ArrayCopier copier;

	static {
		boolean canCopyRanges;
		try {
			Arrays.class.getMethod("copyOfRange", (new long[0]).getClass(), Integer.TYPE, Integer.TYPE);
			canCopyRanges = true;
		} catch (NoSuchMethodException e) {
			canCopyRanges = false;
		}
		copier = canCopyRanges ? new RangeCopier() : new SystemCopier();
	}

	// fields

	//core fields
	private final int start;
	private final int finish;
	private final long[] bits;
	private final boolean mutable;

	// constructors

	//creates a new bit vector of the specified size
	//naturally aligned
	public BitVector(int size) {
		if (size < 0) throw new IllegalArgumentException();
		if (size > (Integer.MAX_VALUE / 8)) throw new IllegalArgumentException();
		final int length = (size + ADDRESS_MASK) >> ADDRESS_BITS;
		this.bits = new long[length];
		this.start = 0;
		this.finish = size;
		this.mutable = true;
	}

	//TODO consider changing String constructors to static methods
	//creates a new bit vector from the supplied binary string
	//naturally aligned
	public BitVector(String str) {
		this(stringLength(str));
		//TODO can this be usefully optimized?
		for (int i = 0; i < finish; i++) {
			final char c = str.charAt(i);
			if (c == '1') setBit(finish - 1 - i, true);
			else if (c != '0') throw new IllegalArgumentException("Illegal character '" + c + "' at index " + i + ", expected '0' or '1'.");
		}
	}

	public BitVector(String str, int radix) {
		this(new BigInteger(str, radix));
	}

	//TODO unit test
	public BitVector(Random random, float probability, int size) {
		this(size);
		if (random == null) throw new IllegalArgumentException("null random");
		if (probability < 0f) throw new IllegalArgumentException("negative probability");
		if (probability > 1f) throw new IllegalArgumentException("probability exceeds one");
		if (probability == 0f) {
			// nothing to do
		} else if (probability == 1f) {
			for (int i = 0; i < bits.length; i++) {
				bits[i] = -1L;
			}
		} else if (probability == 0.5f) {
			for (int i = 0; i < bits.length; i++) {
				bits[i] = random.nextLong();
			}
		} else {
			for (int i = 0; i < bits.length; i++) {
				long b = 0L;
				for (int j = 0; j < 64; j++) {
					b <<= 1;
					if (random.nextFloat() < probability) b |= 1L;
				}
				bits[i] = b;
			}
		}
	}

	public BitVector(Random random, int size) {
		this(random, 0.5f, size);
	}

	private BitVector(int start, int finish, long[] bits, boolean mutable) {
		this.start = start;
		this.finish = finish;
		this.bits = bits;
		this.mutable = mutable;
	}

	private BitVector(Serial serial) {
		this(serial.start, serial.finish, serial.bits, serial.mutable);
	}

	//only called to support parsing with a different radix
	private BitVector(BigInteger bigInt) {
		this(bigInt.bitLength());
		//TODO ideally trap this earlier
		if (bigInt.signum() < 0) throw new IllegalArgumentException();
		for (int i = 0; i < finish; i++) {
			performSetAdj(i, bigInt.testBit(i));
		}
	}

	private BitVector(BitStore store) {
		this(store.size());
		store.writeTo(new VectorWriter());
	}

	// accessors

	@Override
	public int size() {
		return finish - start;
	}

	public boolean isAligned() {
		return start == 0;
	}

	// duplication

	//TODO consider adding a trimmed copy, or guarantee this is trimmed?
	//only creates a new bit vector if necessary
	public BitVector aligned() {
		return start == 0 ? this : getVectorAdj(start, finish - start, true);
	}

	public BitVector duplicate(boolean copy, boolean mutable) {
		if (mutable && !copy && !this.mutable) throw new IllegalStateException("Cannot obtain mutable view of an immutable BitVector");
		return duplicateAdj(start, finish, copy, mutable);
	}

	public BitVector duplicate(int from, int to, boolean copy, boolean mutable) {
		if (mutable && !copy && !this.mutable) throw new IllegalStateException("Cannot obtain mutable view of an immutable BitVector");
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return duplicateAdj(from, to, copy, mutable);
	}

	public BitVector alignedCopy(boolean mutable) {
		return getVectorAdj(start, finish - start, mutable);
	}

	public BitVector resizedCopy(int newSize) {
		if (newSize < 0) throw new IllegalArgumentException();
		final int size = finish - start;
		if (newSize == size) return copy();
		if (newSize < size) return range(0, newSize).copy();
		final BitVector copy = new BitVector(newSize);
		copy.perform(SET, 0, this);
		return copy;
	}

	// getters

	@Override
	public boolean getBit(int position) {
		if (position < 0) throw new IllegalArgumentException();
		position += start;
		if (position >= finish) throw new IllegalArgumentException();
		//can't assume inlining, so duplicate getBitImpl here
		final int i = position >> ADDRESS_BITS;
		final long m = 1L << (position & ADDRESS_MASK);
		return (bits[i] & m) != 0;
	}

	public byte getByte(int position) {
		return (byte) getBits(position, 8);
	}

	public short getShort(int position) {
		return (byte) getBits(position, 16);
	}

	public int getInt(int position) {
		return (int) getBits(position, 32);
	}

	public long getLong(int position) {
		return (int) getBits(position, 64);
	}

	public long getBits(int position, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (length < 0) throw new IllegalArgumentException();
		position += start;
		if (position + length > finish) throw new IllegalArgumentException();
		if (length == 0) return 0L;
		final int i = position >> ADDRESS_BITS;
		final int s = position & ADDRESS_MASK;
		final long b;
		if (s == 0) { // fast case, long-aligned
			b = bits[i];
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			b = bits[i] >>> s;
		} else {
			b = (bits[i] >>> s) | (bits[i+1] << (ADDRESS_SIZE - s));
		}
		return length == ADDRESS_SIZE ? b : b & ((1L << length) - 1);
	}

	//always mutable & aligned
	public BitVector getVector(int position, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (length < 0) throw new IllegalArgumentException();
		position += start;
		if (position + length > finish) throw new IllegalArgumentException();
		return getVectorAdj(position, length, true);
	}

	// bit matching methods

	public Matches match(boolean bit) {
		return bit ? new MatchesOnes() : new MatchesZeros();
	}

	public Matches ones() {
		return new MatchesOnes();
	}

	public Matches zeros() {
		return new MatchesZeros();
	}

	//NOTE: preserved for performance testing
	int countOnes(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return countOnesAdj(from, to);
	}

	//NOTE: preserved for performance testing
	int countZeros(int from, int to) {
		return to - from - countOnes(from, to);
	}

	// search methods

	//NOTE: preserved for performance testing
	int firstOneInRange(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return firstOneInRangeAdj(from, to) - start;
	}

	//NOTE: preserved for performance testing
	int firstZeroInRange(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return firstZeroInRangeAdj(from, to) - start;
	}

	//NOTE: preserved for performance testing
	int lastOneInRange(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return lastOneInRangeAdj(from, to) - start;
	}

	//NOTE: preserved for performance testing
	int lastZeroInRange(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return lastZeroInRangeAdj(from, to) - start;
	}

	// rotations and shifts & reversals

	public void rotate(int distance) {
		rotateAdj(start, finish, distance);
	}

	public void shift(int distance, boolean fill) {
		shiftAdj(start, finish, distance, fill);
	}

	public void reverse() {
		reverseAdj(start, finish);
	}

	public void shuffle(Random random) {
		if (random == null) throw new IllegalArgumentException("null random");
		shuffleAdj(start, finish, random);
	}

	// comparisons

	@Override
	public int compareTo(BitVector that) {
		if (that == null) throw new IllegalArgumentException("null that");
		if (this == that) return 0; // cheap check
		return this.size() < that.size() ? compareNumeric(this, that) : -compareNumeric(that, this);
	}

	public boolean test(Test test, BitVector vector) {
		if (this.finish - this.start != vector.finish - vector.start) throw new IllegalArgumentException();
		return test(test.ordinal(), vector);
	}

	// tests

	@Override
	public boolean isAll(boolean value) {
		return isAllAdj(start, finish, value);
	}

	// views

	public byte[] toByteArray() {
		//TODO can optimize when byte aligned
		final int size = finish - start;
		final int length = (size + 7) >> 3;
		final byte[] bytes = new byte[length];
		if (length == 0) return bytes;
		if ((start & ADDRESS_MASK) == 0) { //long aligned case
			int i = start >> ADDRESS_BITS;
			int j = length; //how many bytes we have left to process
			for (; j > 8; i++) {
				final long l = bits[i];
				bytes[--j] = (byte) (  l        & 0xff);
				bytes[--j] = (byte) ( (l >>  8) & 0xff);
				bytes[--j] = (byte) ( (l >> 16) & 0xff);
				bytes[--j] = (byte) ( (l >> 24) & 0xff);
				bytes[--j] = (byte) ( (l >> 32) & 0xff);
				bytes[--j] = (byte) ( (l >> 40) & 0xff);
				bytes[--j] = (byte) ( (l >> 48) & 0xff);
				bytes[--j] = (byte) ( (l >> 56) & 0xff);
			}
			if (j > 0) {
				final long m = -1L >>> (ADDRESS_SIZE - finish & ADDRESS_MASK);
				final long l = bits[i] & m;
				for (int k = 0; j > 0; k++) {
					bytes[--j] = (byte) ( (l >> (k*8)) & 0xff);
				}
			}
		} else { //general case
			//TODO indexing could probably be tidied up
			int i = 0;
			for (; i < length - 1; i++) {
				bytes[length - 1 - i] =  (byte) getBits(i << 3, 8);
			}
			bytes[0] = (byte) getBits(i << 3, size - (i << 3));
		}
		return bytes;
	}

	public int[] toIntArray() {
		final int size = finish - start;
		final int length = (size + 31) >> 5;
		final int[] ints = new int[length];
		if (length == 0) return ints;
		if ((start & ADDRESS_MASK) == 0) {
			int i = start >> ADDRESS_BITS;
			int j = length; // how many ints we have to process
			for (; j > 2; i++) {
				final long l = bits[i];
				ints[--j] = (int) (l      );
				ints[--j] = (int) (l >> 32);
			}
			if (j > 0) {
				final long m = -1L >>> (ADDRESS_SIZE - finish & ADDRESS_MASK);
				final long l = bits[i] & m;
				for (int k = 0; j > 0; k++) {
					ints[--j] = (int) (l >> (k*32));
				}
			}
		} else { // general case
			int i = 0;
			for (; i < length - 1; i++) {
				ints[length - 1 - i] = getInt(i << 5);
			}
			ints[0] = (int) getBits(i << 5, size - (i << 5));
		}
		return ints;
	}

	public long[] toLongArray() {
		// create array through an aligned copy
		BitVector copy = alignedCopy(true);
		long[] longs = copy.bits;
		int length = longs.length;
		if (length == 0) return longs;
		// reverse the array
		for (int i = 0, mid = length >> 1, j = length - 1; i < mid; i++, j--) {
			long t = longs[i];
			longs[i] = longs[j];
			longs[j] = t;
		}
		// mask off top bits in case copy was produced via clone
		final long mask = -1L >>> (ADDRESS_SIZE - copy.finish & ADDRESS_MASK);
		longs[0] &= mask;
		// return the result
		return longs;
	}

	//TODO consider renaming bigIntValue() for pseudo-consistency with Number
	public BigInteger toBigInteger() {
		return start == finish ? BigInteger.ZERO : new BigInteger(1, toByteArray());
	}

	public BitSet toBitSet() {
		final int size = finish - start;
		final BitSet bitSet = new BitSet(size);
		for (int i = 0; i < size; i++) {
			bitSet.set(i, getBitAdj(i + start));
		}
		return bitSet;
	}

	// IO

	public void write(OutputStream out) throws IOException {
		//TODO could optimize for aligned instances
		final int size = finish - start;
		final int length = (size + 7) >> 3;
		if (length == 0) return;
		int p = size & 7;
		final int q = finish - p;
		if (p != 0) out.write((byte) getBitsAdj(q, p));
		p = q;
		while (p > start) {
			p -= 8;
			out.write((byte) getBitsAdj(p, 8));
		}
	}

	public void readFrom(InputStream in) throws IOException {
		//TODO could optimize for aligned instances
		final int size = finish - start;
		final int length = (size + 7) >> 3;
		if (length == 0) return;
		int p = size & 7;
		final int q = finish - p;
		if (p != 0) performAdj(SET, q, (long) in.read(), p);
		p = q;
		while (p > start) {
			p -= 8;
			performAdj(SET, p, (long) in.read(), 8);
		}
	}

	@Override
	public void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		int size = finish - start;
		if (size <= 64) {
			performAdj(SET, start, reader.readLong(size), size) ;
		} else {
			int head = finish & ADDRESS_MASK;
			if (head != 0) performAdj(SET, finish - head, reader.readLong(head), head);
			final int f = (finish     ) >> ADDRESS_BITS;
			final int t = (start  + 63) >> ADDRESS_BITS;
			for (int i = f - 1; i >= t; i--) bits[i] = reader.readLong(ADDRESS_SIZE);
			int tail = 64 - (start & ADDRESS_MASK);
			if (tail != 64) performAdj(SET, start, reader.readLong(tail), tail);
		}
	}

	@Override
	public int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		int size = finish - start;
		int count = 0;
		if (size <= 64) {
			count += writer.write(getBitsAdj(start, size), size);
		} else {
			int head = finish & ADDRESS_MASK;
			if (head != 0) count += writer.write(getBitsAdj(finish - head, head), head);
			final int f = (finish     ) >> ADDRESS_BITS;
			final int t = (start  + 63) >> ADDRESS_BITS;
			for (int i = f - 1; i >= t; i--) count += writer.write(bits[i], ADDRESS_SIZE);
			int tail = 64 - (start & ADDRESS_MASK);
			if (tail != 64) count += writer.write(getBitsAdj(start, tail), tail);
		}
		return count;
	}

	// bitstore methods

	@Override
	public void clear(boolean value) {
		performAdj(SET, start, finish, value);
	}

	@Override
	public void setBit(int position, boolean value) {
		perform(SET, position, value);
	}

	@Override
	public boolean getThenSetBit(int position, boolean value) {
		return getThenPerform(SET, position, value);
	}

	@Override
	public void setStore(int position, BitStore store) {
		if (store instanceof BitVector) {
			perform(SET, position, (BitVector) store);
		} else {
			store.writeTo(openWriter(position));
		}
	}

	@Override
	public int countOnes() {
		return countOnesAdj(start, finish);
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public BitVector mutable() {
		return mutable ? this : mutableCopy();
	}

	@Override
	public BitVector immutable() {
		return mutable ? immutableView() : this;
	}

	@Override
	public BitVector immutableView() {
		return duplicate(false, false);
	}

	@Override
	public BitVector immutableCopy() {
		return duplicate(true, false);
	}

	@Override
	public BitVector mutableCopy() {
		return duplicate(true, true);
	}

	// convenience setters

	//named flip for consistency with BigInteger and BitSet
	@Override
	public void flip() {
		performAdj(XOR, start, finish, true);
	}

	// equivalent to xor().with(position, true)
	public void flipBit(int position) {
		perform(XOR, position, true);
	}

	//TODO consider flipRange ?

	public Op op(Operation operation) {
		if (operation == null) throw new IllegalArgumentException("null operation");
		return new Op(operation.ordinal());
	}

	public Op set() {
		return new Op(SET);
	}

	public Op and() {
		return new Op(AND);
	}

	public Op or() {
		return new Op(OR);
	}

	public Op xor() {
		return new Op(XOR);
	}

	// convenience comparisons

	@Override
	public boolean testEquals(BitStore store) {
		if (store instanceof BitVector) {
			return test(Test.EQUALS, (BitVector) store);
		} else {
			return BitStore.super.testEquals(store);
		}
	}

	@Override
	public boolean testIntersects(BitStore store) {
		if (store instanceof BitVector) {
			return test(Test.INTERSECTS, (BitVector) store);
		} else {
			return BitStore.super.testIntersects(store);
		}
	}

	@Override
	public boolean testContains(BitStore store) {
		if (store instanceof BitVector) {
			return test(Test.CONTAINS, (BitVector) store);
		} else {
			return BitStore.super.testContains(store);
		}
	}

	// convenience tests

	public boolean isAllZeros() {
		return isAllAdj(start, finish, false);
	}

	public boolean isAllOnes() {
		return isAllAdj(start, finish, true);
	}

	// convenience views

	// returns a new bitvector that is backed by the same data as this one
	// equivalent to: duplicate(from, to, false, isMutable());
	// bypasses duplicate for efficiency
	public BitVector range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BitVector(from, to, bits, mutable);
	}

	//returns a new bitvector that is backed by the same data as this one
	//equivalent to clone
	public BitVector view() {
		return clone();
	}

	// returns a new bitvector that is backed by the same data as this one
	// equivalent to: duplicate(from, to, false, false);
	// bypasses duplicate for efficiency
	public BitVector immutableRange(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BitVector(from, to, bits, false);
	}

	// convenience copies

	public BitVector copy() {
		return duplicate(true, mutable);
	}

	// convenience rotations and shifts
	// TODO consider removing these convenience methods

	public void rotateLeft(int distance) {
		rotate(distance);
	}

	public void rotateRight(int distance) {
		rotate(-distance);
	}

	public void shiftLeft(int distance) {
		shift(distance, false);
	}

	public void shiftRight(int distance) {
		shift(-distance, false);
	}

	// number methods

	@Override
	public byte byteValue() {
		return (byte) getBitsAdj(start, Math.min(8, finish-start));
	}

	@Override
	public short shortValue() {
		return (short) getBitsAdj(start, Math.min(16, finish-start));
	}

	@Override
	public int intValue() {
		return (int) getBitsAdj(start, Math.min(32, finish-start));
	}

	@Override
	public long longValue() {
		return (long) getBitsAdj(start, Math.min(64, finish-start));
	}

	@Override
	public float floatValue() {
		//TODO can make more efficient by writing a method that returns vector in base 10 string
		return toBigInteger().floatValue();
	}

	@Override
	public double doubleValue() {
		//TODO can make more efficient by writing a method that returns vector in base 10 string
		return toBigInteger().doubleValue();
	}

	// collection methods

	@Override
	public Iterator<Boolean> iterator() {
		return new BitIterator();
	}

	public ListIterator<Boolean> listIterator() {
		return new BitIterator();
	}

	public ListIterator<Boolean> listIterator(int index) {
		if (index < 0) throw new IllegalArgumentException();
		index += start;
		if (index > finish) throw new IllegalArgumentException();
		return new BitIterator(index);
	}

	public ListIterator<Integer> positionIterator() {
		return new PositionIterator();
	}

	public ListIterator<Integer> positionIterator(int position) {
		if (position < 0) throw new IllegalArgumentException();
		position += start;
		if (position > finish) throw new IllegalArgumentException();
		return new PositionIterator(position);
	}

	public List<Boolean> asList() {
		return new BitList();
	}

	public SortedSet<Integer> asSet() {
		return new IntSet(start);
	}

	// stream methods

	public BitReader openReader() {
		return new VectorReader();
	}

	public BitReader openReader(int position) {
		if (position < 0) throw new IllegalArgumentException();
		position = finish - position;
		if (position < start) throw new IllegalArgumentException();
		return new VectorReader(position);
	}

	public BitWriter openWriter() {
		return new VectorWriter();
	}

	public BitWriter openWriter(int position) {
		return openWriter(Operation.SET, position);
	}

	public BitWriter openWriter(Operation operation, int position) {
		if (operation == null) throw new IllegalArgumentException("null operation");
		if (position < 0) throw new IllegalArgumentException();
		position = finish - position;
		if (position < start) throw new IllegalArgumentException();
		return new VectorWriter(operation.ordinal(), position);
	}

	// object methods

	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitVector)) return false;
		final BitVector that = (BitVector) obj;
		if (this.finish - this.start != that.finish - that.start) return false;
		return test(EQUALS, that);
	}

	@Override
	public int hashCode() {
		final int size = finish - start;
		//trivial case
		if (size == 0) return size;
		int h = 0;
		//optimized case, starts at zero
		if (start == 0) {
			final int f = finish >> ADDRESS_BITS;
			for (int i = 0; i < f; i++) {
				final long l = bits[i];
				h = h * 31 + ((int) l       );
				h = h * 31 + ((int)(l >> 32));
			}
			if ((finish & ADDRESS_MASK) != 0) {
				final long m = -1L >>> (ADDRESS_SIZE - finish & ADDRESS_MASK);
				final long l = bits[f] & m;
				h = h * 31 + ((int) l       );
				h = h * 31 + ((int)(l >> 32));
			}
		} else {
			final int limit = size - ADDRESS_SIZE;
			for (int i = 0; i <= limit; i += ADDRESS_SIZE) {
				//TODO consider a getBitsImpl?
				long l = getBits(i, 64);
				h = h * 31 + ((int) l       );
				h = h * 31 + ((int)(l >> 32));
			}
			final int r = size & ADDRESS_MASK;
			if (r != 0) {
				final long l = getBits(size - r, r);
				h = h * 31 + ((int) l       );
				h = h * 31 + ((int)(l >> 32));
			}
		}
		return h ^ size;
	}

	@Override
	public String toString() {
		final int size = finish - start;
		switch (size) {
		case 0 : return "";
		case 1 : return getBitAdj(start) ? "1" : "0";
		default :
			StringBuilder sb = new StringBuilder(size);
			for (int i = finish - 1; i >= start; i--) {
				sb.append(getBitAdj(i) ? '1' : '0');
			}
			return sb.toString();
		}
	}

	public String toString(int radix) {
		return toBigInteger().toString(radix);
	}

	//shallow, externally identical to calling view();
	public BitVector clone() {
		try {
			return (BitVector) super.clone();
		} catch (CloneNotSupportedException e) {
			//should never occur
			throw new RuntimeException("Clone failure!", e);
		}
	}

	// serialization

	private Object writeReplace() throws ObjectStreamException {
		return new Serial(this);
	}

	// private utility methods

	private void perform(int operation, int position, boolean value) {
		if (position < 0)  throw new IllegalArgumentException();
		position += start;
		if (position >= finish) throw new IllegalArgumentException();
		performAdj(operation, position, value);
	}

	private boolean getThenPerform(int operation, int position, boolean value) {
		if (position < 0)  throw new IllegalArgumentException();
		position += start;
		if (position >= finish) throw new IllegalArgumentException();
		return getThenPerformAdj(operation, position, value);
	}

	private void performAdj(int operation, int from, int to, boolean value) {
		if (!mutable) throw new IllegalStateException();
		if (from == to) return; // nothing to do for an empty vector

		//rationalize possible operations into SETs or INVERTs
		switch (operation) {
		case AND: if (value == false) performAdj(SET, from, to, false); else return;
		case OR:  if (value == true) performAdj(SET, from, to, true); else return;
		case XOR : if (value == false) return;
		}

		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;
		final long fm = -1L << (from - f * ADDRESS_SIZE);
		final long tm = -1L >>> (t * ADDRESS_SIZE - to);

		if (f == t) { // change falls into one element
			final long mask = fm & tm;
			switch (operation) {
			case SET :
				if (value) {
					bits[f] |= mask;
				} else {
					bits[f] &= ~mask;
				}
				break;
			case XOR :
				bits[f] ^= mask;
				break;
			}
			return;
		}

		switch (operation) { //process intermediate elements
		case SET :
			Arrays.fill(bits, f+1, t, value ? -1L : 0L);
			break;
		case XOR :
			for (int i = f+1; i < t; i++) bits[i] = ~bits[i];
			break;
		}

		//process terminals
		switch (operation) {
		case SET :
			if (value) {
				bits[f] |= fm;
				bits[t] |= tm;
			} else {
				bits[f] &= ~fm;
				bits[t] &= ~tm;
			}
			break;
		case XOR :
			bits[f] ^= fm;
			bits[t] ^= tm;
			break;
		}
	}

	//assumes address size is size of long
	private void perform(int operation, int position, long bs, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (length < 0 || length > ADDRESS_SIZE) throw new IllegalArgumentException();
		position += start;
		if (position + length > finish) throw new IllegalArgumentException();
		if (!mutable) throw new IllegalStateException();
		performAdj(operation, position, bs, length);
	}

	private void performAdj(int operation, int position, long bs, int length) {
		if (length == 0) return;
		final int i = position >> ADDRESS_BITS;
		final int s = position & ADDRESS_MASK;
		final long m = length == ADDRESS_SIZE ? -1L : (1L << length) - 1L;
		final long v = bs & m;
		if (s == 0) { // fast case, long-aligned
			switch (operation) {
			case SET : bits[i] = bits[i] & ~m | v; break;
			case AND : bits[i] &= v | ~m; break;
			case OR  : bits[i] |= v; break;
			case XOR : bits[i] ^= v; break;
			}
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			switch (operation) {
			case SET : bits[i] = bits[i] & Long.rotateLeft(~m, s) | (v << s); break;
			case AND : bits[i] &= (v << s) | Long.rotateLeft(~m, s); break;
			case OR  : bits[i] |= v << s; break;
			case XOR : bits[i] ^= v << s; break;
			}

		} else {
			switch (operation) {
			case SET :
				bits[i  ] = bits[i  ] & (-1L >>> (         ADDRESS_SIZE - s)) | (v <<                  s );
				bits[i+1] = bits[i+1] & (-1L <<  (length - ADDRESS_SIZE + s)) | (v >>> (ADDRESS_SIZE - s));
				break;
			case AND :
				bits[i  ]  &= (v <<                  s ) | (-1L >>> (         ADDRESS_SIZE - s));
				bits[i+1]  &= (v >>> (ADDRESS_SIZE - s)) | (-1L <<  (length - ADDRESS_SIZE + s));
				break;
			case OR  :
				bits[i  ]  |= (v <<                  s );
				bits[i+1]  |= (v >>> (ADDRESS_SIZE - s));
				break;
			case XOR :
				bits[i  ]  ^= (v <<                  s );
				bits[i+1]  ^= (v >>> (ADDRESS_SIZE - s));
				break;
			}
		}
	}

	private void perform(int operation, BitVector that) {
		if (this.size() != that.size()) throw new IllegalArgumentException("mismatched vector size");
		perform(operation, 0, that);
	}

	private void perform(int operation, int position, BitVector that) {
		if (that == null) throw new IllegalArgumentException("null vector");
		if (position < 0) throw new IllegalArgumentException("negative position");
		if (!mutable) throw new IllegalStateException();
		position += this.start;
		if (position + that.finish - that.start > this.finish) throw new IllegalArgumentException();
		performAdj(operation, position, that);
	}

	private void perform(int operation, BitStore store) {
		if (this.size() != store.size()) throw new IllegalArgumentException("mismatched store size");
		perform(operation, 0, store);
	}

	private void perform(int operation, int position, BitStore store) {
		if (store instanceof BitVector) {
			perform(operation, position, (BitVector) store);
			return;
		}
		if (store == null) throw new IllegalArgumentException("null store");
		if (position < 0) throw new IllegalArgumentException("negative position");
		if (!mutable) throw new IllegalStateException();
		position += this.start;
		if (position + store.size() > finish) throw new IllegalArgumentException();
		performAdj(operation, position, store);
	}

	private void perform(int operation, int position, byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (position < 0) throw new IllegalArgumentException("negative position");
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length == 0) return;
		if (offset + length > (bytes.length << 3)) throw new IllegalArgumentException("length greater than number of bits in byte array");
		position += start;
		if (position + length > finish) throw new IllegalArgumentException("operation exceeds length of bit vector");
		performAdj(operation, position, bytes, offset, length);
	}

	private BitVector duplicateAdj(int from, int to, boolean copy, boolean mutable) {
		return new BitVector(from, to, copy ? bits.clone() : bits, mutable);
	}

	private boolean test(final int test, final BitVector that) {
		//trivial case
		if (this.start == this.finish) {
			switch (test) {
			case EQUALS : return true;
			case INTERSECTS : return false;
			case CONTAINS : return true;
			case COMPLEMENTS : return true;
			default : throw new IllegalArgumentException("Unexpected comparison constant: " + test);
			}
		}
		//TODO worth optimizing for case where this == that?
		//fully optimal case - both start at 0
		//TODO can weaken this constraint - can optimize if their start are equal
		if (this.start == 0 && that.start == 0) {
			final long[] thisBits = this.bits;
			final long[] thatBits = that.bits;
			final int t = (finish-1) >> ADDRESS_BITS;
			switch (test) {
			case EQUALS :
				for (int i = t-1; i >= 0; i--) {
					if (thisBits[i] != thatBits[i]) return false;
				}
				break;
			case INTERSECTS :
				for (int i = t-1; i >= 0; i--) {
					if ((thisBits[i] & thatBits[i]) != 0) return true;
				}
				break;
			case CONTAINS :
				for (int i = t-1; i >= 0; i--) {
					final long bits = thisBits[i];
					if ((bits | thatBits[i]) != bits) return false;
				}
				break;
			case COMPLEMENTS :
				for (int i = t-1; i >= 0; i--) {
					if (~thisBits[i] != thatBits[i]) return false;
				}
				break;
			default : throw new IllegalArgumentException("Unexpected comparison constant: " + test);
			}
			{
				// same length & same start so same finish mask
				final long m = -1L >>> (ADDRESS_SIZE - finish & ADDRESS_MASK);
				final long thisB = thisBits[t] & m;
				final long thatB = thatBits[t] & m;
				switch (test) {
				case EQUALS : return thisB == thatB;
				case INTERSECTS : return (thisB & thatB) != 0;
				case CONTAINS : return (thisB | thatB) == thisB;
				case COMPLEMENTS : return (~thisB & m) == thatB;
				default : throw new IllegalArgumentException("Unexpected comparison constant: " + test);
				}
			}
		}
		//TODO an additional optimization is possible when their starts differ by 64
		//partially optimal case - both are address aligned
		if ((this.start & ADDRESS_MASK) == 0 && (that.start & ADDRESS_MASK) == 0 && (this.finish & ADDRESS_MASK) == 0) {
			final long[] thisBits = this.bits;
			final long[] thatBits = that.bits;
			final int f = this.start >> ADDRESS_BITS;
			final int t = this.finish >> ADDRESS_BITS;
			final int d = (that.start - this.start) >> ADDRESS_BITS;
			switch (test) {
			case EQUALS :
				for (int i = f; i < t; i++) {
					if (thisBits[i] != thatBits[i+d]) return false;
				}
				return true;
			case INTERSECTS :
				for (int i = f; i < t; i++) {
					if ((thisBits[i] & thatBits[i+d]) != 0) return true;
				}
				return false;
			case CONTAINS :
				for (int i = f; i < t; i++) {
					final long bits = thisBits[i];
					if ((bits | thatBits[i+d]) != bits) return false;
				}
				return true;
			case COMPLEMENTS :
				for (int i = f; i < t; i++) {
					if (~thisBits[i] != thatBits[i+d]) return false;
				}
				return true;
			default : throw new IllegalArgumentException("Unexpected comparison constant: " + test);
			}
		}
		//non-optimized case
		//TODO consider if this can be gainfully optimized
		final int size = finish - start;
		switch (test) {
		case EQUALS :
			for (int i = 0; i < size; i++) {
				if (that.getBitAdj(that.start + i) != this.getBitAdj(this.start + i)) return false;
			}
			return true;
		case INTERSECTS :
			for (int i = 0; i < size; i++) {
				if (that.getBitAdj(that.start + i) && this.getBitAdj(this.start + i)) return true;
			}
			return false;
		case CONTAINS :
			for (int i = 0; i < size; i++) {
				if (that.getBitAdj(that.start + i) && !this.getBitAdj(this.start + i)) return false;
			}
			return true;
		case COMPLEMENTS :
			for (int i = 0; i < size; i++) {
				if (that.getBitAdj(that.start + i) == this.getBitAdj(this.start + i)) return false;
			}
			return true;
		default : throw new IllegalArgumentException("Unexpected comparison constant: " + test);
		}
	}

	private boolean getBitAdj(int position) {
		final int i = position >> ADDRESS_BITS;
		final long m = 1L << (position & ADDRESS_MASK);
		return (bits[i] & m) != 0;
	}

	private long getBitsAdj(int position, int length) {
		final int i = position >> ADDRESS_BITS;
		if (i >= bits.length) return 0L; // may happen if position == finish
		final int s = position & ADDRESS_MASK;
		final long b;
		if (s == 0) { // fast case, long-aligned
			b = bits[i];
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			b = bits[i] >>> s;
		} else {
			b = (bits[i] >>> s) | (bits[i+1] << (ADDRESS_SIZE - s));
		}
		return length == ADDRESS_SIZE ? b : b & ((1L << length) - 1);
	}
	private BitVector getVectorAdj(int position, int length, boolean mutable) {
		final long[] newBits;
		if (length == 0) {
			newBits = new long[0];
		} else {
			final int from = position >> ADDRESS_BITS;
			final int to = (position + length + ADDRESS_MASK) >> ADDRESS_BITS;
			if ((position & ADDRESS_MASK) == 0) {
				newBits = copier.copy(bits, from, to);
			} else {
				final int s = position & ADDRESS_MASK;
				final int plen = to - from; // number of longs which need processing
				final int alen = (length + ADDRESS_MASK) >> ADDRESS_BITS; // number of longs to return
				newBits = new long[alen];
				//do all but last bit
				int j = from;
				int i = 0;
				for (; i < alen - 1; i++, j++) {
					newBits[i] = (bits[j] >>> s) | (bits[j+1] << (ADDRESS_SIZE - s));
				}
				//do last bits as a special case
				if (plen == alen) {
					newBits[i] = bits[j] >>> s;
				} else {
					newBits[i] = (bits[j] >>> s) | (bits[j+1] << (ADDRESS_SIZE - s));
				}
			}
		}
		return new BitVector(0, length, newBits, mutable);
	}

	private int countOnesAdj(int from, int to) {
		if (from == to) return 0;
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;
		final int r = from & ADDRESS_MASK;
		final int l = to & ADDRESS_MASK;
		if (f == t) {
			//alternatively: (0x8000000000000000L >> (l - r - 1)) >>> (ADDRESS_SIZE - l);
			final long m = (-1L >>> (ADDRESS_SIZE - l + r)) << r;
			return Long.bitCount(m & bits[f]);
		}

		int count = 0;
		count += Long.bitCount( (-1L << r) & bits[f] );
		for (int i = f+1; i < t; i++) {
			count += Long.bitCount(bits[i]);
		}
		count += Long.bitCount( (-1L >>> (ADDRESS_SIZE - l)) & bits[t] );
		return count;
	}

	private boolean isAllAdj(int from, int to, boolean value) {
		if (from == to) return true;
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;

		final long fm;
		final long tm;
		{
			final int fs = from & ADDRESS_MASK;
			final int ts = to & ADDRESS_MASK;
			if (value) {
				fm = fs == 0 ? 0 : -1L >>> (ADDRESS_SIZE - fs);
				tm = ts == 0 ? 0 : -1L << ts;
			} else {
				fm = -1L << fs;
				tm = -1L >>> (ADDRESS_SIZE - ts);
			}
		}

		if (f == t) { // bits fit into a single element
			if (value) {
				return (bits[f] | fm | tm) == -1L;
			} else {
				return (bits[f] & fm & tm) == 0L;
			}
		}

		//check intermediate elements
		if (value) {
			for (int i = f+1; i < t; i++) if (bits[i] != -1L) return false;
		} else {
			for (int i = f+1; i < t; i++) if (bits[i] != 0L) return false;
		}

		//check terminals
		if (value) {
			return (bits[f] | fm) == -1L && (bits[t] | tm) == -1L;
		} else {
			return (bits[f] & fm) == 0L && (bits[t] & tm) == 0L;
		}
	}

	private void performAdj(int operation, int position, boolean value) {
		if (!mutable) throw new IllegalStateException();
		final int i = position >> ADDRESS_BITS;
		final long m = 1L << (position & ADDRESS_MASK);
		switch(operation) {
		case SET :
			if (value) {
				bits[i] |=  m;
			} else {
				bits[i] &= ~m;
			}
			break;
		case AND :
			if (value) {
				/* no-op */
			} else {
				bits[i] &= ~m;
			}
			break;
		case OR :
			if (value) {
				bits[i] |=  m;
			} else {
				/* no-op */
			}
			break;
		case XOR :
			if (value) {
				bits[i] ^=  m;
			} else {
				/* no-op */
			}
			break;
		}

	}

	//TODO really needs a more efficient implementation (see below for a failure)
	private void performAdj(int operation, int position, byte[] bytes, int offset, int length) {
		if (!mutable) throw new IllegalStateException();
		final int to = (bytes.length << 3) - offset;
		final int from = to - length;
		position += length;
		for (int i = from; i < to; i++) {
			boolean b = (bytes[i >> 3] & (128 >> (i & 7))) != 0;
			performAdj(operation, --position, b);
		}
	}
/*
	// implementation that is totally bogus because array needs to considered bwards
	private void performAdj(int operation, int position, byte[] bytes, int offset, int length) {
		if (!mutable) throw new IllegalStateException();
		final int limit = offset + length;
		//knock off any initial unaligned bits
		if ((offset & 7) != 0) {
			int prelim = Math.min( (offset | 7) + 1, limit);
			for (; offset < prelim; offset++) {
				boolean b = ((bytes[offset >> 3] >> (offset & 7) ) & 1) != 0;
				performAdj(operation, position++, b);
			}
			length = limit - offset;
			if (length == 0) return;
		}
		//at this point we are byte aligned
		final int byteLimit = limit >> 3;
		int j = offset >> 3;
		//bunch as many bytes as we can into longs and operate with those
		for (; j + 8 <= byteLimit; j += 8) {
			final long bs =
				((bytes[j    ] & 0xff) << 56) |
				((bytes[j + 1] & 0xff) << 48) |
				((bytes[j + 2] & 0xff) << 40) |
				((bytes[j + 3] & 0xff) << 32) |
				((bytes[j + 4] & 0xff) << 24) |
				((bytes[j + 5] & 0xff) << 16) |
				((bytes[j + 6] & 0xff) <<  8) |
				((bytes[j + 7] & 0xff)      );
			performAdj(operation, position, bs, 64);
			position += 64;
		}
		//now we have less than a long's worth of bits left, operate in bytes
		for (; j < byteLimit; j++) {
			performAdj(operation, position, bytes[j], 8);
			position += 8;
		}
		//finally we may have less than a byte's worth of bits left - mop them up
		offset = j << 3;
		length = limit - offset;
		if (length > 0) performAdj(operation, position, bytes[j], length);
	}
*/

	private void performAdj(int operation, int position, BitVector that) {
		final int thatSize = that.size();
		if (thatSize == 0) return;
		if (this.bits == that.bits && overlapping(position, position + thatSize, that.start, that.finish)) that = that.copy();
		if (thatSize <= ADDRESS_SIZE) {
			performAdj(operation, position, that.getBitsAdj(that.start, thatSize), thatSize);
		} else {
			// TODO would like not to depend on writer here
			// a direct implementation would be faster
			that.writeTo( new VectorWriter(operation, position + thatSize) );
		}
	}

	private void performAdj(int operation, int position, BitStore store) {
		final int storeSize = store.size();
		// note - can't defend against possibility of overlapping data backing here
		// note 8 here is a heuristic
		if (storeSize < 8) {
			for (int i = 0; i < storeSize; i++) {
				performAdj(operation, position++, store.getBit(i));
			}
		} else {
			store.writeTo( new VectorWriter(operation, position + storeSize) );
		}
	}

	//specialized implementation for the common case of setting an individual bit

	private void performSetAdj(int position, boolean value) {
		if (!mutable) throw new IllegalStateException();
		final int i = position >> ADDRESS_BITS;
		final long m = 1L << (position & ADDRESS_MASK);
		if (value) {
			bits[i] |=  m;
		} else {
			bits[i] &= ~m;
		}
	}

	//separate implementation from performAdj is an optimization

	private boolean getThenPerformAdj(int operation, int position, boolean value) {
		if (!mutable) throw new IllegalStateException();
		final int i = position >> ADDRESS_BITS;
		final long m = 1L << (position & ADDRESS_MASK);
		final long v = bits[i] & m;
		switch(operation) {
		case SET :
			if (value) {
				bits[i] |=  m;
			} else {
				bits[i] &= ~m;
			}
			break;
		case AND :
			if (value) {
				/* no-op */
			} else {
				bits[i] &= ~m;
			}
			break;
		case OR :
			if (value) {
				bits[i] |=  m;
			} else {
				/* no-op */
			}
			break;
		case XOR :
			if (value) {
				bits[i] ^=  m;
			} else {
				/* no-op */
			}
			break;
		}
		return v != 0;
	}

	private void rotateAdj(int from, int to, int distance) {
		final int length = to - from;
		if (length < 2) return;
		distance = distance % length;
		if (distance < 0) distance += length;
		if (distance == 0) return;

		//TODO is this capable of optimization in some cases?
		final int cycles = gcd(distance, length);
		for (int i = from + cycles - 1; i >= from; i--) {
			boolean m = getBitAdj(i); // the previously overwritten value
			int j = i; // the index that is to be overwritten next
			do {
				j += distance;
				if (j >= to) j -= length;
				m = getThenPerformAdj(SET, j, m);
			} while (j != i);
		}
	}

	private void shiftAdj(int from, int to, int distance, boolean fill) {
		if (from == to) return;
		if (distance == 0) return;

		//TODO this capable of optimization in some cases
		if (distance > 0) {
			int j = to - 1;
			for (int i = j - distance; i >= from; i--, j--) {
				performSetAdj(j, getBitAdj(i));
			}
			performAdj(SET, from, j + 1, fill);
		} else {
			int j = from;
			for (int i = j - distance; i < to; i++, j++) {
				performSetAdj(j, getBitAdj(i));
			}
			performAdj(SET, j, to, fill);
		}

	}

	private void reverseAdj(int from, int to) {
		to--;
		while (from < to) {
			performSetAdj(to, getThenPerformAdj(SET, from, getBitAdj(to)));
			from++; to--;
		}
	}

	private void shuffleAdj(int from, int to, Random random) {
		int size = to - from;
		int ones = countOnesAdj(from, to);
		// simple case - all bits identical, nothing to do
		if (ones == 0 || ones == size) return;
		// relocate one-bits
		//TODO could set multiple bits at once for better performance
		for (int i = from; ones < size && ones > 0; size--) {
			boolean one = random.nextInt(size) < ones;
			performSetAdj(i++, one);
			if (one) ones--;
		}
		// fill remaining definites
		if (size > 0) performAdj(SET, to - size, to, ones > 0);
	}

	//TODO can eliminate calls to getBitsAdj from these methods

	private int firstOneInRangeAdj(int from, int to) {
		// trivial case
		if (from == to) return to;
		final int size = to - from;
		//simple case
		if (size <= ADDRESS_SIZE) {
			final int j = Long.numberOfTrailingZeros( getBitsAdj(from, size) );
			return j >= size ? to : from + j;
		}
		int i = from;
		// check head
		int a = i & ADDRESS_MASK;
		if (a != 0) {
			final int s = ADDRESS_SIZE - a;
			final int j = Long.numberOfTrailingZeros( getBitsAdj(i, s) );
			if (j < s) return from + j;
			i += s;
		}
		// check body
		final int b = to & ADDRESS_MASK;
		final int t = to - b;
		while (i < t) {
			final int j = Long.numberOfTrailingZeros( bits[i >> ADDRESS_BITS] );
			if (j < ADDRESS_SIZE) return i + j;
			i += ADDRESS_SIZE;
		}
		// check tail
		if (b != 0) {
			final int j = Long.numberOfTrailingZeros( getBitsAdj(t, b) );
			return j >= b ? to : i + j;
		}
		// give up
		return to;
	}

	private int firstZeroInRangeAdj(int from, int to) {
		// trivial case
		if (from == to) return to;
		final int size = to - from;
		//simple case
		if (size <= ADDRESS_SIZE) {
			final int j = Long.numberOfTrailingZeros( ~getBitsAdj(from, size) );
			return j >= size ? to : from + j;
		}
		int i = from;
		// check head
		int a = i & ADDRESS_MASK;
		if (a != 0) {
			final int s = ADDRESS_SIZE - a;
			final int j = Long.numberOfTrailingZeros( ~getBitsAdj(i, s) );
			if (j < s) return from + j;
			i += s;
		}
		// check body
		final int b = to & ADDRESS_MASK;
		final int t = to - b;
		while (i < t) {
			final int j = Long.numberOfTrailingZeros( ~bits[i >> ADDRESS_BITS] );
			if (j < ADDRESS_SIZE) return i + j;
			i += ADDRESS_SIZE;
		}
		// check tail
		if (b != 0) {
			final int j = Long.numberOfTrailingZeros( ~getBitsAdj(t, b) );
			return j >= b ? to : i + j;
		}
		// give up
		return to;
	}

	private int lastOneInRangeAdj(int from, int to) {
		// trivial case
		if (from == to) return start - 1;
		final int size = to - from;
		//simple case
		if (size <= ADDRESS_SIZE) {
			final int j = Long.numberOfLeadingZeros( getBitsAdj(from, size) << (ADDRESS_SIZE - size) );
			return j == ADDRESS_SIZE ? start - 1 : to - (j + 1);
		}
		// check tail
		final int b = to & ADDRESS_MASK;
		int i = to - b;
		if (b != 0) {
			final int j = Long.numberOfLeadingZeros( getBitsAdj(i, b) << (ADDRESS_SIZE - b) );
			if (j != ADDRESS_SIZE) return to - (j + 1);
		}
		// check body
		final int a = from & ADDRESS_MASK;
		final int f = from + (ADDRESS_SIZE - a);
		while (i >= f) {
			i -= ADDRESS_SIZE;
			final int j = Long.numberOfLeadingZeros( bits[i >> ADDRESS_BITS] );
			if (j != ADDRESS_SIZE) return i + ADDRESS_SIZE - (j + 1);
		}
		// check head
		if (a != 0) {
			final int s = ADDRESS_SIZE - a;
			final int j = Long.numberOfLeadingZeros( getBitsAdj(from, s) << a );
			if (j != ADDRESS_SIZE) return i - (j + 1);
		}
		// give up
		return start - 1;
	}

	private int lastZeroInRangeAdj(int from, int to) {
		// trivial case
		if (from == to) return start - 1;
		final int size = to - from;
		//simple case
		if (size <= ADDRESS_SIZE) {
			final int j = Long.numberOfLeadingZeros( ~getBitsAdj(from, size) << (ADDRESS_SIZE - size) );
			return j == ADDRESS_SIZE ? start - 1 : to - (j + 1);
		}
		// check tail
		final int b = to & ADDRESS_MASK;
		int i = to - b;
		if (b != 0) {
			final int j = Long.numberOfLeadingZeros( ~getBitsAdj(i, b) << (ADDRESS_SIZE - b) );
			if (j != ADDRESS_SIZE) return to - (j + 1);
		}
		// check body
		final int a = from & ADDRESS_MASK;
		final int f = from + (ADDRESS_SIZE - a);
		while (i >= f) {
			i -= ADDRESS_SIZE;
			final int j = Long.numberOfLeadingZeros( ~bits[i >> ADDRESS_BITS] );
			if (j != ADDRESS_SIZE) return i + ADDRESS_SIZE - (j + 1);
		}
		// check head
		if (a != 0) {
			final int s = ADDRESS_SIZE - a;
			final int j = Long.numberOfLeadingZeros( ~getBitsAdj(from, s) << a );
			if (j != ADDRESS_SIZE) return i - (j + 1);
		}
		// give up
		return start - 1;
	}

	private IntSet asSet(int offset) {
		return new IntSet(offset);
	}

	// inner classes

	public final class Op {

		private final int op;

		Op(int op) {
			this.op = op;
		}

		public Operation getOperation() {
			return Operation.values[op];
		}

		public void with(boolean value) {
			performAdj(op, start, finish, value);
		}

		public void withBit(int position, boolean value) {
			perform(op, position, value);
		}

		public boolean getThenWithBit(int position, boolean value) {
			return getThenPerform(op, position, value);
		}

		public void withByte(int position, byte value) {
			perform(op, position, value, 8);
		}

		public void withShort(int position, short value) {
			perform(op, position, value, 16);
		}

		public void withInt(int position, short value) {
			perform(op, position, value, 32);
		}

		public void withLong(int position, short value) {
			perform(op, position, value, 64);
		}

		public void withBits(int position, long value, int length) {
			perform(op, position, value, length);
		}

		public void withVector(BitVector vector) {
			perform(op, vector);
		}

		public void withVector(int position, BitVector vector) {
			perform(op, position, vector);
		}

		public void withStore(BitStore store) {
			perform(op, store);
		}

		public void withStore(int position, BitStore store) {
			perform(op, position, store);
		}

		public void withBytes(int position, byte[] bytes, int offset, int length) {
			perform(op, position, bytes, offset, length);
		}

	}

	//TODO could extend to general patterns
	public abstract class Matches {

		public abstract int count();

		public abstract int first();

		public abstract int last();

		public abstract int next(int position);

		public abstract int previous(int position);

	}

	private final class MatchesOnes extends Matches {

		@Override
		public int count() {
			return countOnesAdj(start, finish);
		}

		@Override
		public int first() {
			return firstOneInRangeAdj(start, finish) - start;
		}

		@Override
		public int last() {
			return lastOneInRangeAdj(start, finish) - start;
		}

		@Override
		public int next(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position > finish) throw new IllegalArgumentException();
			return firstOneInRangeAdj(position, finish) - start;
		}

		public int previous(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position - 1 > finish) throw new IllegalArgumentException();
			return lastOneInRangeAdj(start, position) - start;
		}

	}

	private final class MatchesZeros extends Matches {

		@Override
		public int count() {
			return finish - start - countOnes();
		}

		@Override
		public int first() {
			return firstZeroInRangeAdj(start, finish) - start;
		}

		@Override
		public int last() {
			return lastZeroInRangeAdj(start, finish) - start;
		}

		@Override
		public int next(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position > finish) throw new IllegalArgumentException();
			return firstZeroInRangeAdj(position, finish) - start;
		}

		@Override
		public int previous(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position - 1 > finish) throw new IllegalArgumentException();
			return lastZeroInRangeAdj(start, position) - start;
		}

	}

	//TODO make public and expose more efficient methods?
	private final class BitIterator implements ListIterator<Boolean> {

		private final int from;
		private final int to;
		// points to the element that will be returned  by next
		private int index;
		private int recent = -1;

		BitIterator(int from, int to, int index) {
			this.from = from;
			this.to = to;
			this.index = index;
		}

		BitIterator(int index) {
			this(start, finish, index);
		}

		BitIterator() {
			this(start, finish, start);
		}

		@Override
		public boolean hasNext() {
			return index < to;
		}

		@Override
		public Boolean next() {
			if (!hasNext()) throw new NoSuchElementException();
			recent = index;
			return Boolean.valueOf( getBitAdj(index++) );
		}

		@Override
		public int nextIndex() {
			return hasNext() ? index - start : -1;
		}

		@Override
		public boolean hasPrevious() {
			return index > from;
		}

		@Override
		public Boolean previous() {
			if (!hasPrevious()) throw new NoSuchElementException();
			recent = --index;
			return Boolean.valueOf( getBitAdj(recent) );
		}

		@Override
		public int previousIndex() {
			return hasPrevious() ? index - start - 1 : -1;
		}

		@Override
		public void set(Boolean bit) {
			if (recent == -1) throw new IllegalStateException();
			performAdj(SET, recent, bit);
		}

		@Override
		public void add(Boolean bit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	//TODO make public and expose more efficient methods?
	private final class PositionIterator implements ListIterator<Integer> {

		private static final int NOT_SET = Integer.MIN_VALUE;

		private final int from;
		private final int to;
		private int previous;
		private int next;
		private int nextIndex;
		private int recent = NOT_SET;

		PositionIterator(int from, int to, int position) {
			this.from = from;
			this.to = to;
			previous = lastOneInRangeAdj(from, position);
			next = firstOneInRangeAdj(position, to);
			nextIndex = previous == -1 ? 0 : NOT_SET;
		}

		PositionIterator(int index) {
			this(start, finish, index);
		}

		PositionIterator() {
			this(start, finish, start);
		}

		@Override
		public boolean hasPrevious() {
			return previous != -1;
		}

		@Override
		public boolean hasNext() {
			return next != to;
		}

		@Override
		public Integer previous() {
			if (previous == -1) throw new NoSuchElementException();
			recent = previous;
			next = recent;
			previous = lastOneInRangeAdj(from, recent);
			if (nextIndex != NOT_SET) nextIndex--;
			return next - start;
		}

		@Override
		public Integer next() {
			if (next == to) throw new NoSuchElementException();
			recent = next;
			previous = recent;
			next = firstOneInRangeAdj(recent + 1, to);
			if (nextIndex != NOT_SET) nextIndex++;
			return previous - start;
		}

		@Override
		public int previousIndex() {
			return nextIndex() - 1;
		}

		@Override
		public int nextIndex() {
			return nextIndex == NOT_SET ? nextIndex = countOnesAdj(from, next) : nextIndex;
		}

		@Override
		public void add(Integer e) {
			doAdd(e);
			recent = NOT_SET;
		}

		@Override
		public void remove() {
			doRemove();
			recent = NOT_SET;
		}

		@Override
		public void set(Integer e) {
			doRemove();
			doAdd(e);
			recent = NOT_SET;
		}

		private void doAdd(Integer e) {
			if (e == null) throw new IllegalArgumentException("null e");
			int i = start + e;
			if (i < start || i >= finish) throw new IllegalArgumentException("e out of bounds: [" + 0 + "," + (finish - start) + "]");
			if (i < previous) throw new IllegalArgumentException("e less than previous value: " + (previous - start));
			if (i >= next) throw new IllegalArgumentException("e not less than next value: " + (next - start));
			boolean changed = !getThenPerformAdj(SET, i, true);
			if (changed) {
				if (nextIndex != NOT_SET) nextIndex ++;
				previous = i;
			}
		}

		private void doRemove() {
			if (recent == previous) { // we went forward
				previous = lastOneInRangeAdj(from, recent);
				if (nextIndex != NOT_SET) nextIndex --;
			} else if (recent == next) { // we went backwards
				next = firstOneInRangeAdj(recent + 1, to);
			} else { // no recent value
				throw new IllegalStateException();
			}
			performAdj(SET, recent, false);
		}

	}

	private final class BitList extends AbstractList<Boolean> {

		@Override
		public boolean isEmpty() {
			return BitVector.this.size() == 0;
		}

		@Override
		public int size() {
			return BitVector.this.size();
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Boolean)) return false;
			return !isAll(!(Boolean)o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!(o instanceof Boolean)) return false;
				if (isAll(!(Boolean)o)) return false;
			}
			return true;
		}

		@Override
		public Boolean get(int index) {
			return getBit(index);
		}

		@Override
		public Iterator<Boolean> iterator() {
			return BitVector.this.listIterator();
		}

		@Override
		public ListIterator<Boolean> listIterator() {
			return BitVector.this.listIterator();
		}

		@Override
		public ListIterator<Boolean> listIterator(int index) {
			return BitVector.this.listIterator(index);
		}

		@Override
		public int indexOf(Object object) {
			if (!(object instanceof Boolean)) return -1;
			boolean bit = (Boolean) object;
			int position = bit ?
				firstOneInRangeAdj(start, finish) :
				firstZeroInRangeAdj(start, finish);
			return position == finish ? -1 : position - start;
		}

		@Override
		public int lastIndexOf(Object object) {
			if (!(object instanceof Boolean)) return -1;
			boolean bit = (Boolean) object;
			int position = bit ?
					lastOneInRangeAdj(start, finish) :
					lastZeroInRangeAdj(start, finish);
			return position - start;
		}

		@Override
		public Boolean set(int index, Boolean element) {
			boolean b = element;
			return getThenSetBit(index, b) != b;
		}

		@Override
		public List<Boolean> subList(int fromIndex, int toIndex) {
			return range(fromIndex, toIndex).asList();
		}

	}

	private final class IntSet extends AbstractSet<Integer> implements SortedSet<Integer> {

		private final int offset; // the value that must be added to received values to map them onto the bits

		// constructors

		private IntSet(int offset) {
			this.offset = offset;
		}

		// set methods

		@Override
		public int size() {
			return countOnesAdj(start, finish);
		}

		@Override
		public boolean isEmpty() {
			return isAllAdj(start, finish, false);
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Integer)) return false;
			int position = offset + (Integer) o;
			return position >= start && position < finish && getBitAdj(position);
		}

		@Override
		public boolean add(Integer e) {
			return !getThenPerformAdj(SET, position(e), true);
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Integer)) return false;
			int i = offset + (Integer) o;
			if (i < start || i >= finish) return false;
			return getThenSetBit(i, false);
		}

		@Override
		public void clear() {
			performAdj(SET, start, finish, false);
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				final Iterator<Integer> it = positionIterator();

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Integer next() {
					//TODO build offset into positionIterator?
					return it.next() + start - offset;
				}

				@Override
				public void remove() {
					it.remove();
				}

			};
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			//TODO optimize
//			if (c instanceof IntSet) {
//			}

			return super.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends Integer> c) {
			//TODO optimize
//			if (c instanceof IntSet) {
//			}

			for (Integer e : c) position(e);
			Iterator<? extends Integer> it = c.iterator();
			boolean changed = false;
			while (!changed && it.hasNext()) {
				changed = !getThenPerformAdj(SET, it.next() + offset, true);
			}
			while (it.hasNext()) {
				performAdj(SET, it.next() + offset, true);
			}
			return changed;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			//TODO optimize
//			if (c instanceof IntSet) {
//			}

			return super.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			//TODO optimize
//			if (c instanceof IntSet) {
//			}

			return super.retainAll(c);
		}

		// sorted set methods

		@Override
		public Comparator<? super Integer> comparator() {
			return null;
		}

		@Override
		public Integer first() {
			int i = firstOneInRangeAdj(start, finish);
			if (i == finish) throw new NoSuchElementException();
			return i - offset;
		}

		@Override
		public Integer last() {
			int i = lastOneInRangeAdj(start, finish);
			if (i == start - 1) throw new NoSuchElementException();
			return i - offset;
		}

		@Override
		public SortedSet<Integer> headSet(Integer toElement) {
			if (toElement == null) throw new NullPointerException();
			int to = Math.max(toElement + offset, start);
			return duplicateAdj(start, to, false, mutable).asSet(offset);
		}

		@Override
		public SortedSet<Integer> tailSet(Integer fromElement) {
			if (fromElement == null) throw new NullPointerException();
			int from = Math.min(fromElement + offset, finish);
			return duplicateAdj(from, finish, false, mutable).asSet(offset);
		}

		@Override
		public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
			if (fromElement == null) throw new NullPointerException();
			if (toElement == null) throw new NullPointerException();
			int fromInt = fromElement;
			int toInt = toElement;
			if (fromInt > toInt) throw new IllegalArgumentException("from exceeds to");
			int from = Math.min(fromInt + offset, finish);
			int to = Math.max(toInt + offset, start);
			return duplicateAdj(from, to, false, mutable).asSet(offset);
		}

		// object methods

		@Override
		public boolean equals(Object o) {
			//TODO optimize
//			if (c instanceof IntSet) {
//			}
			return super.equals(o);
		}

		// private methods

		private int position(Integer e) {
			if (e == null) throw new NullPointerException("null value");
			int i = e + offset;
			if (i < start) throw new IllegalArgumentException("value less than lower bound");
			if (i >= finish) throw new IllegalArgumentException("value greater than upper bound");
			return i;
		}

	}

	private static class Serial implements Serializable {

		private static final long serialVersionUID = -1476938830216828886L;

		private int start;
		private int finish;
		private long[] bits;
		private boolean mutable;

		Serial(BitVector v) {
			start = v.start;
			finish = v.finish;
			bits = v.bits;
			mutable = v.mutable;
		}

		private Object readResolve() throws ObjectStreamException {
			return new BitVector(this);
		}

	}

	// classes for reading and writing bits

	private final class VectorReader implements BitReader {

		private final long initialPosition;
		private int position;

		private VectorReader(int position) {
			this.initialPosition = position;
			this.position = position;
		}

		private VectorReader() {
			this(finish);
		}

		@Override
		public int readBit() {
			if (position == start) throw new EndOfBitStreamException();
			return getBitAdj(--position) ? 1 : 0;
		}

		@Override
		public int read(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 32) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0;
			if (position - count < start) throw new EndOfBitStreamException();
			return (int) getBitsAdj(position -= count, count);
		}

		@Override
		public long readLong(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0L;
			if (position - count < start) throw new EndOfBitStreamException();
			return getBitsAdj(position -= count, count);
		}

		@Override
		public BigInteger readBigInt(int count) throws BitStreamException {
			if (position - count < start) throw new EndOfBitStreamException();
			switch(count) {
			case 0 : return BigInteger.ZERO;
			case 1 : return getBitAdj(position--) ? BigInteger.ZERO : BigInteger.ONE;
			default :
				final int from = position - count;
				final int to = position;
				position = from;
				return duplicateAdj(from, to, false, false).toBigInteger();
			}
		}

		@Override
		public boolean readBoolean() {
			if (position == start) throw new EndOfBitStreamException();
			return getBitAdj(--position);
		}

		@Override
		public int readUntil(boolean one) throws BitStreamException {
			int index = one ? lastOneInRangeAdj(start, position) : lastZeroInRangeAdj(start, position);
			if (index < start) throw new EndOfBitStreamException();
			int read = position - index - 1;
			position = index;
			return read;
		}

		@Override
		public long skipBits(long count) {
			if (count < 0L) throw new IllegalArgumentException("negative count");
			int remaining = position - start;
			long advance = remaining < count ? remaining : count;
			position -= (int) advance;
			return advance;
		}

		@Override
		public long getPosition() {
			return initialPosition - position;
		}

		@Override
		public long setPosition(long position) {
			if (position < 0) throw new IllegalArgumentException();
			//TODO need to guard against overflow?
			return this.position = Math.max((int) (initialPosition - position), start);
		}

	}

	private final class VectorWriter implements BitWriter {

		private final long initialPosition;
		private final int operation;
		private int position;

		private VectorWriter(int operation, int position) {
			this.operation = operation;
			this.initialPosition = position;
			this.position = position;
		}

		private VectorWriter() {
			this(SET, finish);
		}

		@Override
		public int writeBit(int bit) {
			if (position == start) throw new EndOfBitStreamException();
			//TODO consider an optimized version of this
			performAdj(operation, --position, (bit & 1) == 1);
			return 1;
		}

		@Override
		public int writeBoolean(boolean bit) {
			if (position == start) throw new EndOfBitStreamException();
			performAdj(operation, --position, bit);
			return 1;
		}

		@Override
		public long writeBooleans(boolean value, long count) {
			//TODO need to guard against overflow?
			if (position - count < start) throw new EndOfBitStreamException();
			int from = position - (int) count;
			performAdj(operation, from, position, value);
			position = from;
			return count;
		}

		@Override
		public int write(int bits, int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 32) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0;
			if (position - count < start) throw new EndOfBitStreamException();
			performAdj(operation, position -= count, bits, count);
			return count;
		}

		@Override
		public int write(long bits, int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0;
			if (position - count < start) throw new EndOfBitStreamException();
			performAdj(operation, position -= count, bits, count);
			return count;
		}

		@Override
		public int write(BigInteger bits, int count) {
			if (bits == null) throw new IllegalArgumentException("null bits");
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count == 0) return 0;
			if (count <= 64) {
				performAdj(operation, position -= count, bits.longValue(), count);
			} else {
				for (int i = count - 1; i >= 0; i--) {
					performAdj(operation, position--, bits.testBit(i));
				}
			}
			return count;
		}

		@Override
		public long getPosition() {
			return initialPosition - position;
		}

	}

	// classes to accommodate environments that don't have Arrays.copyOfRange

	private interface ArrayCopier {

		long[] copy(long[] array, int from, int to);
	}

	private static class RangeCopier implements ArrayCopier {

		@Override
		public long[] copy(long[] array, int from, int to) {
			return Arrays.copyOfRange(array, from, to);
		}

	}

	private static class SystemCopier implements ArrayCopier {

		@Override
		public long[] copy(long[] array, int from, int to) {
			long[] copy = new long[to - from];
			System.arraycopy(array, from, copy, 0, to - from);
			return copy;
		}

	}

}
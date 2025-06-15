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

import static com.tomgibara.bits.Bits.checkBitsLength;
import static com.tomgibara.bits.Bits.checkIntBitsLength;

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

import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;
import com.tomgibara.fundament.Alignable;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

/**
 * <p>
 * Provides a 'cannonical' {@link BitStore} implementation for situations where
 * bit operations must be performed with a trusted implementation. The class is
 * marked as final to ensure that immutable instances can be safely used in
 * security sensitive code (eg. within a {@link PrivilegedAction}).
 *
 * <p>
 * Instances of this class may be <em>aligned</em> (the class implements the
 * <code>Alignable</code> interface - see {@link #isAligned()} and
 * {@link #aligned()}). Many operations will execute more efficiently on aligned
 * instances. Instances may also be immutable (see {@link #isMutable()},
 * {@link #mutable()} and {@link #immutable()}).
 *
 * <p>
 * The class is {@link Serializable} and {@link Cloneable} too (clones are
 * shallow and essentially behave as a view of the original instance). For
 * instances where more control over serialization is needed,
 * {@link #readFrom(ReadStream)} and {@link #writeTo(WriteStream)} methods are
 * available, though better performance may result from calling
 * {@link #toByteArray()} and managing the writing outside this class.
 *
 * <p>
 * Performance should be adequate for most uses; none of the methods operate in
 * anything more than linear time and inner loops are mostly 'tight'. Generally,
 * method implementations aim to avoid unnecessary intermediate object creation
 * and data copying.
 *
 * @author Tom Gibara
 */

public final class BitVector implements BitStore, Alignable<BitVector>, Cloneable, Serializable {

	// statics

	private static final int ADDRESS_BITS = 6;
	private static final int ADDRESS_SIZE = 1 << ADDRESS_BITS;
	private static final int ADDRESS_MASK = ADDRESS_SIZE - 1;

	private static final int SET = 0;
	private static final int AND = 1;
	private static final int OR  = 2;
	private static final int XOR = 3;

	private static final int EQUALS = 0;
	private static final int EXCLUDES = 1;
	private static final int CONTAINS = 2;
	private static final int COMPLEMENTS = 3;

	// static constructors

	/**
	 * Creates a {@link BitVector} from a <code>BigInteger</code>. The bits of
	 * the integer copied into the new bit vector. The size of the returned
	 * {@link BitVector} is <code>bigInt.bitLength()</code>. Negative values are
	 * recorded using 2's complement encoding. No sign-bit is included in the
	 * returned {@link BitVector}.
	 *
	 * @param bigInt
	 *            a big integer
	 * @return a {@link BitVector} initialized with the bits of the big integer
	 * @see #fromBigInteger(BigInteger, int)
	 * @see Bits#asStore(BigInteger)
	 */

	public static BitVector fromBigInteger(BigInteger bigInt) {
		if (bigInt == null) throw new IllegalArgumentException();
		final int length = bigInt.bitLength();
		return fromBigIntegerImpl(bigInt, length);
	}

	/**
	 * Creates a {@link BitVector} from a <code>BigInteger</code>. The bits of
	 * the integer copied into the new bit vector. The size of the returned
	 * {@link BitVector} is the specified <code>size</code>. If the size exceeds
	 * <code>bigInt.bitLength()</code> then the most significant bits are padded
	 * with ones if the integer is negative and zeros otherwise. Negative values
	 * are recorded using 2's complement encoding.
	 *
	 * @param bigInt
	 *            a big integer
	 * @param size
	 *            the size of the {@link BitVector} to create
	 * @return a {@link BitVector} initialized with the bits of the big integer
	 * @see #fromBigInteger(BigInteger)
	 * @see Bits#asStore(BigInteger)
	 */

	public static BitVector fromBigInteger(BigInteger bigInt, int size) {
		if (bigInt == null) throw new IllegalArgumentException();
		if (size < 0) throw new IllegalArgumentException();
		return fromBigIntegerImpl(bigInt, size);

	}

	/**
	 * Creates a {@link BitVector} from the bits of a byte array. The byte array
	 * is in big-endian order. If the specified size is less than the number of
	 * bits in the byte array, the most-significant bits are discarded. If the
	 * size exceeds the number of bits in the byte array, the most-significant
	 * bits of the {@link BitVector} are padded with zeros.
	 *
	 * @param bytes
	 *            the bits in big-endian order
	 * @param size
	 *            the size of the returned bit vector
	 * @return a bit vector containing the specified bits
	 * @see Bits#asStore(byte[])
	 */

	public static BitVector fromByteArray(byte[] bytes, int size) {
		//TODO provide a more efficient implementation, perhaps based on Bits.asStore()
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (size < 0) throw new IllegalArgumentException("negative size");
		BigInteger bigInt = new BigInteger(1, bytes);
		return fromBigIntegerImpl(bigInt, size);
	}

	private static BitVector fromBigIntegerImpl(BigInteger bigInt, int size) {
		int length = bigInt.bitLength();
		boolean neg = bigInt.signum() == -1;
		if (neg) bigInt = bigInt.negate().subtract(BigInteger.ONE);
		final BitVector vector = new BitVector(size);
		final long[] bits = vector.bits;
		long v = 0L;
		int i = 0;
		for (; i < size; i++) {
			if (i < length ? bigInt.testBit(i) != neg : neg) {
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

	/**
	 * Creates a {@link BitVector} by copying the bits in a <code>BitSet</code>.
	 * The size of the returned {@link BitVector} will equal the length of the
	 * supplied bitSet.
	 *
	 * @param bitSet
	 *            a <code>BitSet</code>
	 * @return a bit vector containing the bits of the bit set.
	 * @see #fromBitSet(BitSet, int)
	 */

	public static BitVector fromBitSet(BitSet bitSet) {
		if (bitSet == null) throw new IllegalArgumentException();
		final int length = bitSet.length();
		return fromBitSetImpl(bitSet, length, length);
	}

	/**
	 * Creates a {@link BitVector} by copying the bits in a <code>BitSet</code>.
	 *
	 * @param bitSet
	 *            a <code>BitSet</code>
	 * @param size
	 *            the size of {@link BitVector} to create, in bits
	 * @return a bit vector containing the bits of the bit set.
	 * @see Bits#asStore(BitSet, int)
	 */

	public static BitVector fromBitSet(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException();
		if (size < 0) throw new IllegalArgumentException();
		final int length = bitSet.length();
		return fromBitSetImpl(bitSet, size, length);
	}

	private static BitVector fromBitSetImpl(BitSet bitSet, int size, int length) {
		final BitVector vector = new BitVector(size);
		final long[] bits = vector.bits;
		long v = 0L;
		int i = 0;
		int limit = Math.min(size, length);
		for (; i < limit; i++) {
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

	/**
	 * Creates a {@link BitVector} by copying another {@link BitStore}.
	 *
	 * @param store
	 *            the bit store to be copied
	 * @return a new bit vector that equals the supplied store
	 */

	public static BitVector fromStore(BitStore store) {
		if (store instanceof BitVector) return ((BitVector)store).mutableCopy();
		if (store == null) throw new IllegalArgumentException("null store");
		return new BitVector(store);
	}

	// static utility methods

	//a, b not null a size not greater than b size
	private static int compareNumeric(BitVector a, BitVector b) {
		final int aSize = a.size();
		final int bSize = b.size();
		if (aSize != bSize && !b.isAllZerosAdj(b.finish - bSize + aSize, b.finish)) return -1;
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
			long aBits = a.getBitsAdj(aStart, ADDRESS_SIZE + offset);
			long bBits = b.getBitsAdj(bStart, ADDRESS_SIZE + offset);
			if (aBits == bBits) return 0;
			return aBits < bBits ? -1 : 1;
		}
	}

	//TODO should optimize
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

	// fields

	private final int start;
	private final int finish;
	private final long[] bits;
	private final boolean mutable;

	// public constructors

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
		readFrom(new CharBitReader(str));
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
			Arrays.fill(bits, -1L);
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

	// private constructors

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

	// fundamental methods

	@Override
	public int size() {
		return finish - start;
	}

	@Override
	public boolean getBit(int index) {
		index = adjIndex(index);
		final int i = index >> ADDRESS_BITS;
		final long m = 1L << (index & ADDRESS_MASK);
		return (bits[i] & m) != 0;
	}

	@Override
	public void setBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutable();
		final int i = index >> ADDRESS_BITS;
		final long m = 1L << (index & ADDRESS_MASK);
		if (value) {
			bits[i] |=  m;
		} else {
			bits[i] &= ~m;
		}
	}

	// accelerating methods

	@Override
	public long getBits(int position, int length) {
		checkBitsLength(length);
		return getBitsAdj(adjPosition(position, length), length);
	}

	@Override
	public int getBitsAsInt(int position, int length) {
		checkIntBitsLength(length);
		return (int) getBitsAdj(adjPosition(position, length), length);
	}

	// equivalent to xor().with(index, true)
	@Override
	public void flipBit(int index) {
		index = adjIndex(index);
		checkMutable();
		final int i = index >> ADDRESS_BITS;
		final long m = 1L << (index & ADDRESS_MASK);
		bits[i] ^=  m;
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutable();
		int i = index >> ADDRESS_BITS;
		long m = 1L << (index & ADDRESS_MASK);
		boolean previous = (bits[i] & m) != 0;
		if (previous != value) bits[i] ^= m;
		return previous;
	}

	@Override
	public void setBits(int position, long value, int length) {
		checkBitsLength(length);
		position = adjPosition(position);
		checkMutable();
		if (length != 0) setBitsImpl(position, value, length);
	}

	@Override
	public void setBitsAsInt(int position, int value, int length) {
		checkIntBitsLength(length);
		position = adjPosition(position);
		checkMutable();
		if (length != 0) setBitsImpl(position, value, length);
	}

	@Override
	public void setStore(int position, BitStore store) {
		if (store instanceof BitVector) {
			perform(SET, position, (BitVector) store);
		} else {
//			store.writeTo(openWriter(SET, size() - position - store.size()));
//			store.writeTo(openWriter(SET, size() - position - store.size(), size() - position));
			store.writeTo(openWriter(SET, position, position + store.size()));
		}
	}

	@Override
	public void fill() {
		checkMutable();
		performAdjSet(start, finish);
	}

	@Override
	public void clear() {
		checkMutable();
		performAdjClear(start, finish);
	}

	//named flip for consistency with BigInteger and BitSet
	@Override
	public void flip() {
		checkMutable();
		performAdjXor(start, finish);
	}

	// operations

	@Override
	public Op set() {
		return new SetOp();
	}

	@Override
	public Op and() {
		return new AndOp();
	}

	@Override
	public Op or() {
		return new OrOp();
	}

	@Override
	public Op xor() {
		return new XorOp();
	}

	// shifting

	@Override
	public void shift(int distance, boolean fill) {
		shiftAdj(start, finish, distance, fill);
	}

	// matching

	@Override
	public BitMatches ones() {
		return new MatchesOnes();
	}

	@Override
	public BitMatches zeros() {
		return new MatchesZeros();
	}

	@Override
	public Tests equals() {
		return new VectorTests(EQUALS);
	}

	@Override
	public Tests contains() {
		return new VectorTests(CONTAINS);
	}

	@Override
	public Tests excludes() {
		return new VectorTests(EXCLUDES);
	}

	@Override
	public Tests complements() {
		return new VectorTests(COMPLEMENTS);
	}

	// I/O

	@Override
	public BitWriter openWriter(int finalPos, int initialPos) {
		return openWriter(SET, finalPos, initialPos);
	}

	@Override
	public BitReader openReader(int finalPos, int initialPos) {
		return new VectorReader( adjPosition(finalPos), adjPosition(initialPos) );
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
	public void writeTo(WriteStream writer) {
		//TODO could actually optimize under weaker condition that start is byte aligned
		if ((start & ADDRESS_MASK) == 0L) {
			int head = finish & ADDRESS_MASK;
			int to = finish >> ADDRESS_BITS;
			if (head != 0L) {
				long mask = -1L >>> (ADDRESS_SIZE - head);
				LongBitStore.writeBits(writer, bits[to] & mask, head);
			}
			int from = start >> ADDRESS_BITS;
			for (int i = to - 1; i >= from; i--) {
				writer.writeLong(bits[i]);
			}
		} else {
			//TODO this can be optimized by doing the byte breakdown internally
			BitStore.super.writeTo(writer);
		}
	}

	@Override
	public void readFrom(ReadStream reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		checkMutable();
		//TODO could actually optimize under weaker condition that start is byte aligned
		if ((start & ADDRESS_MASK) == 0L) {
			int head = finish & ADDRESS_MASK;
			int to = finish >> ADDRESS_BITS;
			if (head != 0L) {
				long mask = -1L << head;
				bits[to] = (bits[to] & mask) | (LongBitStore.readBits(reader, head) & ~mask);
			}
			int from = start >> ADDRESS_BITS;
			for (int i = to - 1; i >= from; i--) {
				bits[i] = reader.readLong();
			}
		} else {
			//TODO what other optimizations?
			BitStore.super.readFrom(reader);
		}
	}

	// views

	// returns a new bitvector that is backed by the same data as this one
	// equivalent to: duplicate(from, to, false, isMutable());
	// bypasses duplicate for efficiency
	@Override
	public BitVector range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BitVector(from, to, bits, mutable);
	}

	@Override
	public Permutes permute() {
		checkMutable();
		return new VectorPermutes();
	}

	@Override
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
				bytes[length - 1 - i] =  (byte) getBitsAdj(start + (i << 3), 8);
			}
			bytes[0] = (byte) getBitsAdj(start + (i << 3), size - (i << 3));
		}
		return bytes;
	}

	@Override
	public BitSet toBitSet() {
		final int size = finish - start;
		final BitSet bitSet = new BitSet(size);
		for (int i = 0; i < size; i++) {
			bitSet.set(i, getBitAdj(i + start));
		}
		return bitSet;
	}

	@Override
	public Number asNumber() {
		return new VectorNumber();
	}

	public List<Boolean> asList() {
		return new VectorList();
	}

	// mutability methods

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

	// comparable methods

	@Override
	public int compareNumericallyTo(BitStore that) {
		if (that instanceof BitVector) {
			if (this == that) return 0; // cheap check
			BitVector v = (BitVector) that;
			return this.size() < that.size() ? compareNumeric(this, v) : -compareNumeric(v, this);
		}
		return BitStore.super.compareNumericallyTo(that);
	}

	@Override
	public int compareLexicallyTo(BitStore that) {
		if (that instanceof BitVector) {
			if (this == that) return 0; // cheap check
			return compareLexical(this, (BitVector) that);
		}
		return BitStore.super.compareLexicallyTo(that);
	}

	// convenience methods

	@Override
	public void setAll(boolean value) {
		checkMutable();
		if (value) {
			performAdjSet(start, finish);
		} else {
			performAdjClear(start, finish);
		}
	}

	@Override
	public Tests test(Test test) {
		if (test == null) throw new IllegalArgumentException("null test");
		return new VectorTests(test.ordinal());
	}

	@Override
	public BitWriter openWriter() {
		return new VectorWriter();
	}

	@Override
	public BitReader openReader() {
		return new VectorReader();
	}

	@Override
	public BitVector rangeFrom(int from) {
		return new BitVector(adjPosition(from), finish, bits, mutable);
	}

	@Override
	public BitVector rangeTo(int to) {
		return new BitVector(start, adjPosition(to), bits, mutable);
	}

	// alignment methods

	@Override
	public boolean isAligned() {
		return start == 0;
	}

	//TODO consider adding a trimmed copy, or guarantee this is trimmed?
	//only creates a new bit vector if necessary
	@Override
	public BitVector aligned() {
		return start == 0 ? this : getVectorAdj(start, finish - start, mutable);
	}

	@Override
	public BitVector alignedCopy() {
		return getVectorAdj(start, finish - start, mutable);
	}

	// bit vector specific methods

	/**
	 * Duplicates the {@link BitVector}.
	 *
	 * @param copy
	 *            true if the duplicate should be a detached copy, false if it
	 *            should be a view, backed by the same bit data.
	 * @param mutable
	 *            whether the duplicate should be mutable
	 * @return a duplicate of this BitVector
	 * @see #duplicateRange(int, int, boolean, boolean)
	 */

	public BitVector duplicate(boolean copy, boolean mutable) {
		if (mutable && !copy && !this.mutable) throw new IllegalStateException("Cannot obtain mutable view of an immutable BitVector");
		return duplicateAdj(start, finish, copy, mutable);
	}

	/**
	 * Duplicates a range of the {@link BitVector}.
	 *
	 * @param from
	 *            the (inclusive) position at which the range begins
	 * @param to
	 *            the (exclusive) position at which the range ends
	 * @param copy
	 *            true if the duplicated range should be a detached copy, false
	 *            if it should be a view, backed by the same bit data.
	 * @param mutable
	 *            whether the duplicated range should be mutable
	 * @return a duplicate of this BitVector
	 * @see #duplicate(boolean, boolean)
	 */

	public BitVector duplicateRange(int from, int to, boolean copy, boolean mutable) {
		if (mutable && !copy && !this.mutable) throw new IllegalStateException("Cannot obtain mutable view of an immutable BitVector");
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return duplicateAdj(from, to, copy, mutable);
	}

	/**
	 * Creates a mutable copy of this {@link BitVector} that may have a
	 * different size.
	 *
	 * @param newSize
	 *            the size of the returned {@link BitVector}
	 * @param anchorLeft
	 *            true if the most-significant bit of this {@link BitVector}
	 *            remains the most-significant bit of the returned
	 *            {@link BitVector}, false if the least-significant bit of this
	 *            {@link BitVector} remains the least-significant bit of the
	 *            returned {@link BitVector}.
	 * @return a resized mutable copy of this {@link BitVector}
	 */

	public BitVector resizedCopy(int newSize, boolean anchorLeft) {
		if (newSize < 0) throw new IllegalArgumentException();
		final int size = finish - start;
		if (newSize == size) return duplicate(true, true);
		int from;
		int to;
		if (anchorLeft) {
			from = size - newSize;
			to = size;
		} else {
			from = 0;
			to = newSize;
		}
		if (newSize < size) return new BitVector(start + from, start + to, bits, mutable).duplicate(true, true);
		final BitVector copy = new BitVector(newSize);
		copy.perform(SET, -from, this);
		return copy;
	}

	// object methods

	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof BitVector that) {
            if (this.finish - this.start != that.finish - that.start) return false;
			return test(EQUALS, that);
		}
		if (obj instanceof BitStore store) {
            if (this.size() != store.size()) return false;
			return equals().store(store);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Bits.bitStoreHasher().hash(this).intValue();
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

	//shallow, externally identical to calling view();
	@Override
	public BitVector clone() {
		try {
			return (BitVector) super.clone();
		} catch (CloneNotSupportedException e) {
			//should never occur
			throw new RuntimeException("Clone failure!", e);
		}
	}

	// serialization

	@java.io.Serial
	private Object writeReplace() throws ObjectStreamException {
		return new Serial(this);
	}

	// package preserved methods

	//NOTE: preserved for performance testing
	void clear(int from, int to, boolean value) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		checkMutable();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		if (value) {
			performAdjSet(from, to);
		} else {
			performAdjClear(from, to);
		}
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

	//NOTE: uncertain future
	int[] toIntArray() {
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
				ints[length - 1 - i] = (int) getBitsAdj(start + (i << 5), 32);
			}
			ints[0] = (int) getBitsAdj(start + (i << 5), size - (i << 5));
		}
		return ints;
	}

	//NOTE: uncertain future
	long[] toLongArray() {
		// create array through an aligned copy
		BitVector copy = alignedCopy();
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

	//NOTE: preserved for performance testing
	void writeTo(OutputStream out) throws IOException {
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

	//NOTE: preserved for performance testing
	void readFrom(InputStream in) throws IOException {
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

	// private utility methods

	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}

	private int adjIndex(int index) {
		if (index < 0) throw new IllegalArgumentException("negative index");
		index += start;
		if (index >= finish) throw new IllegalArgumentException("index beyond highest bit");
		return index;
	}

	private int adjPosition(int position) {
		if (position < 0) throw new IllegalArgumentException("negative position");
		position += start;
		if (position > finish) throw new IllegalArgumentException("position exceeds size");
		return position;
	}

	private int adjPosition(int position, int length) {
		if (position < 0) throw new IllegalArgumentException();
		position += start;
		if (position + length > finish) throw new IllegalArgumentException();
		return position;
	}

	private void perform(int operation, int index, boolean value) {
		performAdj(operation, adjIndex(index), value);
	}

	private boolean getThenPerform(int operation, int index, boolean value) {
		return getThenPerformAdj(operation, adjIndex(index), value);
	}

	private void performAdj(int operation, int from, int to, boolean value) {
		if (from == to) return; // nothing to do for an empty vector
		//rationalize possible operations into SETs or INVERTs
		switch (operation) {
		case AND : if (!value) performAdjClear(from, to); return;
		case OR  : if ( value) performAdjSet(from, to); return;
		case XOR : if ( value) performAdjXor(from, to); return;
		case SET : if ( value) performAdjSet(from, to); else performAdjClear(from, to);
		}
	}

	private void performAdjSet(int from, int to) {
		if (from == to) return; // nothing to do for an empty vector
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;
		final long fm = -1L << (from - f * ADDRESS_SIZE);
		final long tm = -1L >>> (t * ADDRESS_SIZE - to);

		if (f == t) { // change falls into one element
			bits[f] |= fm & tm;
			return;
		}

		//process intermediate elements
		Arrays.fill(bits, f+1, t, -1L);

		//process terminals
		bits[f] |= fm;
		bits[t] |= tm;
	}

	private void performAdjClear(int from, int to) {
		if (from == to) return; // nothing to do for an empty vector
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;
		final long fm = -1L << (from - f * ADDRESS_SIZE);
		final long tm = -1L >>> (t * ADDRESS_SIZE - to);

		if (f == t) { // change falls into one element
			bits[f] &= ~(fm & tm);
			return;
		}

		//process intermediate elements
		Arrays.fill(bits, f+1, t, 0L);

		//process terminals
		bits[f] &= ~fm;
		bits[t] &= ~tm;
	}

	private void performAdjXor(int from, int to) {
		if (from == to) return; // nothing to do for an empty vector
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;
		final long fm = -1L << (from - f * ADDRESS_SIZE);
		final long tm = -1L >>> (t * ADDRESS_SIZE - to);

		if (f == t) { // change falls into one element
			bits[f] ^= fm & tm;
			return;
		}

		//process intermediate elements
		for (int i = f+1; i < t; i++) bits[i] = ~bits[i];

		//process terminals
		bits[f] ^= fm;
		bits[t] ^= tm;
	}

	//assumes address size is size of long
	private void perform(int operation, int position, long bs, int length) {
		checkBitsLength(length);
		position = adjPosition(position);
		checkMutable();
		performAdj(operation, position, bs, length);
	}

	private void performAdj(int operation, int position, long bs, int length) {
		if (length == 0) return;

		int i = position >> ADDRESS_BITS;
		int s = position & ADDRESS_MASK;
		long m = length == ADDRESS_SIZE ? -1L : (1L << length) - 1L;
		long v = bs & m;

		switch(operation) {
		case SET : performAdjSet(length, i, s, m, v); return;
		case AND : performAdjAnd(length, i, s, m, v); return;
		case OR  : performAdjOr (length, i, s, m, v); return;
		case XOR : performAdjXor(length, i, s, m, v); return;
		}
	}

	private void performAdjSet(int length, int i, int s, long m, long v) {
		if (s == 0) { // fast case, long-aligned
			bits[i] = bits[i] & ~m | v;
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			bits[i] = bits[i] & Long.rotateLeft(~m, s) | (v << s);
		} else {
			bits[i  ] = bits[i  ] & (-1L >>> (         ADDRESS_SIZE - s)) | (v <<                  s );
			bits[i+1] = bits[i+1] & (-1L <<  (length - ADDRESS_SIZE + s)) | (v >>> (ADDRESS_SIZE - s));
		}
	}

	private void performAdjAnd(int length, int i, int s, long m, long v) {
		if (s == 0) { // fast case, long-aligned
			bits[i] &= v | ~m;
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			bits[i] &= (v << s) | Long.rotateLeft(~m, s);
		} else {
			bits[i  ]  &= (v <<                  s ) | (-1L >>> (         ADDRESS_SIZE - s));
			bits[i+1]  &= (v >>> (ADDRESS_SIZE - s)) | (-1L <<  (length - ADDRESS_SIZE + s));
		}
	}

	private void performAdjOr(int length, int i, int s, long m, long v) {
		if (s == 0) { // fast case, long-aligned
			bits[i] |= v;
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			bits[i] |= v << s;
		} else {
			bits[i  ]  |= (v <<                  s );
			bits[i+1]  |= (v >>> (ADDRESS_SIZE - s));
		}
	}

	private void performAdjXor(int length, int i, int s, long m, long v) {
		if (s == 0) { // fast case, long-aligned
			bits[i] ^= v;
		} else if (s + length <= ADDRESS_SIZE) { //single long case
			bits[i] ^= v << s;
		} else {
			bits[i  ]  ^= (v <<                  s );
			bits[i+1]  ^= (v >>> (ADDRESS_SIZE - s));
		}
	}

	private void perform(int operation, int position, BitVector that) {
		if (that == null) throw new IllegalArgumentException("null vector");
		position = adjPosition(position);
		checkMutable();
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
		checkMutable();
		performAdj(operation, adjPosition(position), store);
	}

	private void perform(int operation, int position, byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length == 0) return;
		if (offset + length > (bytes.length << 3)) throw new IllegalArgumentException("length greater than number of bits in byte array");
		performAdj(operation, adjPosition(position), bytes, offset, length);
	}

	private BitVector duplicateAdj(int from, int to, boolean copy, boolean mutable) {
		if (copy) {
			int f = from >> ADDRESS_BITS;
			int t = (to + ADDRESS_MASK) >> ADDRESS_BITS;
			from &= ADDRESS_MASK;
			to -= f << ADDRESS_BITS;
			return new BitVector(from, to, Arrays.copyOfRange(bits, f, t), mutable);
		} else {
			return new BitVector(from, to, bits, mutable);
		}
	}

	private boolean test(final int test, final BitVector that) {
		//trivial case
		if (this.start == this.finish) return true;
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
			case EXCLUDES :
				for (int i = t-1; i >= 0; i--) {
					if ((thisBits[i] & thatBits[i]) != 0) return false;
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
                return switch (test) {
                    case EQUALS -> thisB == thatB;
                    case EXCLUDES -> (thisB & thatB) == 0;
                    case CONTAINS -> (thisB | thatB) == thisB;
                    case COMPLEMENTS -> (~thisB & m) == thatB;
                    default -> throw new IllegalArgumentException("Unexpected comparison constant: " + test);
                };
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
            return switch (test) {
                case EQUALS -> {
                    for (int i = f; i < t; i++) {
                        if (thisBits[i] != thatBits[i + d]) yield false;
                    }
                    yield true;
                }
                case EXCLUDES -> {
                    for (int i = f; i < t; i++) {
                        if ((thisBits[i] & thatBits[i + d]) == 0) yield true;
                    }
                    yield true;
                }
                case CONTAINS -> {
                    for (int i = f; i < t; i++) {
                        final long bits = thisBits[i];
                        if ((bits | thatBits[i + d]) != bits) yield false;
                    }
                    yield true;
                }
                case COMPLEMENTS -> {
                    for (int i = f; i < t; i++) {
                        if (~thisBits[i] != thatBits[i + d]) yield false;
                    }
                    yield true;
                }
                default -> throw new IllegalArgumentException("Unexpected comparison constant: " + test);
            };
		}
		//non-optimized case
		//TODO could be optimized with long comparisions
		final int size = finish - start;
        return switch (test) {
            case EQUALS -> {
                for (int i = 0; i < size; i++) {
                    if (that.getBitAdj(that.start + i) != this.getBitAdj(this.start + i)) yield false;
                }
                yield true;
            }
            case EXCLUDES -> {
                for (int i = 0; i < size; i++) {
                    if (that.getBitAdj(that.start + i) && this.getBitAdj(this.start + i)) yield false;
                }
                yield true;
            }
            case CONTAINS -> {
                for (int i = 0; i < size; i++) {
                    if (that.getBitAdj(that.start + i) && !this.getBitAdj(this.start + i)) yield false;
                }
                yield true;
            }
            case COMPLEMENTS -> {
                for (int i = 0; i < size; i++) {
                    if (that.getBitAdj(that.start + i) == this.getBitAdj(this.start + i)) yield false;
                }
                yield true;
            }
            default -> throw new IllegalArgumentException("Unexpected comparison constant: " + test);
        };
	}

	// size cannot exceed 64
	private boolean test(int test, long bits, int size) {
		switch (size) {
		case 0 : return true;
		case 1 : return test(test, getBitAdj(start), (bits & 1L) != 0L);
		case 64: return test(test, getBitsAdj(0, 64), bits);
		default :
			long m = -1L << size;
			long value = getBitsAdj(0, size);
			if (test == COMPLEMENTS) {
				bits |= m;
			} else {
				bits &= ~m;
			}
			return test(test, value, bits);
		}
	}

	private boolean test(int test, boolean a, boolean b) {
		return switch (test) {
		case EQUALS      ->   a  ==  b ;
		case EXCLUDES    -> !(a  &&  b);
		case CONTAINS    ->   a  || !b ;
		case COMPLEMENTS ->   a  !=  b ;
		default  -> throw new IllegalArgumentException("Unexpected comparison constant: " + test);
		};
	}

	private boolean test(int test, long a, long b) {
        return switch (test) {
            case EQUALS      -> a == b;
            case EXCLUDES    -> (a & b) == 0L;
            case CONTAINS    -> (a | ~b) == -1L;
            case COMPLEMENTS -> a != b;
            default -> throw new IllegalArgumentException("Unexpected comparison constant: " + test);
        };
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

	// length guaranteed to be non-zero
	private void setBitsImpl(int position, long value, int length) {
		int i = position >> ADDRESS_BITS;
		int s = position & ADDRESS_MASK;
		long m = length == ADDRESS_SIZE ? -1L : (1L << length) - 1L;
		long v = value & m;
		performAdjSet(length, i, s, m, v);
	}

	private BitVector getVectorAdj(int position, int length, boolean mutable) {
		final long[] newBits;
		if (length == 0) {
			newBits = new long[0];
		} else {
			final int from = position >> ADDRESS_BITS;
			final int to = (position + length + ADDRESS_MASK) >> ADDRESS_BITS;
			if ((position & ADDRESS_MASK) == 0) {
				newBits = Arrays.copyOfRange(bits, from, to);
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

	private boolean isAllOnesAdj(int from, int to) {
		if (from == to) return true;
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;

		final long fm;
		final long tm;
		{
			final int fs = from & ADDRESS_MASK;
			final int ts = to & ADDRESS_MASK;
			fm = fs == 0 ? 0 : -1L >>> (ADDRESS_SIZE - fs);
			tm = ts == 0 ? 0 : -1L << ts;
		}

		if (f == t) { // bits fit into a single element
			return (bits[f] | fm | tm) == -1L;
		}

		//check intermediate elements
		for (int i = f+1; i < t; i++) if (bits[i] != -1L) return false;

		//check terminals
		return (bits[f] | fm) == -1L && (bits[t] | tm) == -1L;
	}

	private boolean isAllZerosAdj(int from, int to) {
		if (from == to) return true;
		final int f = from >> ADDRESS_BITS;
		final int t = (to-1) >> ADDRESS_BITS;

		final long fm;
		final long tm;
		{
			final int fs = from & ADDRESS_MASK;
			final int ts = to & ADDRESS_MASK;
			fm = -1L << fs;
			tm = -1L >>> (ADDRESS_SIZE - ts);
		}

		if (f == t) { // bits fit into a single element
			return (bits[f] & fm & tm) == 0L;
		}

		//check intermediate elements
		for (int i = f+1; i < t; i++) if (bits[i] != 0L) return false;

		//check terminals
		return (bits[f] & fm) == 0L && (bits[t] & tm) == 0L;
	}

	private void performAdj(int operation, int index, boolean value) {
		checkMutable();
		final int i = index >> ADDRESS_BITS;
		final long m = 1L << (index & ADDRESS_MASK);
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
		checkMutable();
		final int to = (bytes.length << 3) - offset;
		final int from = to - length;
		position += length;
		for (int i = from; i < to; i++) {
			boolean b = (bytes[i >> 3] & (128 >> (i & 7))) != 0;
			performAdj(operation, --position, b);
		}
	}

	private void performAdj(int operation, int position, BitVector that) {
		final int thatSize = that.size();
		if (thatSize == 0) return;
		if (this.bits == that.bits && overlapping(position, position + thatSize, that.start, that.finish)) that = that.immutableCopy();
		if (thatSize <= ADDRESS_SIZE) {
			performAdj(operation, position, that.getBitsAdj(that.start, thatSize), thatSize);
		} else {
			// TODO would like not to depend on writer here
			// a direct implementation would be faster
			that.writeTo( new VectorWriter(operation, position, position + thatSize) );
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
			store.writeTo( new VectorWriter(operation, position, position + storeSize) );
		}
	}

	//specialized implementation for the common case of setting an individual bit

	private void performSetAdj(int position, boolean value) {
		checkMutable();
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
		checkMutable();
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
		final int cycles = Bits.gcd(distance, length);
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
		checkMutable();
		if (from == to) return;
		if (distance == 0) return;

		//TODO have separate methods for true/false fill?
		//TODO this capable of optimization in some cases
		if (distance > 0) {
			int j = to - 1;
			for (int i = j - distance; i >= from; i--, j--) {
				performSetAdj(j, getBitAdj(i));
			}
			if (fill) {
				performAdjSet(from, j + 1);
			} else {
				performAdjClear(from, j + 1);
			}
		} else {
			int j = from;
			for (int i = j - distance; i < to; i++, j++) {
				performSetAdj(j, getBitAdj(i));
			}
			if (fill) {
				performAdjSet(j, to);
			} else {
				performAdjClear(j, to);
			}
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
		checkMutable();
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
		if (size > 0) {
			if (ones > 0) {
				performAdjSet(to - size, to);
			} else {
				performAdjClear(to - size, to);
			}
		}
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

	private BitWriter openWriter(int operation, int finalPos, int initialPos) {
		if (finalPos < 0) throw new IllegalArgumentException("negative finalPos");
		finalPos += start;
		initialPos += start;
		if (initialPos < finalPos) throw new IllegalArgumentException("finalPos exceeds initialPos");
		if (initialPos > finish) throw new IllegalArgumentException("initialPos too large");
		return new VectorWriter(operation, finalPos, initialPos);
	}

	// collection methods

	private IntSet asSet(boolean bit, int offset) {
		return new IntSet(bit, offset);
	}

	private ListIterator<Boolean> listIterator() {
		return new VectorIterator();
	}

	private ListIterator<Boolean> listIterator(int position) {
		return new VectorIterator(adjPosition(position));
	}

	// inner classes

	private final class VectorNumber extends Number {

		@java.io.Serial
		private static final long serialVersionUID = 2471332225370258558L;

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
			return toBigInteger().floatValue();
		}

		@Override
		public double doubleValue() {
			return toBigInteger().doubleValue();
		}

	}

	private final class SetOp implements Op {

		@Override
		public Operation getOperation() {
			return Operation.SET;
		}

		@Override
		public void with(boolean value) {
			checkMutable();
			performAdj(SET, start, finish, value);
		}

		@Override
		public void withBit(int position, boolean value) {
			perform(SET, position, value);
		}

		@Override
		public boolean getThenWithBit(int position, boolean value) {
			return getThenPerform(SET, position, value);
		}

		@Override
		public void withByte(int position, byte value) {
			perform(SET, position, value, 8);
		}

		@Override
		public void withShort(int position, short value) {
			perform(SET, position, value, 16);
		}

		@Override
		public void withInt(int position, int value) {
			perform(SET, position, value, 32);
		}

		@Override
		public void withLong(int position, long value) {
			perform(SET, position, value, 64);
		}

		@Override
		public void withBits(int position, long value, int length) {
			perform(SET, position, value, length);
		}

		@Override
		public void withStore(BitStore store) {
			perform(SET, store);
		}

		@Override
		public void withStore(int position, BitStore store) {
			perform(SET, position, store);
		}

		@Override
		public void withBytes(int position, byte[] bytes, int offset, int length) {
			perform(SET, position, bytes, offset, length);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			return BitVector.this.openWriter(SET, finalPos, initialPos);
		}
	}

	private final class AndOp implements Op {

		@Override
		public Operation getOperation() {
			return Operation.AND;
		}

		@Override
		public void with(boolean value) {
			checkMutable();
			performAdj(AND, start, finish, value);
		}

		@Override
		public void withBit(int position, boolean value) {
			perform(AND, position, value);
		}

		@Override
		public boolean getThenWithBit(int position, boolean value) {
			return getThenPerform(AND, position, value);
		}

		@Override
		public void withByte(int position, byte value) {
			perform(AND, position, value, 8);
		}

		@Override
		public void withShort(int position, short value) {
			perform(AND, position, value, 16);
		}

		@Override
		public void withInt(int position, int value) {
			perform(AND, position, value, 32);
		}

		@Override
		public void withLong(int position, long value) {
			perform(AND, position, value, 64);
		}

		@Override
		public void withBits(int position, long value, int length) {
			perform(AND, position, value, length);
		}

		@Override
		public void withStore(BitStore store) {
			perform(AND, store);
		}

		@Override
		public void withStore(int position, BitStore store) {
			perform(AND, position, store);
		}

		@Override
		public void withBytes(int position, byte[] bytes, int offset, int length) {
			perform(AND, position, bytes, offset, length);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			return BitVector.this.openWriter(AND, finalPos, initialPos);
		}
	}

	private final class OrOp implements Op {

		@Override
		public Operation getOperation() {
			return Operation.OR;
		}

		@Override
		public void with(boolean value) {
			checkMutable();
			performAdj(OR, start, finish, value);
		}

		@Override
		public void withBit(int position, boolean value) {
			perform(OR, position, value);
		}

		@Override
		public boolean getThenWithBit(int position, boolean value) {
			return getThenPerform(OR, position, value);
		}

		@Override
		public void withByte(int position, byte value) {
			perform(OR, position, value, 8);
		}

		@Override
		public void withShort(int position, short value) {
			perform(OR, position, value, 16);
		}

		@Override
		public void withInt(int position, int value) {
			perform(OR, position, value, 32);
		}

		@Override
		public void withLong(int position, long value) {
			perform(OR, position, value, 64);
		}

		@Override
		public void withBits(int position, long value, int length) {
			perform(OR, position, value, length);
		}

		@Override
		public void withStore(BitStore store) {
			perform(OR, store);
		}

		@Override
		public void withStore(int position, BitStore store) {
			perform(OR, position, store);
		}

		@Override
		public void withBytes(int position, byte[] bytes, int offset, int length) {
			perform(OR, position, bytes, offset, length);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			return BitVector.this.openWriter(OR, finalPos, initialPos);
		}
	}

	private final class XorOp implements Op {

		@Override
		public Operation getOperation() {
			return Operation.XOR;
		}

		@Override
		public void with(boolean value) {
			checkMutable();
			performAdj(XOR, start, finish, value);
		}

		@Override
		public void withBit(int position, boolean value) {
			perform(XOR, position, value);
		}

		@Override
		public boolean getThenWithBit(int position, boolean value) {
			return getThenPerform(XOR, position, value);
		}

		@Override
		public void withByte(int position, byte value) {
			perform(XOR, position, value, 8);
		}

		@Override
		public void withShort(int position, short value) {
			perform(XOR, position, value, 16);
		}

		@Override
		public void withInt(int position, int value) {
			perform(XOR, position, value, 32);
		}

		@Override
		public void withLong(int position, long value) {
			perform(XOR, position, value, 64);
		}

		@Override
		public void withBits(int position, long value, int length) {
			perform(XOR, position, value, length);
		}

		@Override
		public void withStore(BitStore store) {
			perform(XOR, store);
		}

		@Override
		public void withStore(int position, BitStore store) {
			perform(XOR, position, store);
		}

		@Override
		public void withBytes(int position, byte[] bytes, int offset, int length) {
			perform(XOR, position, bytes, offset, length);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			return BitVector.this.openWriter(XOR, finalPos, initialPos);
		}
	}

	private final class MatchesOnes extends AbstractBitMatches {

		@Override
		public boolean bit() {
			return true;
		}

		@Override
		public ImmutableOne sequence() {
			return ImmutableOne.INSTANCE;
		}

		@Override
		public BitMatches range(int from, int to) {
			return BitVector.this.range(from, to).ones();
		}

		@Override
		public BitVector store() {
			return BitVector.this;
		}

		@Override
		public boolean isAll() {
			return isAllOnesAdj(start, finish);
		}

		@Override
		public boolean isNone() {
			return isAllZerosAdj(start, finish);
		}

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

		public Positions positions() {
			return new PositionIterator(true);
		}

		public Positions positions(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position > finish) throw new IllegalArgumentException();
			return new PositionIterator(true, position);
		}

		@Override
		public SortedSet<Integer> asSet() {
			return new IntSet(true, start);
		}
	}

	private final class MatchesZeros extends AbstractBitMatches {

		@Override
		public boolean bit() {
			return false;
		}

		@Override
		public ImmutableZero sequence() {
			return ImmutableZero.INSTANCE;
		}

		@Override
		public BitVector store() {
			return BitVector.this;
		}

		@Override
		public BitMatches range(int from, int to) {
			return BitVector.this.range(from, to).zeros();
		}

		@Override
		public boolean isAll() {
			return isAllZerosAdj(start, finish);
		}

		@Override
		public boolean isNone() {
			return isAllOnesAdj(start, finish);
		}

		@Override
		public int count() {
			return finish - start - countOnesAdj(start, finish);
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

		@Override
		public Positions positions() {
			return new PositionIterator(false);
		}

		@Override
		public Positions positions(int position) {
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position > finish) throw new IllegalArgumentException();
			return new PositionIterator(false, position);
		}

		@Override
		public SortedSet<Integer> asSet() {
			return new IntSet(false, start);
		}

	}

	private final class VectorTests implements Tests {

		private final int test;

		VectorTests(int test) {
			this.test = test;
		}

		public Test getTest() {
			return Test.values[test];
		}

		public boolean store(BitStore store) {
			if (store instanceof BitVector vector) {
                if (finish - start != vector.finish - vector.start) throw new IllegalArgumentException();
				return test(test, vector);
			}
			//TODO needs to be optimized
			switch (test) {
			case EQUALS : return new BitStoreTests.Equals(BitVector.this).store(store);
			case EXCLUDES : return new BitStoreTests.Excludes(BitVector.this).store(store);
			case CONTAINS : return new BitStoreTests.Contains(BitVector.this).store(store);
			case COMPLEMENTS : new BitStoreTests.Complements(BitVector.this).store(store);
			default : throw new IllegalStateException();
			}
		}

		public boolean bits(long bits) {
			int size = finish - start;
			if (size > 64) throw new IllegalArgumentException("size exceeds 64 bits");
			return test(test, bits, size);
		}

	}

	//TODO make public and expose more efficient methods?
	private final class VectorIterator implements ListIterator<Boolean> {

		private final int from;
		private final int to;
		// points to the element that will be returned  by next
		private int index;
		private int recent = -1;

		VectorIterator(int from, int to, int index) {
			this.from = from;
			this.to = to;
			this.index = index;
		}

		VectorIterator(int index) {
			this(start, finish, index);
		}

		VectorIterator() {
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
			return getBitAdj(index++);
		}

		@Override
		public int nextIndex() {
			return hasNext() ? index - start : finish - start;
		}

		@Override
		public boolean hasPrevious() {
			return index > from;
		}

		@Override
		public Boolean previous() {
			if (!hasPrevious()) throw new NoSuchElementException();
			recent = --index;
			return getBitAdj(recent);
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

	private final class VectorPermutes implements Permutes {

		@Override
		public void transpose(int i, int j) {
		}

		@Override
		public void rotate(int distance) {
			rotateAdj(start, finish, distance);
		}

		@Override
		public void reverse() {
			reverseAdj(start, finish);
		}

		@Override
		public void shuffle(Random random) {
			if (random == null) throw new IllegalArgumentException("null random");
			shuffleAdj(start, finish, random);
		}

	}

	private final class PositionIterator implements Positions {

		private static final int NOT_SET = Integer.MIN_VALUE;

		private final boolean bit;
		private final int from;
		private final int to;
		private int previous;
		private int next;
		private int nextIndex;
		private int recent = NOT_SET;

		PositionIterator(boolean bit, int from, int to, int position) {
			this.bit = bit;
			this.from = from;
			this.to = to;
			previous = lastInRange(from, position);
			next = firstInRange(position, to);
			nextIndex = previous == -1 ? 0 : NOT_SET;
		}

		PositionIterator(boolean bit, int index) {
			this(bit, start, finish, index);
		}

		PositionIterator(boolean bit) {
			this(bit, start, finish, start);
		}

		@Override
		public boolean isDisjoint() {
			return true;
		}

		@Override
		public boolean hasPrevious() {
			return previous != start - 1;
		}

		@Override
		public boolean hasNext() {
			return next != to;
		}

		@Override
		public int previousPosition() {
			if (previous == start - 1) return -1;
			recent = previous;
			next = recent;
			previous = lastInRange(from, recent);
			if (nextIndex != NOT_SET) nextIndex--;
			return next - start;
		}

		@Override
		public Integer previous() {
			int position = previousPosition();
			if (position == -1) throw new NoSuchElementException();
			return position;
		}

		@Override
		public int nextPosition() {
			if (next == to) return to - start;
			recent = next;
			previous = recent;
			next = firstInRange(recent + 1, to);
			if (nextIndex != NOT_SET) nextIndex++;
			return previous - start;
		}

		@Override
		public void replace(BitStore replacement) {
			if (replacement == null) throw new IllegalArgumentException("null replacement");
			if (replacement.size() != 1) throw new IllegalArgumentException("replacement size does not match sequence size");
			if (recent == NOT_SET) throw new IllegalStateException("no position");
			if (replacement.getBit(0) != bit) flipBit(recent);
		}

		@Override
		public void replace(boolean bits) {
			if (recent == NOT_SET) throw new IllegalStateException("no position");
			if (bits != bit) flipBit(recent);
		}

		@Override
		public Integer next() {
			int position = nextPosition();
			if (position == to - start) throw new NoSuchElementException();
			return position;
		}

		@Override
		public int previousIndex() {
			return nextIndex() - 1;
		}

		@Override
		public int nextIndex() {
			return nextIndex == NOT_SET ? nextIndex = countInRange(from, next) : nextIndex;
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
			boolean changed = bit != getThenPerformAdj(SET, i, bit);
			if (changed) {
				if (nextIndex != NOT_SET) nextIndex ++;
				previous = i;
			}
		}

		private void doRemove() {
			if (recent == previous) { // we went forward
				previous = lastInRange(from, recent);
				if (nextIndex != NOT_SET) nextIndex --;
			} else if (recent == next) { // we went backwards
				next = firstInRange(recent + 1, to);
			} else { // no recent value
				throw new IllegalStateException();
			}
			performAdj(SET, recent, !bit);
		}

		private int lastInRange(int from, int to) {
			return bit ? lastOneInRangeAdj(from, to) : lastZeroInRangeAdj(from, to);
		}

		private int firstInRange(int from, int to) {
			return bit ? firstOneInRangeAdj(from, to) : firstZeroInRangeAdj(from, to);
		}

		private int countInRange(int from, int to) {
			int count = countOnesAdj(from, next);
			return bit ? count : next - from - count;
		}
	}

	private final class VectorList extends AbstractList<Boolean> {

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
			int count = countOnesAdj(start, finish);
			return (Boolean)o ?
					// check for ones
					count > 0 :
					// check for zeros
					finish - start - count > 0;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			boolean ones = c.contains(Boolean.TRUE);
			boolean zeros = c.contains(Boolean.FALSE);
			int bools = 0;
			if (ones) bools++;
			if (zeros) bools++;
			if (c.size() > bools) return false; // must contain a non-boolean
            return switch (bools) {
                case 0 -> true; // empty collection
                case 1 -> !match(zeros).isAll();
                default -> {
                    int count = countOnesAdj(start, finish);
                    yield count > 0 && (finish - start - count) > 0;
                }
            };
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

		private final boolean bit;
		private final int offset; // the value that must be added to received values to map them onto the bits

		// constructors

		private IntSet(boolean bit, int offset) {
			this.bit = bit;
			this.offset = offset;
		}

		// set methods

		@Override
		public int size() {
			int count = countOnesAdj(start, finish);
			return bit ? count : finish - start - count;
		}

		@Override
		public boolean isEmpty() {
			return bit ? isAllZerosAdj(start, finish) : isAllOnesAdj(start, finish);
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Integer)) return false;
			int position = offset + (Integer) o;
			return position >= start && position < finish && getBitAdj(position) == bit;
		}

		@Override
		public boolean add(Integer e) {
			return getThenPerformAdj(SET, position(e), bit) != bit;
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Integer)) return false;
			int i = offset + (Integer) o;
			if (i < start || i >= finish) return false;
			return getThenSetBit(i, !bit) == bit;
		}

		@Override
		public void clear() {
			checkMutable();
			if (bit) {
				performAdjClear(start, finish);
			} else {
				performAdjSet(start, finish);
			}
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				final Iterator<Integer> it = match(bit).positions();

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
				changed = getThenPerformAdj(SET, it.next() + offset, bit) != bit;
			}
			while (it.hasNext()) {
				performAdj(SET, it.next() + offset, bit);
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
			return duplicateAdj(start, to, false, mutable).asSet(bit, offset);
		}

		@Override
		public SortedSet<Integer> tailSet(Integer fromElement) {
			if (fromElement == null) throw new NullPointerException();
			int from = Math.min(fromElement + offset, finish);
			return duplicateAdj(from, finish, false, mutable).asSet(bit, offset);
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
			return duplicateAdj(from, to, false, mutable).asSet(bit, offset);
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

		@java.io.Serial
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

		@java.io.Serial
		private Object readResolve() throws ObjectStreamException {
			return new BitVector(this);
		}

	}

	// classes for reading and writing bits

	private abstract class VectorStream implements BitStream {

		final long initialPos;
		final long finalPos;
		int position;

		private VectorStream(int finalPos, int initialPos) {
			this.finalPos = finalPos;
			this.initialPos = initialPos;
			position = initialPos;
		}

		@Override
		public long getPosition() {
			return initialPos - position;
		}

		@Override
		public long setPosition(long position) {
			position = Math.max(position, finalPos - start);
			position = Math.min(position, initialPos - start);
			this.position = (int) (initialPos - position);
			return position;
		}

		@Override
		public long skipBits(long count) {
			long advance = count < 0 ?
				Math.max(position - initialPos, count):
				Math.min(position - finalPos, count);
			position -= (int) advance;
			return advance;
		}

	}

	private final class VectorReader extends VectorStream implements BitReader {

		private VectorReader(int finalPos, int initialPos) {
			super(finalPos, initialPos);
		}

		private VectorReader() {
			this(start, finish);
		}

		@Override
		public int readBit() {
			if (position == finalPos) throw new EndOfBitStreamException();
			return getBitAdj(--position) ? 1 : 0;
		}

		@Override
		public int read(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 32) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0;
			if (position - count < finalPos) throw new EndOfBitStreamException();
			return (int) getBitsAdj(position -= count, count);
		}

		@Override
		public long readLong(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0L;
			if (position - count < finalPos) throw new EndOfBitStreamException();
			return getBitsAdj(position -= count, count);
		}

		@Override
		public BigInteger readBigInt(int count) throws BitStreamException {
			if (position - count < finalPos) throw new EndOfBitStreamException();
			switch(count) {
			case 0 : return BigInteger.ZERO;
			case 1 : return getBitAdj(position--) ? BigInteger.ONE : BigInteger.ZERO;
			default :
				final int from = position - count;
				final int to = position;
				position = from;
				return duplicateAdj(from, to, false, false).toBigInteger();
			}
		}

		@Override
		public boolean readBoolean() {
			if (position == finalPos) throw new EndOfBitStreamException();
			return getBitAdj(--position);
		}

		@Override
		public int readUntil(boolean one) throws BitStreamException {
			int index = one ? lastOneInRangeAdj(start, position) : lastZeroInRangeAdj(start, position);
			if (index < finalPos) throw new EndOfBitStreamException();
			int read = position - index - 1;
			position = index;
			return read;
		}

	}

	private final class VectorWriter extends VectorStream implements BitWriter {

		private final int operation;

		private VectorWriter(int operation, int finalPos, int initialPos) {
			super(finalPos, initialPos);
			this.operation = operation;
		}

		private VectorWriter() {
			this(SET, start, finish);
		}

		@Override
		public int writeBit(int bit) {
			if (position == finalPos) throw new EndOfBitStreamException();
			//TODO consider an optimized version of this
			performAdj(operation, --position, (bit & 1) == 1);
			return 1;
		}

		@Override
		public int writeBoolean(boolean bit) {
			if (position == finalPos) throw new EndOfBitStreamException();
			performAdj(operation, --position, bit);
			return 1;
		}

		@Override
		public long writeBooleans(boolean value, long count) {
			//TODO need to guard against overflow?
			if (position - count < finalPos) throw new EndOfBitStreamException();
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
			if (position - count < finalPos) throw new EndOfBitStreamException();
			performAdj(operation, position -= count, bits, count);
			return count;
		}

		@Override
		public int write(long bits, int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			if (count == 0) return 0;
			if (position - count < finalPos) throw new EndOfBitStreamException();
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

	}

}

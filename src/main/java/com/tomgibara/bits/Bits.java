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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.Random;
import java.util.SortedSet;

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.WriteStream;

/**
 * <p>
 * Static methods that provide the entry-point for most package functions. This
 * package provides a dense library of bit storage, transformation and streaming
 * functionality, and consequently, this class features a large number of
 * methods; organized into the following categories:
 *
 * <ul>
 * <li>
 * Helper objects that assist with implementing {@link BitStore} functionality
 * {@link #bitStoreHasher()}, {@link #numericalComparator()} and
 * {@link #lexicalComparator()}.
 *
 * <li>
 * Methods for creating new {@link BitStore} instances: {@link #store(int)}
 * <code>toStore(...)</code> and <code>asStore(...)</code>.
 *
 * <li>
 * Methods that return immutable basic {@link BitStore} instances:
 * {@link #noBits()}, {@link #oneBit()}, {@link #zeroBit()},
 * {@link #oneBits(int)}, {@link #zeroBits(int)}, {@link #bit(boolean)} and
 * {@link #bits(boolean, int)}.
 *
 * <li>
 * Methods that adapt common Java I/O primitives into bit readers:
 * <code>readerFrom(...)</code>
 *
 * <li>
 * Methods that adapt common Java I/O primitives into bit writers:
 * <code>writerTo(...)</code>, {@link #writerToStdout()},
 * {@link #writerToNothing()}.
 *
 * <li>
 * Methods to assist with growing bit stores: {@link #growableBits()},
 * {@link #growableBits(int)}, {@link #resizedCopyOf(BitStore, int, boolean)}.
 * </ul>
 *
 * <p>
 * The <code>asStore(...)</code> methods each produce a (generally mutable)
 * {@link BitStore} view of another object:
 *
 * <ul>
 * <li>
 * To create immutable {@link BitStore} instances that continue to reflect
 * changes to the underlying object use {@link BitStore#immutableView()}.
 *
 * <li>
 * To create a mutable instance that is detached from the original object, use
 * {@link BitStore#mutableCopy()} (note that there may be a
 * <code>toStore(...)</code> that provides a direct way of producing the same).
 *
 * <li>
 * To create a completely immutable instance that cannot be modified through
 * either the {@link BitStore} API nor the original object use
 * {@link BitStore#immutableCopy()}.
 * </ul>
 *
 * @author Tom Gibara
 *
 */

public final class Bits {

	private static final Hasher<BitStore> bitStoreHasher = bitStoreHasher((b,s) -> b.writeTo(s));

	private static final Comparator<BitStore> numericalComparator = new Comparator<BitStore>() {
		@Override
		public int compare(BitStore a, BitStore b) {
			return a.compareNumericallyTo(b);
		}
	};

	private static final Comparator<BitStore> lexicalComparator = new Comparator<BitStore>() {
		@Override
		public int compare(BitStore a, BitStore b) {
			return a.compareLexicallyTo(b);
		}
	};

	// public

	// helpers

	/**
	 * Produces a binary String representation consistent with that specified in
	 * the contract for {@link BitStore}. This method is intended to assist
	 * implementors of the {@link BitStore} interface and consequently does not
	 * rely on the <code>toString()</code> implementation of the supplied store.
	 *
	 * @param store
	 *            a bit store
	 * @return the supplied bit store as a binary string
	 */

	public static String toString(BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		switch (size) {
		case 0 : return "";
		case 1 : return store.getBit(0) ? "1" : "0";
		default:
			StringBuilder sb = new StringBuilder(size);
			int to = size;
			int from = size & ~63;
			if (from != to) {
				int length = to - from;
				long bits = store.getBits(from, length) << (64 - length);
				for (; length > 0; length --) {
					sb.append(bits < 0L ? '1' : '0');
					bits <<= 1;
				}
			}
			while (from != 0) {
				from -= 64;
				long bits = store.getLong(from);
				for (int i = 0; i < 64; i++) {
					sb.append(bits < 0L ? '1' : '0');
					bits <<= 1;
				}
			}
			return sb.toString();
		}
	}

	/**
	 * A hasher that generates hash codes that are consistent with the stated
	 * contract for {@link BitStore}. The resulting hasher may be used for this
	 * purpose as follows:
	 * <code>Bits.bitStoreHasher().intHashValue(store)</code>.
	 *
	 * @return a hasher of {@link BitStore}
	 */

	public static Hasher<BitStore> bitStoreHasher() {
		return bitStoreHasher;
	}

	/**
	 * A comparator that orders bit stores consistently with
	 * {@link BitStore#compareNumericallyTo(BitStore)}.
	 *
	 * @return a numerical comparator of {@link BitStore}
	 */

	public static Comparator<BitStore> numericalComparator() {
		return numericalComparator;
	}

	/**
	 * A comparator that orders bit stores consistently with
	 * {@link BitStore#compareLexicallyTo(BitStore)}.
	 *
	 * @return a lexical comparator of {@link BitStore}
	 */

	public static Comparator<BitStore> lexicalComparator() {
		return lexicalComparator;
	}

	// new bit store

	/**
	 * Creates a new mutable {@link BitStore} instance with the specified size.
	 * The implementing class is likely to vary with the requested size.
	 *
	 * @param size
	 *            the capacity, in bits, of the new {@link BitStore}
	 *
	 * @return a new mutable {@link BitStore} of the specified size.
	 */

	public static BitStore store(int size) {
		if (size < 0) throw new IllegalArgumentException();
		switch (size) {
		case 0 : return VoidBitStore.MUTABLE;
		case 1 : return new Bit();
		case 64: return new LongBitStore();
		default:
			return size < 64 ?
					new LongBitStore().range(0, size) :
					new BitVector(size);
		}
	}

	/**
	 * Creates a mutable {@link BitStore} initialized with a binary string of
	 * characters. The size of store will equal the number of characters. The
	 * string is treated as an arbitrary length binary number, so (by way of
	 * example) the value the zeroth character in the string determines the
	 * value of the highest indexed bit in the store.
	 *
	 * @param chars
	 *            a character sequence of the binary characters <code>'0'</code>
	 *            and <code>'1'</code>
	 * @return a new mutable {@link BitStore} initialized with the specified
	 *         binary string
	 * @see #asStore(CharSequence)
	 * @see #readerFrom(CharSequence)
	 */

	public static BitStore toStore(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		int size = chars.length();
		BitStore store = store(size);
		transferImpl( new CharBitReader(chars), store.openWriter(), size );
		return store;
	}

	/**
	 * Creates a mutable {@link BitStore} initialized with random bit values.
	 *
	 * @param size
	 *            the capacity, in bits, of the new {@link BitStore}
	 * @param random
	 *            a source of randomness
	 * @param probability
	 *            a number between 0 and 1 inclusive, being the independent
	 *            probability that a bit is set
	 * @return a new mutable {@link BitStore} initialized with random bit values
	 */

	public static BitStore toStore(int size, Random random, float probability) {
		return new BitVector(random, probability, size);
	}

	/**
	 * Creates a mutable {@link BitStore} initialized with random bit values;
	 * the independent probability of each bit having a value of 1 being equal
	 * to 0.5.
	 *
	 * @param size
	 *            the capacity, in bits, of the new {@link BitStore}
	 * @param random
	 *            a source of randomness
	 * @return a new mutable {@link BitStore} initialized with random bit values
	 */

	public static BitStore toStore(int size, Random random) {
		return new BitVector(random, size);
	}

	/**
	 * Creates a mutable {@link BitStore} instance of size one (ie. containing a
	 * single bit).
	 *
	 * @param bit
	 *            the value of the single bit in the new {@link BitStore}
	 *
	 * @return a new mutable {@link BitStore} initialized with the specified bit
	 *         value
	 */

	public static BitStore toStore(boolean bit) {
		return new Bit(bit);
	}

	/**
	 * Creates a mutable {@link BitStore} instance of size 64, initialized with
	 * the bit values of the supplied long. The long is treated as a big-endian
	 * value, so (by way of example) its least significant bit determines the
	 * value of the store's zero indexed bit.
	 *
	 * @param bits
	 *            the values of the bits in the new {@link BitStore}
	 *
	 * @return a new mutable {@link BitStore} of size 64 initialized with the
	 *         bits of the supplied long
	 */

	public static BitStore toStore(long bits) {
		return new LongBitStore(bits);
	}

	/**
	 * Creates a mutable {@link BitStore} instance initialized with the bit
	 * values from a supplied long. The long is treated as a big-endian value,
	 * so (by way of example) its least significant bit determines the value of
	 * the store's zero indexed bit. The count parameter determines the number
	 * of bits read from the long value that are used to construct the new
	 * {@link BitStore}, and consequently, the size of the returned bit store.
	 * Where the count is less that 64, the least signficant bits of the long
	 * value are used.
	 *
	 * @param bits
	 *            the values of the bits in the new {@link BitStore}
	 * @param count
	 *            between 0 and 64 inclusive, the number of bits used to
	 *            populate the new {@link BitStore}
	 *
	 * @return a new mutable {@link BitStore} initialized with the bits of the
	 *         supplied long
	 */

	public static BitStore toStore(long bits, int count) {
		return new LongBitStore(bits).range(0, count);
	}

	// immutable bit stores

	/**
	 * An immutable {@link BitStore} of zero size.
	 *
	 * @return an immutable zero size {@link BitStore}
	 */

	public static BitStore noBits() {
		return VoidBitStore.MUTABLE;
	}

	/**
	 * An immutable {@link BitStore} consisting of a single one bit.
	 *
	 * @return an immutable {@link BitStore} of size 1, containing a one bit.
	 * @see #bit(boolean)
	 */

	public static BitStore oneBit() {
		return ImmutableOne.INSTANCE;
	}

	/**
	 * An immutable {@link BitStore} consisting of a single zero bit.
	 *
	 * @return an immutable {@link BitStore} of size 1, containing a zero bit.
	 * @see #bit(boolean)
	 */

	public static BitStore zeroBit() {
		return ImmutableZero.INSTANCE;
	}

	/**
	 * An immutable {@link BitStore} containing a specified number one bits.
	 *
	 * @param size
	 *            the number of bits in the bit store
	 * @return an immutable {@link BitStore} of the specified size containing
	 *         only one bits
	 * @see #bits(boolean,int)
	 */

	public static BitStore oneBits(int size) {
		checkSize(size);
		return new ImmutableBits.ImmutablesOnes(size);
	}

	/**
	 * An immutable {@link BitStore} containing a specified number zero bits.
	 *
	 * @param size
	 *            the number of bits in the bit store
	 * @return an immutable {@link BitStore} of the specified size containing
	 *         only zero bits
	 * @see #bits(boolean,int)
	 */

	public static BitStore zeroBits(int size) {
		checkSize(size);
		return new ImmutableBits.ImmutablesZeros(size);
	}

	/**
	 * An immutable {@link BitStore} consisting of a single bit with a specified
	 * value.
	 *
	 * @param bit
	 *            the bit with which the {@link BitStore} is populated
	 * @return an immutable {@link BitStore} consisting of the specified bit
	 * @see #oneBit()
	 * @see #zeroBit()
	 */

	public static BitStore bit(boolean bit) {
		return ImmutableBit.instanceOf(bit);
	}

	/**
	 * An immutable {@link BitStore} of identical bits.
	 *
	 * @param ones
	 *            true if the {@link BitStore} contains ones, false if it
	 *            contains zeros
	 * @param size
	 *            the number of bits in the {@link BitStore}
	 * @return an immutable {@link BitStore} of the specified bits
	 * @see #oneBits(int)
	 * @see #zeroBits(int)
	 */

	public static BitStore bits(boolean ones, int size) {
		return ones ? oneBits(size) : zeroBits(size);
	}

	// bit store views

	/**
	 * Exposes a <code>BitSet</code> as a {@link BitStore}. The returned bit
	 * store is a live view over the bit set; changes made to the bit set are
	 * reflected in the bit store and vice versa. Unlike bit sets, bit stores
	 * have a fixed size which must be specified at construction time and which
	 * is not required to match the length of the bit set. In all cases, the
	 * bits of the bit set are drawn from the lowest-indexed bits.
	 *
	 * @param bitSet
	 *            the bits of the {@link BitStore}
	 * @param size
	 *            the size, in bits, of the {@link BitStore}
	 * @return a {@link BitStore} view over the bit set.
	 */

	public static BitStore asStore(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException("null bitSet");
		checkSize(size);
		return new BitSetBitStore(bitSet, 0, size, true);
	}

	/**
	 * Exposes the bits of a byte array as a {@link BitStore}. The returned bit
	 * store is a live view over the byte array; changes made to the array are
	 * reflected in bit store and vice versa. The bit store contains every bit
	 * in the array with the zeroth indexed bit of the store taking its value
	 * from the least significant bit of the byte at index zero.
	 *
	 * @param bytes
	 *            the byte data
	 * @return a {@link BitStore} over the bytes.
	 * @see #asStore(byte[], int, int)
	 * @see BitVector#fromByteArray(byte[], int)
	 */

	public static BitStore asStore(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (bytes.length * 8L > Integer.MAX_VALUE) throw new IllegalArgumentException("index overflow");
		return new BytesBitStore(bytes, 0, bytes.length << 3, true);
	}

	/**
	 * Exposes a subrange of the bits of a byte array as a {@link BitStore}. The
	 * returned bit store is a live view over the bytes; changes made to the
	 * array are reflected in bit store and vice versa. The size of the returned
	 * bit vector is the length of the sub range.
	 *
	 * @param bytes
	 *            the byte data
	 * @param offset
	 *            the index, in bits of the first bit in the bit store
	 * @param length
	 *            the number of bits spanned by the bit store
	 * @return a {@link BitStore} over some range of bytes
	 * @see #asStore(byte[])
	 * @see BitVector#fromByteArray(byte[], int)
	 */

	public static BitStore asStore(byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		int size = bytes.length << 3;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish < 0) throw new IllegalArgumentException("index overflow");
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BytesBitStore(bytes, offset, finish, true);
	}

	/**
	 * Exposes an array of booleans as a {@link BitStore}. The returned bit
	 * store is a live view over the booleans; changes made to the array are
	 * reflected in bit store and vice versa. The size of the returned bit
	 * vector equals the length of the array with the bits of the
	 * {@link BitStore} indexed as per the underlying array.
	 *
	 * @param bits
	 *            the bit values
	 * @return a {@link BitStore} over the boolean array
	 */

	public static BitStore asStore(boolean[] bits) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		return new BooleansBitStore(bits, 0, bits.length, true);
	}

	/**
	 * Exposes a sub-range of a boolean array as a {@link BitStore}. The
	 * returned bit store is a live view over the booleans; changes made to the
	 * array are reflected in bit store and vice versa. The size of the returned
	 * bit vector equals the length of the range with the least-significant bit
	 * of the {@link BitStore} taking its value from the lowest-indexed value in
	 * the range.
	 *
	 * @param bits
	 *            an array of bit values
	 * @param offset
	 *            the index of the first boolean value in the {@link BitStore}
	 * @param length
	 *            the number of boolean values covered by the {@link BitStore}
	 * @return a {@link BitStore} over the boolean array
	 */

	public static BitStore asStore(boolean[] bits, int offset, int length) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		int size = bits.length;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish < 0) throw new IllegalArgumentException("index overflow");
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BooleansBitStore(bits, offset, finish, true);
	}

	/**
	 * Exposes a <code>BigInteger</code> as an immutable {@link BitStore}. The
	 * returned bit store draws its values directly from the supplied big
	 * integer (no copying of bit data takes place) with the bit indexing
	 * consistent with that of <code>BigInteger</code>. The size of the bit
	 * store is <code>bigInt.bitLength()</code>.
	 *
	 * @param bigInt
	 *            a big integer
	 * @return a {@link BitStore} view over the big integer.
	 */

	public static BitStore asStore(BigInteger bigInt) {
		if (bigInt == null) throw new IllegalArgumentException("null bigInt");
		return new BigIntegerBitStore(bigInt);
	}

	/**
	 * Exposes a <code>SortedSet</code> of <code>Integer</code> as
	 * {@link BitStore}. The <code>start</code> and <code>finish</code>
	 * parameters must form a valid sub-range of the set. Since it is not
	 * possible to determine whether a set is modifiable, the mutability of the
	 * generated {@link BitStore} must be specified as a call parameter.
	 * Creating a mutable {@link BitStore} over an unmodifiable set may result
	 * in unspecified errors on any attempt to mutate the bit store.
	 *
	 * @param set
	 *            a sorted set of integers
	 * @param start
	 *            the least integer exposed by the {@link BitStore}
	 * @param finish
	 *            the least integer greater than or equal to <code>start</code>
	 *            that is not exposed by the {@link BitStore}
	 * @param mutable
	 *            whether the returned {@link BitStore} is mutable
	 * @throws IllegalArgumentException
	 *             if the range start-to-finish does not form a valid sub-range
	 *             of the supplied set.
	 * @return a {@link BitStore} view over the set
	 */

	public static BitStore asStore(SortedSet<Integer> set, int start, int finish, boolean mutable) {
		if (set == null) throw new IllegalArgumentException("null set");
		if (start < 0L) throw new IllegalArgumentException("negative start");
		if (finish < start) throw new IllegalArgumentException("start exceeds finish");
		set = set.subSet(start, finish);
		return new IntSetBitStore(set, start, finish, mutable);
	}

	/**
	 * <p>
	 * Exposes a <code>CharSequence</code> as a {@link BitStore}. The returned
	 * bit store draws its values directly from the supplied character data (no
	 * copying of bit data takes place) such that the character at index zero is
	 * exposed as the <em>most-significant</em> bit in the store. This
	 * consistent with the conventional string representation of binary values.
	 *
	 * <p>
	 * The supplied character sequence is required to consist only of the
	 * characters <code>'0'</code> and <code>'1'</code>. Since
	 * <code>CharSequence</code> instances may be mutable, it is not possible to
	 * enforce, in general, that a sequence meets this requirement; sequences
	 * consisting of invalid characters may result an
	 * <code>IllegalStateException</code> being thrown when the bit store is
	 * operated on.
	 *
	 * <p>
	 * It is not possible, given an arbitrary <code>CharSequence</code> instance
	 * to determine its mutability. For this reason the current implementation
	 * is conservative and makes the following assumption: that
	 * <code>StringBuilder</code> or <code>StringBuffer</code> instances are
	 * mutable and that all other <code>CharSequence</code> instances (including
	 * <code>String</code> are not. This is used to determine the mutability of
	 * the returned {@link BitStore}.
	 *
	 * @param chars
	 *            a sequence of characters
	 * @return a {@link BitStore} over the character sequence.
	 * @see #toStore(CharSequence)
	 * @see #readerFrom(CharSequence)
	 */

	public static BitStore asStore(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new CharsBitStore(chars);
	}

	// bit streams

	/**
	 * A {@link BitReader} that sources its bits from a
	 * <code>CharSequence</code>. Bits are read from the sequence starting with
	 * the character at index zero. The presence of a character other than a '1'
	 * or '0' will result in a {@link BitStreamException} being thrown when an
	 * attempt is made to read a bit at that index.
	 *
	 * @param chars
	 *            the source characters
	 * @return a bit reader over the characters
	 */

	public static BitReader readerFrom(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new CharBitReader(chars);
	}

	/**
	 * A {@link BitReader} that sources its bits from an array of bytes. Bits
	 * are read from the byte array starting at index zero. Within each byte,
	 * the most significant bits are read first.
	 *
	 * @param bytes
	 *            the source bytes
	 * @return a bit reader over the bytes
	 */

	public static BitReader readerFrom(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new ByteArrayBitReader(bytes);
	}

	/**
	 * A {@link BitReader} that sources its bits from an array of bytes. Bits are
	 * read from the byte array starting at index zero. Within each byte, the
	 * most significant bits are read first.
	 *
	 * @param bytes
	 *            the source bytes
	 * @param size
	 *            the number of bits that may be read, not negative and no
	 *            greater than the number of bits supplied by the array
	 * @return a bit reader over the bytes
	 */

	public static BitReader readerFrom(byte[] bytes, long size) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		checkSize(size, ((long) bytes.length) << 3);
		return new ByteArrayBitReader(bytes, size);
	}

	/**
	 * A {@link BitReader} that sources its bits from an array of ints. Bits are
	 * read from the int array starting at index zero. Within each int, the most
	 * significant bits are read first. The size of the reader will equal the
	 * total number of bits in the array.
	 *
	 * @param ints
	 *            the source ints
	 * @return a bit reader over the ints
	 */

	public static BitReader readerFrom(int[] ints) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		return new IntArrayBitReader(ints);
	}

	/**
	 * A {@link BitReader} that sources its bits from an array of ints. Bits are
	 * read from the int array starting at index zero. Within each int, the most
	 * significant bits are read first.
	 *
	 * @param ints
	 *            the source ints
	 * @param size
	 *            the number of bits that may be read, not negative and no
	 *            greater than the number of bits supplied by the array
	 * @return a bit reader over the ints
	 */

	public static BitReader readerFrom(int[] ints, long size) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		checkSize(size, ((long) ints.length) << 5);
		return new IntArrayBitReader(ints, size);
	}

	/**
	 * A {@link BitReader} that sources bits from a <code>FileChannel</code>.
	 * This stream operates with a byte buffer. This will generally improve
	 * performance in applications that skip forwards or backwards across the
	 * file.
	 *
	 * Note that using a direct ByteBuffer should generally yield better
	 * performance.
	 *
	 * @param channel
	 *            the file channel from which bits are to be read
	 * @param buffer
	 *            the buffer used to store file data
	 * @return a bit reader over the channel
	 */

	public static BitReader readerFrom(FileChannel channel, ByteBuffer buffer) {
		if (channel == null) throw new IllegalArgumentException("null channel");
		if (buffer == null) throw new IllegalArgumentException("null buffer");
		return new FileChannelBitReader(channel, buffer);
	}

	/**
	 * A {@link BitReader} that sources its bits from an
	 * <code>InputStream</code>.
	 *
	 * @param in
	 *            the source input stream
	 * @return a bit reader over the input stream
	 */

	public static BitReader readerFrom(InputStream in) {
		if (in == null) throw new IllegalArgumentException("null in");
		return new InputStreamBitReader(in);
	}

	/**
	 * A {@link BitReader} that sources its bits from a <code>ReadStream</code>.
	 *
	 * @param stream
	 *            the source stream
	 * @return a bit reader over the stream
	 */

	public static BitReader readerFrom(ReadStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		return new StreamBitReader(stream);
	}

	/**
	 * A {@link BitWriter} that writes its bits to an array of bytes. Bits are
	 * written to the byte array starting at index zero. Within each byte, the
	 * most significant bits is written to first.
	 *
	 * @param bytes
	 *            the array of bytes
	 * @return a writer that writes bits to the supplied array
	 */

	public static BitWriter writerTo(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new ByteArrayBitWriter(bytes);
	}

	/**
	 * A {@link BitWriter} that writes its bits to an array of bytes. Bits are
	 * written to the byte array starting at index zero. Within each byte, the
	 * most significant bits is written to first.
	 *
	 * @param bytes
	 *            the array of bytes
	 * @param size
	 *            the number of bits that may be written, not negative and no
	 *            greater than the number of bits supplied by the array
	 * @return a writer that writes bits to the supplied array
	 */

	public static BitWriter writerTo(byte[] bytes, long size) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		checkSize(size, ((long) bytes.length) << 3);
		return new ByteArrayBitWriter(bytes, size);
	}

	/**
	 * Writes bits to an array of ints.
	 *
	 * @param ints
	 *            the array of ints
	 * @return a writer that writes bits to the supplied array
	 */

	public static BitWriter writerTo(int[] ints) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		return new IntArrayBitWriter(ints);
	}

	/**
	 * Writes bits to an array of ints up-to a specified limit.
	 *
	 * @param ints
	 *            the array of ints
	 * @param size
	 *            the greatest number of bits the writer will write to the array
	 * @return a writer that writes bits to the supplied array
	 */

	public static BitWriter writerTo(int[] ints, long size) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		if (size < 0) throw new IllegalArgumentException("negative size");
		long maxSize = ((long) ints.length) << 5;
		if (size > maxSize) throw new IllegalArgumentException("size exceeds maximum permitted by array length");
		return new IntArrayBitWriter(ints);
	}

	/**
	 * A {@link BitWriter} that writes its bits to an <code>OutputStream</code>.
	 *
	 * @param out
	 *            an output stream
	 * @return a writer over the output stream
	 */

	public static BitWriter writerTo(OutputStream out) {
		if (out == null) throw new IllegalArgumentException("null out");
		return new OutputStreamBitWriter(out);
	}

	/**
	 * A {@link BitWriter} that writes its bits to <code>WriteStream</code>.
	 *
	 * @param stream
	 *            a stream
	 * @return a writer over the stream
	 */

	public static BitWriter writerTo(WriteStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		return new StreamBitWriter(stream);
	}

	/**
	 * <p>
	 * A new 'null' bit stream that only counts the number of bits written,
	 * without storing the bits. The number of bits written can be recovered
	 * from the {@link BitWriter#getPosition()} method.
	 *
	 * <p>
	 * This class is intended to be used in circumstances where adjusting writer
	 * capacity may be less efficient than writing twice to a stream: once to
	 * count the length before allocating storage and a second time to store the
	 * bits written.
	 *
	 * @return a new bit stream.
	 */

	public static BitWriter writerToNothing() {
		return new NullBitWriter();
	}

	/**
	 * A convenient writer for dumping bits to the <code>System.out</code> print
	 * stream.
	 *
	 * @return a bit writer that outputs <code>'1'</code>s and <code>'0'</code>s
	 *         to STDOUT.
	 */

	public static BitWriter writerToStdout() {
		return new PrintStreamBitWriter(System.out);
	}

	/**
	 * A convenient writer for dumping bits to a specified
	 * <code>PrintWriter</code>.
	 *
	 * @param stream
	 *            a print writer
	 * @return a bit writer that outputs <code>'1'</code>s and <code>'0'</code>s
	 *         to the <code>PrintWriter</code>.
	 *
	 * @see #writerToStdout()
	 */

	public static BitWriter writerTo(PrintStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		return new PrintStreamBitWriter(stream);
	}

	// exposed to assist implementors of BitStore.Op interface

	/**
	 * Creates a {@link BitWriter} that writes its bits to a {@link BitStore}
	 * using a specified {@link Operation}. This method is primarily intended to
	 * assist in implementing a highly adapted {@link BitStore} implementation.
	 * Generally {@link BitStore.Op#openWriter(int, int)},
	 * {@link BitStore#openWriter(int, int)}
	 * {@link BitStore.Op#openWriter(int, int)} and should be used in preference
	 * to this method. Note that the {@link BitWriter} will
	 * <em>write in big-endian order</em> which this means that the first bit is
	 * written at the largest index, working downwards to the least index.
	 *
	 * @param store
	 *            the store to which bits will be written
	 * @param operation
	 *            the operation that should be applied on writing
	 * @param finalPos
	 *            the (exclusive) index at which the writer stops; less than
	 *            <code>initialPos</code>
	 * @param initialPos
	 *            the (inclusive) index at which the writer starts; greater than
	 *            <code>finalPos</code>
	 * @return a writer into the store
	 * @see BitStore#openWriter()
	 * @see BitStore#openWriter(int, int)
	 * @see BitStore.Op#openWriter(int, int)
	 */

	public static BitWriter writerTo(BitStore store, Operation operation, int finalPos, int initialPos) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (operation == null) throw new IllegalArgumentException("null operation");
		if (finalPos < 0) throw new IllegalArgumentException("negative finalPos");
		if (initialPos < finalPos) throw new IllegalArgumentException("finalPos exceeds initialPos");
		if (initialPos > store.size()) throw new IllegalArgumentException("invalid initialPos");

		switch(operation) {
		case SET: return new BitStoreWriter.Set(store, finalPos, initialPos);
		case AND: return new BitStoreWriter.And(store, finalPos, initialPos);
		case OR:  return new BitStoreWriter.Or (store, finalPos, initialPos);
		case XOR: return new BitStoreWriter.Xor(store, finalPos, initialPos);
		default:
			throw new IllegalStateException("unsupported operation");
		}
	}

	// miscellany

	/**
	 * Creates a mutable, resized copy of a {@link BitStore}. The new size may
	 * be equal, larger or smaller than original size. When the the new size is
	 * greater than the original size, the new bits are identically zero. The
	 * method makes no guarantees about the BitStore implementation that
	 * results, in particular, there is certainly no guarantee that the returned
	 * {@link BitStore} will share its implementation with the supplied
	 * {@link BitStore}.
	 *
	 * @param store
	 *            a BitStore
	 * @param newSize
	 *            a new size for the BitStore
	 * @param anchorLeft
	 *            true if the most-significant bit of the original store should
	 *            remain the most-significant bit of the resized store, false if
	 *            the least-significant bit of the original store should remain
	 *            the least-significant bit of the resized store
	 * @return a resized copy of the original {@link BitStore}
	 */

	public static BitStore resizedCopyOf(BitStore store, int newSize, boolean anchorLeft) {
		if (newSize < 0) throw new IllegalArgumentException();
		int size = store.size();
		if (size == newSize) return store.mutableCopy();
		int from;
		int to;
		if (anchorLeft) {
			from = size - newSize;
			to = size;
		} else {
			from = 0;
			to = newSize;
		}
		if (newSize < size) return store.range(from, to).mutableCopy();
		BitStore copy = store(newSize);
		copy.setStore(-from, store);
		return copy;
	}

	/**
	 * Creates a new growable bits container with a specified initial capacity.
	 *
	 * It's implementation is such that, if writes do not exceed the initial
	 * capacity, no copies or new allocations of bit data will occur.
	 *
	 * @param initialCapacity
	 *            the initial capacity in bits
	 * @return new growable bits
	 */

	public static GrowableBits growableBits(int initialCapacity) {
		if (initialCapacity < 0) throw new IllegalArgumentException("negative initialCapacity");
		return new GrowableBits(new BitVectorWriter(initialCapacity));
	}

	/**
	 * Creates a new growable bits container with a default initial capacity.
	 *
	 * The default capacity is currently 64 bits, but this is not guaranteed to
	 * remain unchanged between releases.
	 *
	 * @return new growable bits
	 */

	public static GrowableBits growableBits() {
		return new GrowableBits(new BitVectorWriter());
	}

	public static void transfer(BitReader reader, BitWriter writer, long count) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (writer == null) throw new IllegalArgumentException("null writer");
		if (count < 0L) throw new IllegalArgumentException("negative count");
		transferImpl(reader, writer, count);
	}

	// package only

	static <B> Hasher<B> bitStoreHasher(StreamSerializer<B> s) {
		return Hashing.murmur3Int().hasher(s);
	}

	// available via default BitStore method
	static BitStore newRangedView(BitStore store, int from, int to) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		if (to > store.size()) throw new IllegalArgumentException();
		return new AbstractBitStore() {

			@Override
			public int size() {
				return to - from;
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(adjIndex(index));
			}

			@Override
			public void setBit(int index, boolean value) {
				store.setBit(adjIndex(index), value);
			}

			@Override
			public void flipBit(int index) {
				store.flipBit(adjIndex(index));
			}

			@Override
			public long getBits(int position, int length) {
				return store.getBits(adjPosition(position), length);
			}

			@Override
			public boolean getThenSetBit(int index, boolean value) {
				return store.getThenSetBit(adjIndex(index), value);
			}

			@Override
			public void setStore(int position, BitStore that) {
				store.setStore(adjPosition(position), that);
			}

			@Override
			public BitWriter openWriter() {
				return Bits.newBitWriter(store, from, to);
			}

			@Override
			public BitWriter openWriter(int finalPos, int initialPos) {
				return store.openWriter(adjPosition(finalPos), adjPosition(initialPos));
			}

			@Override
			public BitReader openReader() {
				return store.openReader(from, to);
			}

			@Override
			public BitReader openReader(int finalPos, int initialPos) {
				return store.openReader(adjPosition(finalPos), adjPosition(initialPos));
			}

			@Override
			public boolean isMutable() {
				return store.isMutable();
			}

			private int adjIndex(int index) {
				if (index < 0) throw new IllegalArgumentException("negative index");
				index += from;
				if (index >= to) throw new IllegalArgumentException("index too large");
				return index;
			}

			private int adjPosition(int position) {
				if (position < 0) throw new IllegalArgumentException("negative position");
				position += from;
				if (position > to) throw new IllegalArgumentException("position too large");
				return position;
			}
		};
	}

	// available via default BitStore method
	static Number asNumber(BitStore store) {
		return new Number() {

			private static final long serialVersionUID = -2906430071162493968L;

			final int size = store.size();

			@Override
			public byte byteValue() {
				return (byte) store.getBits(0, Math.min(8, size));
			}

			@Override
			public short shortValue() {
				return (short) store.getBits(0, Math.min(16, size));
			}

			@Override
			public int intValue() {
				return (int) store.getBits(0, Math.min(32, size));
			}

			@Override
			public long longValue() {
				return store.getBits(0, Math.min(64, size));
			}

			@Override
			public float floatValue() {
				return store.toBigInteger().floatValue();
			}

			@Override
			public double doubleValue() {
				return store.toBigInteger().doubleValue();
			}

		};
	}

	//TODO further optimizations possible
	// available via default BitStore method
	static BitWriter newBitWriter(BitStore store, int finalPos, int initialPos) {
		return writerTo(store, Operation.SET, finalPos, initialPos);
	}

	// available via default BitStore method
	static BitReader newBitReader(BitStore store, final int finalPos, final int initialPos) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (finalPos < 0) throw new IllegalArgumentException("negative finalPos");
		if (initialPos > store.size()) throw new IllegalArgumentException("initialPos too large");

		return new BitReader() {
			int pos = initialPos;

			@Override
			public long getPosition() {
				return initialPos - pos;
			}

			@Override
			public long setPosition(long newPosition) {
				if (newPosition < finalPos) {
					pos = finalPos - initialPos;
					return finalPos;
				}
				if (newPosition >= initialPos) {
					pos = 0;
					return initialPos;
				}
				pos = initialPos - (int) newPosition;
				return newPosition;
			}

			@Override
			public boolean readBoolean() throws BitStreamException {
				if (pos <= 0) throw new EndOfBitStreamException();
				return store.getBit(--pos);
			}

			@Override
			public int readBit() throws BitStreamException {
				return readBoolean() ? 1 : 0;
			}

			@Override
			public long readLong(int count) throws BitStreamException {
				if (count < 0) throw new IllegalArgumentException();
				if (count > 64) throw new IllegalArgumentException();
				pos -= count;
				if (pos < 0) throw new EndOfBitStreamException();
				return store.getBits(pos, count);
			}

			@Override
			public int read(int count) throws BitStreamException {
				if (count < 0) throw new IllegalArgumentException();
				if (count > 32) throw new IllegalArgumentException();
				pos -= count;
				if (pos < 0) throw new EndOfBitStreamException();
				return (int) store.getBits(pos, count);
			}

			@Override
			public int readUntil(boolean one) throws BitStreamException {
				//TODO efficient implementation needs next/previous bit support on BitStore
				return BitReader.super.readUntil(one);
			}

			@Override
			public BigInteger readBigInt(int count) throws BitStreamException {
				switch(count) {
				case 0 : return BigInteger.ZERO;
				case 1 : return readBoolean() ? BigInteger.ONE : BigInteger.ZERO;
				default :
					final int from = pos - count;
					if (from < 0) throw new EndOfBitStreamException();
					final int to = pos;
					pos = from;
					return store.range(from, to).toBigInteger();
				}
			}
		};
	}

	// available via default BitStore method
	static Positions newPositions(Matches matches, int position) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		if (position < 0L) throw new IllegalArgumentException();
		//TODO consider restoring dedicated size accessor on matches
		if (position > matches.store().size()) throw new IllegalArgumentException();
		return new BitStorePositions(matches, false, position);
	}

	// available via default BitStore method
	static Positions newPositions(Matches matches) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		return new BitStorePositions(matches, false, 0);
	}

	// available via default BitStore method
	static Positions newDisjointPositions(Matches matches) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		return new BitStorePositions(matches, true, 0);
	}

	// available via default BitStore method
	static Positions newDisjointPositions(Matches matches, int position) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		if (position < 0L) throw new IllegalArgumentException();
		//TODO consider restoring dedicated size accessor on matches
		if (position > matches.store().size()) throw new IllegalArgumentException();
		int p = matches.first();
		int s = matches.sequence().size();
		while (p < position) {
			p = matches.next(p + s);
		}
		return new BitStorePositions(matches, true, p);
	}

	static int compareNumeric(BitStore a, BitStore b) {
		ListIterator<Integer> as = a.ones().positions(a.size());
		ListIterator<Integer> bs = b.ones().positions(b.size());
		while (true) {
			boolean ap = as.hasPrevious();
			boolean bp = bs.hasPrevious();
			if (ap && bp) {
				int ai = as.previous();
				int bi = bs.previous();
				if (ai == bi) continue;
				return ai > bi ? 1 : -1;
			} else {
				if (ap) return  1;
				if (bp) return -1;
				return 0;
			}
		}

	}

	// expects a strictly longer than b
	static int compareLexical(BitStore a, BitStore b) {
		int aSize = a.size();
		int bSize = b.size();
		int diff = a.size() - b.size();
		ListIterator<Integer> as = a.ones().positions(aSize);
		ListIterator<Integer> bs = b.ones().positions(bSize);
		while (true) {
			boolean ap = as.hasPrevious();
			boolean bp = bs.hasPrevious();
			if (ap && bp) {
				int ai = as.previous();
				int bi = bs.previous() + diff;
				if (ai == bi) continue;
				return ai > bi ? 1 : -1;
			} else {
				return bp ? -1 : 1;
			}
		}

	}

	static boolean isAllOnes(BitStore s) {
		//TODO could use a reader?
		int size = s.size();
		for (int i = 0; i < size; i++) {
			if (!s.getBit(i)) return false;
		}
		return true;
	}

	static boolean isAllZeros(BitStore s) {
		//TODO could use a reader?
		int size = s.size();
		for (int i = 0; i < size; i++) {
			if (s.getBit(i)) return false;
		}
		return true;
	}

	//duplicated here to avoid dependencies
	static int gcd(int a, int b) {
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

	static void checkMutable() {
		throw new IllegalStateException("immutable");
	}

	static void checkMutable(boolean mutable) {
		if (!mutable) throw new IllegalStateException("immutable");
	}

	static void checkPosition(int position, int size) {
		if (position < 0L) throw new IllegalArgumentException("negative position");
		if (position > size) throw new IllegalArgumentException("position exceeds size");
	}

	static void checkBounds(int finalPos, int initialPos, int size) {
		if (finalPos < 0) throw new IllegalArgumentException("negative finalPos");
		if (initialPos < finalPos) throw new IllegalArgumentException("finalPos exceeds initialPos");
		if (initialPos > size) throw new IllegalArgumentException("initialPos exceeds size");
	}

	static void checkBitsLength(int length) {
		if (length < 0) throw new IllegalArgumentException("negative length");
		if (length > 64) throw new IllegalArgumentException("length exceeds 64");
	}

	static int adjIndex(int index, int start, int finish) {
		if (index < 0) throw new IllegalArgumentException("negative index: " + index);
		index += start;
		if (index >= finish) throw new IllegalArgumentException("index too large: " + (index - start));
		return index;
	}

	static int adjPosition(int position, int start, int finish) {
		if (position < 0) throw new IllegalArgumentException("negative position: " + position);
		position += start;
		if (position > finish) throw new IllegalArgumentException("position too large: " + (position - start));
		return position;
	}

	// private static methods

	private static void transferImpl(BitReader reader, BitWriter writer, long count) {
		while (count >= 64) {
			//TODO could benefit from reading into a larger buffer here - eg bytes?
			long bits = reader.readLong(64);
			writer.write(bits, 64);
			count -= 64;
		}
		if (count != 0L) {
			long bits = reader.readLong((int) count);
			writer.write(bits, (int) count);
		}
	}

	private static void checkSize(int size) {
		if (size < 0) throw new IllegalArgumentException("negative size");
	}

	private static void checkSize(long size, long maxSize) {
		if (size < 0L) throw new IllegalArgumentException("negative size");
		if (size > maxSize) throw new IllegalArgumentException("size exceeds maximum permitted");
	}

	// constructor

	private Bits() { }

}

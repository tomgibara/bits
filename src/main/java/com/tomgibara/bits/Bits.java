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

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.WriteStream;

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
	
	public static Hasher<BitStore> bitStoreHasher() {
		return bitStoreHasher;
	}
	
	public static Comparator<BitStore> numericalComparator() {
		return numericalComparator;
	}
	
	public static Comparator<BitStore> lexicalComparator() {
		return lexicalComparator;
	}

	// new bit store
	
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

	public static BitStore storeFromChars(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		int size = chars.length();
		BitStore store = store(size);
		transferImpl( new CharBitReader(chars), store.openWriter(), size );
		return store;
	}

	public static BitStore storeFromRandom(int size, Random random, float probability) {
		return new BitVector(random, probability, size);
	}

	public static BitStore storeFromRandom(int size, Random random) {
		return new BitVector(random, size);
	}

	public static BitStore storeFromBit(boolean bit) {
		return new Bit(bit);
	}
	
	public static BitStore storeFromLong(long bits) {
		return new LongBitStore(bits);
	}
	
	public static BitStore storeFromLong(long bits, int count) {
		return new LongBitStore(bits).range(0, count);
	}
	
	// immutable bit stores
	
	public static BitStore noBits() {
		return VoidBitStore.MUTABLE;
	}
	
	public static BitStore oneBit() {
		return ImmutableOne.INSTANCE;
	}
	
	public static BitStore zeroBit() {
		return ImmutableZero.INSTANCE;
	}
	
	public static BitStore ones(int size) {
		checkSize(size);
		return new ImmutableBits.ImmutablesOnes(size);
	}
	
	public static BitStore zeros(int size) {
		checkSize(size);
		return new ImmutableBits.ImmutablesZeros(size);
	}

	public static BitStore bit(boolean bit) {
		return ImmutableBit.instanceOf(bit);
	}
	
	public static BitStore bits(boolean ones, int size) {
		return ones ? ones(size) : zeros(size);
	}

	// bit store views
	
	public static BitStore storeOfBitSet(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException("null bitSet");
		checkSize(size);
		return new BitSetBitStore(bitSet, 0, size, true);
	}

	public static BitStore storeOfBytes(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		if (bytes.length * 8L > Integer.MAX_VALUE) throw new IllegalArgumentException("index overflow");
		return new BytesBitStore(bytes, 0, bytes.length << 3, true);
	}

	public static BitStore storeOfBytes(byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		int size = bytes.length << 3;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish < 0) throw new IllegalArgumentException("index overflow");
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BytesBitStore(bytes, offset, finish, true);
	}

	public static BitStore storeOfBooleans(boolean[] bits) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		return new BooleansBitStore(bits, 0, bits.length, true);
	}

	public static BitStore storeOfBooleans(boolean[] bits, int offset, int length) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		int size = bits.length;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish < 0) throw new IllegalArgumentException("index overflow");
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BooleansBitStore(bits, offset, finish, true);
	}
	
	public static BitStore storeOfBigInt(BigInteger bigInt) {
		if (bigInt == null) throw new IllegalArgumentException("null bigInt");
		return new BigIntegerBitStore(bigInt);
	}
	
	public static BitStore storeOfChars(CharSequence chars) {
		if (chars == null) throw new IllegalArgumentException("null chars");
		return new CharsBitStore(chars);
	}
	
	// bit streams

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

	public static BitReader readerFromChars(CharSequence chars) {
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

	public static BitReader readerFromBytes(byte[] bytes) {
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

	public static BitReader readerFromBytes(byte[] bytes, long size) {
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

	public static BitReader readerFromInts(int[] ints) {
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

	public static BitReader readerFromInts(int[] ints, long size) {
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

	public static BitReader readerFromChannel(FileChannel channel, ByteBuffer buffer) {
		if (channel == null) throw new IllegalArgumentException("null channel");
		if (buffer == null) throw new IllegalArgumentException("null buffer");
		return new FileChannelBitReader(channel, buffer);
	}

	/**
	 * A {@link BitReader} that sources its bits from an
	 * <code>InputStream</code>.
	 * 
	 * @param stream
	 *            the source input stream
	 * @return a bit reader over the input stream
	 */

	public static BitReader readerFromInput(InputStream in) {
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

	public static BitReader readerFromStream(ReadStream stream) {
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

	public static BitWriter writerToBytes(byte[] bytes) {
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

	public static BitWriter writerToBytes(byte[] bytes, long size) {
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

	public static BitWriter writerToInts(int[] ints) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		return new IntArrayBitWriter(ints);
	}

	/**
	 * Writes bits to an array of ints up-to a specified limit.
	 * 
	 * @param ints
	 *            the array of ints
	 * @param length
	 *            the greatest number of bits the writer will write to the array
	 * @return a writer that writes bits to the supplied array
	 */

	public static BitWriter writerToInts(int[] ints, long size) {
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

	public static BitWriter writerToOutput(OutputStream out) {
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

	public static BitWriter writerToStream(WriteStream stream) {
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

	public static BitWriter writerToPrintWriter(PrintStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		return new PrintStreamBitWriter(stream);
	}
	
	// exposed to assist implementors of BitStore.Op interface
	public static BitWriter writerToStore(BitStore store, Operation operation, int finalPos, int initialPos) {
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
	
	public static void transfer(BitReader reader, BitWriter writer, long count) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (writer == null) throw new IllegalArgumentException("null writer");
		if (count < 0L) throw new IllegalArgumentException("negative count");
		transferImpl(reader, writer, count);
	}
	
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
		return writerToStore(store, Operation.SET, finalPos, initialPos);
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

	static Positions newDisjointPositions(Matches matches) {
		if (matches == null) throw new IllegalArgumentException("null matches");
		return new BitStorePositions(matches, true, 0);
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

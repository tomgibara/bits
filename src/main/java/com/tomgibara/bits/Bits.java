package com.tomgibara.bits;

import java.math.BigInteger;
import java.util.BitSet;

import com.tomgibara.hashing.HashSerializer;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;

public final class Bits {

	private static final Hasher<BitStore> bitStoreHasher = bitStoreHasher((b,s) -> b.writeTo(s));

	static <B> Hasher<B> bitStoreHasher(HashSerializer<B> s) {
		return Hashing.murmur3Int().hasher(s);
	}
	
	public static Hasher<BitStore> bitStoreHasher() {
		return bitStoreHasher;
	}
	
	public static BitStore asBitStore(long bits) {
		return new LongBitStore(bits);
	}
	
	public static BitStore asBitStore(long bits, int count) {
		return new LongBitStore(bits).range(0, count);
	}
	
	public static BitStore asBitStore(BitSet bitSet, int size) {
		if (bitSet == null) throw new IllegalArgumentException("null bitSet");
		if (size < 0) throw new IllegalArgumentException("negative size");
		return new BitSetBitStore(bitSet, 0, size, true);
	}

	public static BitStore asBitStore(byte[] bytes) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		return new BytesBitStore(bytes, 0, bytes.length << 3, true);
	}

	public static BitStore asBitStore(byte[] bytes, int offset, int length) {
		if (bytes == null) throw new IllegalArgumentException("null bytes");
		int size = bytes.length << 3;
		if (offset < 0) throw new IllegalArgumentException("negative offset");
		if (length < 0) throw new IllegalArgumentException("negative length");
		int finish = offset + length;
		if (finish > size) throw new IllegalArgumentException("exceeds size");
		return new BytesBitStore(bytes, offset, finish, true);
	}
	
	public static BitStore newImmutableView(BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		return new AbstractBitStore() {

			@Override
			public int size() {
				return store.size();
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(index);
			}

			@Override
			public boolean getThenSetBit(int index, boolean value) {
				return store.getThenSetBit(index, value);
			}

			@Override
			public int countOnes() {
				return store.countOnes();
			}

			@Override
			public boolean isAll(boolean value) {
				return store.isAll(value);
			}

			@Override
			public boolean testEquals(BitStore s) {
				return store.testEquals(s);
			}

			@Override
			public boolean testIntersects(BitStore s) {
				return store.testIntersects(s);
			}

			@Override
			public boolean testContains(BitStore s) {
				return store.testContains(s);
			}

			@Override
			public int writeTo(BitWriter writer) {
				return store.writeTo(writer);
			}

			@Override
			public int hashCode() {
				return store.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				return store.equals(obj);
			}

		};
	}

	public static BitStore newRangedView(BitStore store, int from, int to) {
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
				return store.getBit(from + index);
			}

			@Override
			public boolean getThenSetBit(int index, boolean value) {
				return store.getThenSetBit(from + index, value);
			}

			@Override
			public void setStore(int index, BitStore that) {
				store.setStore(from + index, store);
			}

			@Override
			public boolean isMutable() {
				return store.isMutable();
			}

		};
	}

	public static Number asNumber(BitStore store) {
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
	public static BitWriter newBitWriter(BitStore store, int position) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (position < 0) throw new IllegalArgumentException();
		if (position > store.size()) throw new IllegalArgumentException();
		
		return new BitWriter() {

			int pos = position;

			@Override
			public int writeBit(int bit) throws BitStreamException {
				return writeBoolean((bit & 1) == 1);
			}

			@Override
			public int writeBoolean(boolean bit) throws BitStreamException {
				checkPos();
				store.setBit(--pos, bit);
				return 1;
			}

			@Override
			public long getPosition() {
				return position - pos;
			}

			private void checkPos() {
				if (pos <= 0) throw new EndOfBitStreamException();
			}
		};
	}

	public static BitReader newBitReader(BitStore store, int position) {
		if (store == null) throw new IllegalArgumentException("null store");
		if (position < 0) throw new IllegalArgumentException();
		if (position > store.size()) throw new IllegalArgumentException();
		
		return new BitReader() {
			int pos = position;
			
			@Override
			public long getPosition() {
				return position - pos;
			}
			
			@Override
			public long setPosition(long newPosition) {
				if (newPosition < 0L) throw new IllegalArgumentException();
				if (newPosition >= position) {
					pos = 0;
					return position;
				}
				pos = position - (int) newPosition;
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
	
	public static void transfer(BitReader reader, BitWriter writer, long count) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (writer == null) throw new IllegalArgumentException("null writer");
		if (count < 0L) throw new IllegalArgumentException("negative count");
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

	private Bits() { }
	
}

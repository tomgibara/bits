package com.tomgibara.bits;

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
		return new BitSetBitStore(bitSet, 0, size, true);
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

	private Bits() { }
	
}

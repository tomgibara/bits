package com.tomgibara.bits;

import java.util.BitSet;

public final class Bits {

	private Bits() { }
	
	public static BitStore asBitStore(long bits) {
		return new LongBitStore(bits);
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


}

package com.tomgibara.bits;

import java.util.BitSet;

final class BitSetBitStore implements BitStore {

	private final BitSet set;
	private final int from;
	private final int to;
	private final boolean mutable;

	BitSetBitStore(BitSet set, int from, int to, boolean mutable) {
		this.set = set;
		this.from = from;
		this.to = to;
		this.mutable = mutable;
	}

	@Override
	public int size() {
		return to - from;
	}
	
	@Override
	public boolean getBit(int index) {
		return set.get(adjIndex(index));
	}
	
	@Override
	public void clear(boolean value) {
		checkMutable();
		set.set(from, to, value);
	}
	
	@Override
	public void setBit(int index, boolean value) {
		checkMutable();
		set.set(adjIndex(index), value);
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		checkMutable();
		index = adjIndex(index);
		boolean previous = set.get(index);
		set.set(index, value);
		return previous;
	}

	@Override
	public int countOnes() {
		if (to >= set.length() && from == 0) return set.cardinality();
		return BitStore.super.countOnes();
	}
	
	@Override
	public boolean isAll(boolean value) {
		if (value) return set.nextClearBit(from) >= to;
		int i = set.nextSetBit(from);
		return i < 0 || i >= to;
	}

//	@Override
//	public int writeTo(BitWriter writer) {
//		if (writer == null) throw new IllegalArgumentException("null writer");
//		// heuristic - don't bother to copy for small bit sets
//		if (size() < 64) return BitStore.super.writeTo(writer);
//		//TODO should we risk copying potentially large bitsets?
//		int size = Math.max(set.size(), to);
//		return BitVector.fromBitSet(set, size).range(from, to).writeTo(writer);
//	}

	@Override
	public void flip() {
		checkMutable();
		set.flip(from, to);
	}
	
	@Override
	public BitSetBitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += this.from;
		to += this.from;
		if (to > this.to) throw new IllegalArgumentException();
		return new BitSetBitStore(set, from, to, mutable);
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public BitSetBitStore mutableCopy() {
		return new BitSetBitStore(set.get(from, to), 0, to - from, true);
	}
	
	@Override
	public BitStore immutableCopy() {
		return new BitSetBitStore(set.get(from, to), 0, to - from, false);
	}
	
	@Override
	public BitStore immutableView() {
		return new BitSetBitStore(set, from, to, false);
	}
	
	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}
	
	private int adjIndex(int index) {
		if (index < 0) throw new IllegalArgumentException();
		index += from;
		if (index >= to) throw new IllegalArgumentException();
		return index;
	}
}

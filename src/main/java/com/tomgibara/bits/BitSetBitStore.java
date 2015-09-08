package com.tomgibara.bits;

import java.util.BitSet;

final class BitSetBitStore implements BitStore {

	private final BitSet set;
	private final int start;
	private final int finish;
	private final boolean mutable;

	BitSetBitStore(BitSet set, int start, int finish, boolean mutable) {
		this.set = set;
		this.start = start;
		this.finish = finish;
		this.mutable = mutable;
	}

	@Override
	public int size() {
		return finish - start;
	}
	
	@Override
	public boolean getBit(int index) {
		return set.get(adjIndex(index));
	}
	
	@Override
	public void clear(boolean value) {
		checkMutable();
		set.set(start, finish, value);
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
		if (finish >= set.length() && start == 0) return set.cardinality();
		return BitStore.super.countOnes();
	}
	
	@Override
	public boolean isAll(boolean value) {
		if (value) return set.nextClearBit(start) >= finish;
		int i = set.nextSetBit(start);
		return i < 0 || i >= finish;
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
		set.flip(start, finish);
	}
	
	@Override
	public BitSetBitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BitSetBitStore(set, from, to, mutable);
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public BitSetBitStore mutableCopy() {
		return new BitSetBitStore(set.get(start, finish), 0, finish - start, true);
	}
	
	@Override
	public BitStore immutableCopy() {
		return new BitSetBitStore(set.get(start, finish), 0, finish - start, false);
	}
	
	@Override
	public BitStore immutableView() {
		return new BitSetBitStore(set, start, finish, false);
	}
	
	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}
	
	private int adjIndex(int index) {
		if (index < 0) throw new IllegalArgumentException();
		index += start;
		if (index >= finish) throw new IllegalArgumentException();
		return index;
	}
}

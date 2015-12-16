package com.tomgibara.bits;

abstract class SingleBitStore implements BitStore {

	abstract boolean getBit();

	// bit store methods
	
	@Override
	public int size() {
		return 1;
	}
	
	@Override
	public BitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to > 1) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		return from == to ? Bits.noBits() : this;
	}
	
	@Override
	public BitStore reversed() {
		return this;
	}

	// mutability methods
	
	@Override
	public Bit mutableCopy() {
		return new Bit(getBit());
	}

	@Override
	public BitStore immutableCopy() {
		return ImmutableBit.instanceOf(getBit());
	}

	// object methods
	
	@Override
	public int hashCode() {
		return getBit() ? -463810133 : 1364076727; // precomputed
	}
	
	@Override
	public String toString() {
		return getBit() ? "1" : "0";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore)) return false;
		BitStore that = (BitStore) obj;
		if (that.size() != 1) return false;
		return that.getBit(0) == getBit();
	}

	// package scoped methods
	
	void checkIndex(int index) {
		if (index != 0) throw new IllegalArgumentException("invalid index");
	}


}

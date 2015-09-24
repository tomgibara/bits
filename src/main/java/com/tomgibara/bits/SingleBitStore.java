package com.tomgibara.bits;

abstract class SingleBitStore implements BitStore {

	abstract boolean getBit();
	
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
}

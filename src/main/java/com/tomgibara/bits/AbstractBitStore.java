package com.tomgibara.bits;

abstract class AbstractBitStore implements BitStore {

	public int hashCode() {
		return Bits.bitStoreHasher().intHashValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore)) return false;
		BitStore that = (BitStore) obj;
		if (this.size() != that.size()) return false;
		if (!this.testEquals(that)) return false;
		return true;
	}

	public String toString() {
		int size = size();
		switch (size) {
		case 0 : return "";
		case 1 : return getBit(0) ? "1" : "0";
		default:
			StringBuilder sb = new StringBuilder(size);
			for (int i = size - 1; i >= 0; i--) {
				sb.append( getBit(i) ? '1' : '0');
			}
			return sb.toString();
		}
	}

}

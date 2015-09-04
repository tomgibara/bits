package com.tomgibara.bits;

//TODO make public if/when ready
class LongBits implements BitStore {

	// statics
	
	private static long asLong(BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size > 64) throw new IllegalArgumentException("store size greater than 64");
		long bits = 0L;
		for (int i = size - 1; i >= 0; i--) {
			bits <<= 1;
			if (store.getBit(i)) bits |= 1L;
		}
		return bits;
	}
	
	// fields

	private long bits;
	
	// constructors
	
	public LongBits() {
		bits = 0L;
	}
	
	public LongBits(long bits) {
		this.bits = bits;
	}
	
	public LongBits(BitStore store) {
		this.bits = asLong(store);
	}
	
	// methods
	
	public long longValue() {
		return bits;
	}
	
	// bitstore methods
	
	@Override
	public int size() {
		return 64;
	}
	
	@Override
	public boolean getBit(int index) {
		return (1L << index & bits) != 0L;
	}
	
	@Override
	public void clear(boolean value) {
		bits = value ? -1L : 0L;
	}

	@Override
	public void setBit(int index, boolean value) {
		long mask = 1L << index;
		if (value) {
			bits |= mask;
		} else {
			bits &= ~mask;
		}
	}
	
	@Override
	public boolean getThenSetBit(int index, boolean value) {
		long mask = 1L << index;
		long masked = mask & bits;
		if (masked == 0L) {
			// setting a zero bit to one
			if (value) bits |= mask;
			return false;
		} else {
			// setting a one bit to zero
			if (!value) bits &= ~mask;
			return true;
		}
	}
	
	@Override
	public void setStore(int index, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size == 0) return;
		int to = index + size;
		if (to > 64) throw new IllegalArgumentException("store size too great");
		long acc = 0L;
		for (int i = size - 1; i >= 0; i++) {
			acc <<= 1;
			if (store.getBit(i)) acc |= 1L;
		}
		long mask = (1 << size) - 1L;
		acc <<= index;
		mask <<= index;
		bits = (bits & ~mask) | acc;
	}

	@Override
	public int countOnes() {
		return Long.bitCount(bits);
	}
	
	@Override
	public boolean isAll(boolean value) {
		return value ? bits == -1L : bits == 0L;
	}
	
	@Override
	public boolean testEquals(BitStore store) {
		return bits == asLong(store);
	}
	
	@Override
	public boolean testIntersects(BitStore store) {
		return (bits & asLong(store)) != 0L;
	}
	
	@Override
	public boolean testContains(BitStore store) {
		return (~bits & asLong(store)) == 0L;
	}
	
	@Override
	public int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return writer.write(bits, 64);
	}
	
	@Override
	public void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		bits = reader.readLong(64);
	}
	
	@Override
	public void flip() {
		bits = ~bits;
	}
	
	// mutability methods

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public LongBits mutableCopy() {
		return new LongBits(bits);
	}

	@Override
	public String toString() {
		String str = Long.toBinaryString(bits);
		int pad = 64 - str.length();
		if (pad == 0) return str;
		StringBuilder sb = new StringBuilder(64);
		while (pad-- > 0) sb.append('0');
		sb.append(str);
		return sb.toString();
	}
}

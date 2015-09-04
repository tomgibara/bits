package com.tomgibara.bits;

import com.tomgibara.fundament.Mutability;

public interface BitStore extends Mutability<BitStore> {
	
	int size();
	
	boolean getBit(int index);
	
	default void setBit(int index, boolean value) {
		throw new IllegalStateException("immutable");
	}
	
	default void clear(boolean value) {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, value);
		}
	}
	
	default boolean getThenSetBit(int index, boolean value) {
		boolean previous = getBit(index);
		if (previous != value) setBit(index, value);
		return previous;
	}

	default void setStore(int index, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int to = index + store.size();
		if (to > size()) throw new IllegalArgumentException("store size too great");
		for (int i = index; i < to; i++) {
			setBit(index, store.getBit(i - index));
		}
	}
	
	default int countOnes() {
		int size = size();
		int count = 0;
		for (int i = 0; i < size; i++) {
			if (getBit(i)) count++;
		}
		return count;
	}
	
	default boolean isAll(boolean value) {
		int size = size();
		for (int i = 0; i < size; i++) {
			if (getBit(i) != value) return false;
		}
		return true;
	}
	
	default boolean testEquals(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (this.getBit(i) != store.getBit(i)) return false;
		}
		return true;
	}
	
	default boolean testIntersects(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (this.getBit(i) && store.getBit(i)) return true;
		}
		return false;
	}
	
	default boolean testContains(BitStore store) {
		int size = size();
		if (store.size() != size) throw new IllegalArgumentException("mismatched size");
		for (int i = 0; i < size; i++) {
			if (!this.getBit(i) && store.getBit(i)) return false;
		}
		return true;
	}
	
	default int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		int size = size();
		for (int i = 0; i < size; i++) {
			writer.writeBoolean(getBit(i));
		}
		return size;
	}
	
	default void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, reader.readBoolean());
		}
	}

	default void flip() {
		int size = size();
		for (int i = 0; i < size; i++) {
			setBit(i, !getBit(i));
		}
	}
}

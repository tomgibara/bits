package com.tomgibara.bits;

class FlippedBitStore extends AbstractBitStore {

	private final BitStore store;
	private final int size;

	public FlippedBitStore(BitStore store) {
		this.store = store;
		size = store.size();
	}

	// fundamentals
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean getBit(int index) {
		return !store.getBit(index);
	}
	
	@Override
	public void setBit(int index, boolean value) {
		store.setBit(index, !value);
	}
	
	// acceleration
	
	@Override
	public long getBits(int position, int length) {
		return ~store.getBits(position, length);
	}
	
	@Override
	public void setBits(int position, long value, int length) {
		store.setBits(position, ~value, length);
	}
	
	@Override
	public boolean getThenSetBit(int index, boolean value) {
		return !store.getThenSetBit(index, !value);
	}
	
	@Override
	public void flipBit(int index) {
		store.flipBit(index);
	}
	
	@Override
	public void clearWithOnes() {
		store.clearWithZeros();
	}
	
	@Override
	public void clearWithZeros() {
		store.clearWithOnes();
	}
	
	@Override
	public void flip() {
		store.flip();
	}

	// views

	@Override
	public BitStore range(int from, int to) {
		return store.range(from, to).flipped();
	}
	
	@Override
	public BitStore flipped() {
		return store;
	}

	// mutability
	
	@Override
	public boolean isMutable() {
		return store.isMutable();
	}
	
}

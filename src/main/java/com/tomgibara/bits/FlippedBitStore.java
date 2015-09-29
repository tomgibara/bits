package com.tomgibara.bits;

//TODO support mutability
class FlippedBitStore extends AbstractBitStore {

	private final BitStore store;
	private final int size;

	public FlippedBitStore(BitStore store) {
		this.store = store;
		size = store.size();
	}

	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean getBit(int index) {
		return !store.getBit(index);
	}
	
	@Override
	public long getBits(int position, int length) {
		return ~store.getBits(position, length);
	}

}

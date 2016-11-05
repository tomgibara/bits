package com.tomgibara.bits;

class EnlargedBitStore extends AbstractBitStore {

	private final BitStore store;

	private final int finish; // of first zeros
	private final int start; // of second zeros
	private final int size; // total size

	EnlargedBitStore(BitStore store, int left, int right) {
		this.store = store;
		finish = right;
		start = finish + store.size();
		size = start + left;
	}

	// fundamental methods

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean getBit(int index) {
		if (index < 0 || index > size) throw new IllegalArgumentException("invalid index");
		return index >= finish && index < start && store.getBit(index - finish);
	}

	// accelerating methods

	@Override
	public long getBits(int position, int length) {
		if (position < 0) throw new IllegalArgumentException("negative position");
		int limit = position + length;
		if (limit > size) throw new IllegalArgumentException("length too great");
		if (position >= start) return 0L;
		if (limit < finish) return 0L;
		if (position >= finish && limit <= start) return store.getBits(position - finish, length);
		return super.getBits(position, length);
	}

	// view methods

	@Override
	public BitStore range(int from, int to) {
		if (from <= finish && to <= finish) return Bits.zeroBits(size).range(from, to);
		if (from >= start && to >= start) return Bits.zeroBits(size).range(from, to);
		if (from >= finish && to <= start) return store.range(from - finish, to - finish);
		return super.range(from, to);
	}
}

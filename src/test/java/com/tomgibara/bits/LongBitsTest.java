package com.tomgibara.bits;

public class LongBitsTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return newStore(0L, size);
	}
	
	@Override
	BitStore randomStore(int size) {
		return newStore(random.nextLong(), size);
	}

	private BitStore newStore(long bits, int size) {
		if (size > 64) throw new IllegalArgumentException();
		BitStore store = BitStore.asBitStore(bits);
		return size < 64 ? store.range(0, size) : store;
	}
	
	@Override
	int validSize(int suggested) {
		return 64;
	}

}

package com.tomgibara.bits;

public class LongBitsTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		if (size != 64) throw new IllegalArgumentException();
		return new LongBits();
	}

	@Override
	int validSize(int suggested) {
		return 64;
	}

}

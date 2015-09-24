package com.tomgibara.bits;

public class BooleansBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return new BooleansBitStore(new boolean[size], 0, size, true);
	}

}

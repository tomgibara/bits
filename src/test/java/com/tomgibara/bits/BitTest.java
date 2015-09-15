package com.tomgibara.bits;

public class BitTest extends BitStoreTest {

	@Override
	int validSize(int suggested) {
		return 1;
	}
	
	@Override
	BitStore newStore(int size) {
		return new Bit(false);
	}

}

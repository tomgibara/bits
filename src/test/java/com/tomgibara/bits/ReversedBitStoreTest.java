package com.tomgibara.bits;

public class ReversedBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return new BitVector(size).reversed();
	}

}

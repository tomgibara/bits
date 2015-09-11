package com.tomgibara.bits;


public class BytesBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		byte[] bytes = new byte[(size + 7) >> 3];
		return Bits.asBitStore(bytes, 0, size);
	}
	
	@Override
	BitStore randomStore(int size) {
		byte[] bytes = new byte[(size + 7) >> 3];
		random.nextBytes(bytes);
		return Bits.asBitStore(bytes, 0, size);
	}

}

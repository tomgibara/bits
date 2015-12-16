package com.tomgibara.bits;

public class CharsBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		StringBuilder chars = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			chars.append('0');
		}
		return new CharsBitStore(chars);
	}

}

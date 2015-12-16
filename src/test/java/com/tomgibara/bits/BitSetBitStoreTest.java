package com.tomgibara.bits;

import java.util.BitSet;

public class BitSetBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return Bits.storeOfBitSet(new BitSet(), size);
	}

}

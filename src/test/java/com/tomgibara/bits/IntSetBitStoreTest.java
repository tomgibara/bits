package com.tomgibara.bits;

import java.util.TreeSet;

public class IntSetBitStoreTest extends BitStoreTest {

	@Override
	BitStore newStore(int size) {
		return new IntSetBitStore(new TreeSet<Integer>().subSet(0, size), 0, size, true);
	}
	
	public void testSparsity() {
		int size = 10000000;
		BitStore s = newStore(size);
		BitStore t = newStore(size);
		s.setBit(0, true);
		t.setBit(size - 1, true);
		s.or().withStore(t);
		assertTrue(s.getBit(0));
		assertTrue(s.getBit(size - 1));
	}

}

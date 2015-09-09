package com.tomgibara.bits;

import org.junit.Assert;

public class LongBitStoreTest extends BitStoreTest {

	@Override
	int validSize(int suggested) {
		return Math.min(suggested, 64);
	}

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
		BitStore store = Bits.asBitStore(bits);
		return size < 64 ? store.range(0, size) : store;
	}

	public void testLongToByteArray() {
		Assert.assertArrayEquals(
				new byte[] {0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x00},
				Bits.asBitStore(0x7766554433221100L).toByteArray()
				);
	}
}

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

	public void testBitShift() {
		{
			BitStore bit = Bits.asBitStore(true);
			bit.shift(0, false);
			assertTrue(bit.getBit(0));
		}
		{
			BitStore bit = Bits.asBitStore(true);
			bit.shift(1, false);
			assertFalse(bit.getBit(0));
		}
	}
}

package com.tomgibara.bits;

public class BitStoreBitReaderTest extends AbstractBitReaderTest {

	@Override
	BitReader readerFor(BitVector vector) {
		return canon(vector).openReader();
	}

	private BitStore canon(BitStore store) {
		return new AbstractBitStore() {

			@Override
			public int size() {
				return store.size();
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(size() - 1 - index);
			}
		};
	}

}

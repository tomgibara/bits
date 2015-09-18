package com.tomgibara.bits;

import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitVector;

public class BitVectorBitReaderTest extends AbstractBitReaderTest {

	BitReader readerFor(BitVector vector) {
		BitVector copy = vector.mutableCopy();
		copy.permute().reverse();
		return copy.openReader();
	}

}

package com.tomgibara.bits;

import java.util.WeakHashMap;

import com.tomgibara.bits.BitBoundary;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.BitWriter;

public class BitVectorBitWriterTest extends AbstractBitWriterTest {

	private final WeakHashMap<BitWriter, BitVector> map = new WeakHashMap<BitWriter, BitVector>();

	@Override
	BitWriter newBitWriter(long size) {
		BitVector vector = new BitVector((int) size);
		BitWriter writer = vector.openWriter();
		map.put(writer, vector);
		return writer;
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		BitVector vector = map.get(writer);
		return vector.openReader();
	}

	@Override
	BitBoundary getBoundary() {
		return BitBoundary.BIT;
	}

}

package com.tomgibara.bits;

public class BitVectorWriterFixedTest extends AbstractBitWriterTest {

	@Override
	BitWriter newBitWriter(long size) {
		return new BitVectorWriter((int) size);
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		BitVector vector = ((BitVectorWriter) writer).toBitVector();
		return vector.openReader();
	}

	@Override
	BitBoundary getBoundary() {
		return BitBoundary.BIT;
	}
	
	public void testStripes() {
		BitWriter writer = newBitWriter(0);
		for (int i = 0; i < 10; i++) {
			writer.writeBooleans(true,  10);
			writer.writeBooleans(false, 10);
		}

		BitReader reader = bitReaderFor(writer);
		for (int i = 0; i < 10 * 20; i++) {
			boolean b = reader.readBoolean();
			assertEquals("At index " + i, (i / 10 & 1) == 0, b);
		}
	}

}

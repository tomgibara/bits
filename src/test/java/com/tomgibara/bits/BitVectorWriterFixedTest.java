package com.tomgibara.bits;

public class BitVectorWriterFixedTest extends AbstractBitWriterTest {

	@Override
	BitVectorWriter newBitWriter(long size) {
		return new BitVectorWriter((int) size);
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		BitVector vector = ((BitVectorWriter) writer).toImmutableBitVector();
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

	public void testToMutableBitVector() {
		BitVectorWriter writer = newBitWriter(0);
		writer.writeBoolean(true);
		BitVector vector = writer.toMutableBitVector();
		// bit vector really is mutable
		vector.set(false);
		// cannot write
		try {
			writer.writeBoolean(true);
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
		// cannot pad
		try {
			writer.padToBoundary(BitBoundary.BYTE);
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
		// cannot convert again
		try {
			writer.toMutableBitVector();
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
		// position remains available
		assertEquals(1, writer.getPosition());
	}
}

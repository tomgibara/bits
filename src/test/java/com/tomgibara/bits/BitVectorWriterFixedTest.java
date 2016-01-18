/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tomgibara.bits;

import java.util.Map;
import java.util.WeakHashMap;

public class BitVectorWriterFixedTest extends AbstractBitWriterTest {

	private Map<BitWriter, GrowableBits> lookup = new WeakHashMap<>();

	@Override
	BitWriter newBitWriter(long size) {
		GrowableBits growable = Bits.growableBits((int) size);
		BitWriter writer = growable.writer();
		lookup.put(writer, growable);
		return writer;
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		return lookup.get(writer).toImmutableBitVector().openReader();
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
		GrowableBits growable = Bits.growableBits(0);
		BitWriter writer = growable.writer();
		writer.writeBoolean(true);
		BitVector vector = growable.toMutableBitVector();
		// bit vector really is mutable
		vector.clear();
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
			growable.toMutableBitVector();
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
		// position remains available
		assertEquals(1, writer.getPosition());
	}
}

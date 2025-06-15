/*
 * Copyright 2011 Tom Gibara
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class IntArrayBitWriterTest extends AbstractBitWriterTest {

	@Override
	IntArrayBitWriter newBitWriter(long size) {
		return new IntArrayBitWriter(new int[(int) ((size + 31) / 32)], size);
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		IntArrayBitWriter mw = (IntArrayBitWriter) writer;
		return new IntArrayBitReader(mw.getInts(), mw.getSize());
	}

	@Override
	BitBoundary getBoundary() {
		return BitBoundary.BIT;
	}

	@Test
	public void testBitOrder() {
		testBitOrder("11111111111111110000000000000000");
		testBitOrder("11111111000000001111111100000000");
		testBitOrder("11110000111100001111000011110000");
		testBitOrder("11001100110011001100110011001100");
		testBitOrder("10101010101010101010101010101010");
	}

	private void testBitOrder(String binary) {
		IntArrayBitWriter writer = newBitWriter(32);
		new BitVector(binary).writeTo(writer);
		writer.flush();
		int[] ints = writer.getInts();
		assertEquals(bite(binary.substring(0,   8)), (byte) (ints[0] >> 24));
		assertEquals(bite(binary.substring(8,  16)), (byte) (ints[0] >> 16));
		assertEquals(bite(binary.substring(16, 24)), (byte) (ints[0] >>  8));
		assertEquals(bite(binary.substring(24, 32)), (byte) (ints[0]      ));
	}


}

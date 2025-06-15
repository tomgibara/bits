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

public abstract class AbstractByteBasedBitWriterTest extends AbstractBitWriterTest {

	@Override
	BitBoundary getBoundary() {
		return BitBoundary.BYTE;
	}

	abstract ByteBasedBitWriter newBitWriter(long size);

	abstract byte[] getWrittenBytes(BitWriter writer);

	@Test
	public void testBitOrder() {
		testBitOrder("1111111100000000");
		testBitOrder("1111000011110000");
		testBitOrder("1100110011001100");
		testBitOrder("1010101010101010");
	}

	private void testBitOrder(String binary) {
		ByteBasedBitWriter writer = newBitWriter(16);
		new BitVector(binary).writeTo(writer);
		byte[] bytes = getWrittenBytes(writer);
		assertEquals(bite(binary.substring(0, 8)), bytes[0]);
		assertEquals(bite(binary.substring(8, 16)), bytes[1]);
	}

}

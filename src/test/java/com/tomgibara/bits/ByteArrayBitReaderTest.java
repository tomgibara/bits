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

import java.util.Random;


public class ByteArrayBitReaderTest extends AbstractBitReaderTest {

	@Override
	ByteArrayBitReader readerFor(BitVector vector) {
		vector = vector.mutableCopy();
		vector.permute().reverse();
		return new ByteArrayBitReader(vector.toByteArray(), vector.size());
	}

	public void testBitOrder() {
		{
			BitReader r = new BitVector("10110010").openReader();
			assertEquals(0b10110010, r.read(8));
			BitReader s = Bits.readerFrom(new byte[] { (byte) 0b10110010 });
			assertEquals(0b10110010, s.read(8));
		}
		{
			BitReader r = new BitVector("1011001001001101").openReader();
			assertEquals(0b10110010, r.read(8));
			assertEquals(0b01001101, r.read(8));
			BitReader s = Bits.readerFrom(new byte[] { (byte) 0b10110010, (byte) 0b01001101 });
			assertEquals(0b10110010, s.read(8));
			assertEquals(0b01001101, s.read(8));
		}
		
		byte[] bytes = new byte[4];
		new Random(0L).nextBytes(bytes);
		byte[] copy = { bytes[3], bytes[2], bytes[1], bytes[0] };
		BitReader r = Bits.asStore(copy).openReader();
		BitReader s = Bits.readerFrom(bytes);
		assertTrue(equal(r, s));
	}
	
	public void testSizing() {
		byte[] bytes = Bits.ones(16).toByteArray();
		for (int j = 0; j < 16; j++) {
			BitStore bits = Bits.asStore(bytes).rangeTo(j);
			BitReader reader = Bits.readerFrom(bytes, j);
			// check advancing by reading
			for (int i = 0; i < j; i++) {
				reader.readBit();
			}
			try {
				reader.readBit();
				fail();
			} catch (EndOfBitStreamException e) {
				/* expected */
			}
			// check advancing multiple
			if (j > 0) {
				reader.setPosition(0);
				for (int i = 0; i < j - 1; i++) {
					reader.readBit();
				}
				try {
					reader.read(2);
					fail();
				} catch (EndOfBitStreamException e) {
					/* expected */
				}
			}
			// check set position
			reader.setPosition(0);
			assertEquals(j/2, reader.setPosition(j/2));
			reader.setPosition(24);
			assertEquals(j, reader.getPosition());
			// check read zeros
			if (j > 0) {
				int i = j - 1;
				// adjust for difference in bit ordering
				if (j <= 8) {
					bytes[0] = (byte) ~(1 << (7-i));
				} else {
					bytes[1] = (byte) ~(1 << (15-i));
				}
				// renew reader, bits have changed
				reader = Bits.readerFrom(bytes, j);
				assertEquals(i, reader.readUntil(false));
				assertEquals(j, reader.getPosition());
				bytes[0] = (byte) 0xff;
				bytes[1] = (byte) 0xff;
			}
		}
	}
	
}

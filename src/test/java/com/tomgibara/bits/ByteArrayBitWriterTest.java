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


public class ByteArrayBitWriterTest extends AbstractByteBasedBitWriterTest {

	@Override
	ByteBasedBitWriter newBitWriter(long size) {
		return new ByteArrayBitWriter(new byte[(int) ((size + 7) / 8)], size);
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		return new ByteArrayBitReader(getWrittenBytes((ByteArrayBitWriter) writer));
	}

	@Override
	byte[] getWrittenBytes(BitWriter writer) {
		return ((ByteArrayBitWriter) writer).getBytes();
	}

	public void testLength() {
		for (int i = 0; i <= 16; i++) {
			ByteBasedBitWriter writer = newBitWriter(i);
			try {
				writer.writeBooleans(false, i + 1);
				fail();
			} catch (EndOfBitStreamException e) {
				/* expected */
			}
			for (int j = 0; j < i; j++) {
				writer.writeBit(0);
			}
			try {
				writer.writeBoolean(false);
				fail();
			} catch (EndOfBitStreamException e) {
				/* expected */
			}
		}
	}

}

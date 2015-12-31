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

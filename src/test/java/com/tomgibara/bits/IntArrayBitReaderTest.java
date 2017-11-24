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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntArrayBitReaderTest extends AbstractBitReaderTest {

	@Override
	//TODO support arbitrary length vectors
	BitReader readerFor(BitStore vector) {
		vector = vector.mutableCopy();
		vector.permute().reverse();
		byte[] bytes = vector.toByteArray();
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		final IntBuffer intBuffer = byteBuffer.asIntBuffer();
		int[] ints = new int[ intBuffer.capacity() ];
		intBuffer.get(ints);
		IntArrayBitReader reader = new IntArrayBitReader(ints);
		if (reader.getSize() != vector.size()) throw new IllegalStateException("possibly passed vector that doesn't match int size?");
		return reader;
	}

}

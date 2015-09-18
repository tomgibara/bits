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

import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.ByteArrayBitReader;


public class ByteArrayBitReaderTest extends AbstractBitReaderTest {

	@Override
	ByteArrayBitReader readerFor(BitVector vector) {
		vector = vector.mutableCopy();
		vector.permute().reverse();
		return new ByteArrayBitReader(vector.toByteArray());
	}

}

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class LongBitStoreTest extends BitStoreTest {

	@Override
	int validSize(int suggested) {
		return Math.min(suggested, 64);
	}

	@Override
	BitStore newStore(int size) {
		return newStore(0L, size);
	}

	@Override
	BitStore randomStore(int size) {
		return newStore(random.nextLong(), size);
	}

	private BitStore newStore(long bits, int size) {
		if (size > 64) throw new IllegalArgumentException();
		BitStore store = Bits.toStore(bits);
		return size < 64 ? store.range(0, size) : store;
	}

	public void testLongToByteArray() {
		assertArrayEquals(
				new byte[] {0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x00},
				Bits.toStore(0x7766554433221100L).toByteArray()
				);
	}
}

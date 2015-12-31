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

public class BitTest extends BitStoreTest {

	@Override
	int validSize(int suggested) {
		return 1;
	}
	
	@Override
	BitStore newStore(int size) {
		return new Bit(false);
	}

	public void testBitShift() {
		{
			BitStore bit = Bits.storeFromBit(true);
			bit.shift(0, false);
			assertTrue(bit.getBit(0));
		}
		{
			BitStore bit = Bits.storeFromBit(true);
			bit.shift(1, false);
			assertFalse(bit.getBit(0));
		}
	}
}

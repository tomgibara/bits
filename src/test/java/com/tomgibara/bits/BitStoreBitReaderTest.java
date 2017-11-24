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

public class BitStoreBitReaderTest extends AbstractBitReaderTest {

	@Override
	BitReader readerFor(BitStore vector) {
		return canon(vector).openReader();
	}

	private BitStore canon(BitStore store) {
		return new AbstractBitStore() {

			@Override
			public int size() {
				return store.size();
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(size() - 1 - index);
			}
		};
	}

}

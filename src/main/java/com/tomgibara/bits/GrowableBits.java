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

public class GrowableBits {

	private final BitVectorWriter writer;
	
	GrowableBits(BitVectorWriter writer) {
		this.writer = writer;
	}
	
	public BitWriter writer() {
		return writer;
	}

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * continue to be written to after this method has been called.
	 *
	 * @return an immutable {@link BitVector} containing the written bits
	 */

	public BitVector toImmutableBitVector() {
		return writer.toImmutableBitVector();
	}

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * not be written to after this method has been called. Attempting to do so
	 * will raise an <code>IllegalStateException</code>.
	 *
	 * @return a mutable {@link BitVector} containing the written bits
	 */

	public BitVector toMutableBitVector() {
		return writer.toMutableBitVector();
	}
}

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

import java.util.Random;

import com.tomgibara.bits.BitStore.Permutes;

//TODO could just use a new BitStorePermutes instance
class ImmutablePermutes implements Permutes {

	static final ImmutablePermutes INSTANCE = new ImmutablePermutes();
	
	private ImmutablePermutes() { }
	
	@Override
	public void transpose(int i, int j) {
		failMutable();
	}

	@Override
	public void rotate(int distance) {
		failMutable();
	}

	@Override
	public void reverse() {
		failMutable();
	}

	@Override
	public void shuffle(Random random) {
		failMutable();
	}

	private void failMutable() {
		throw new IllegalStateException("immutable");
	}

}

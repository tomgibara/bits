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

import java.util.Arrays;

//TODO optimize further
public class BytesBitStore extends AbstractBitStore {

	// statics
	
	private static final byte[] NO_BITS = new byte[0];
	
	private static final int ADDRESS_BITS = 3;
	private static final int ADDRESS_SIZE = 1 << ADDRESS_BITS;
	private static final int ADDRESS_MASK = ADDRESS_SIZE - 1;

	// fields
	
	private final byte[] bits;
	private final int start;
	private final int finish;
	private final boolean mutable;
	
	// constructors
	
	BytesBitStore(byte[] bits, int start, int finish, boolean mutable) {
		this.bits = bits;
		this.start = start;
		this.finish = finish;
		this.mutable = mutable;
	}
	
	// fundamentals
	
	@Override
	public int size() {
		return finish - start;
	}

	@Override
	public boolean getBit(int index) {
		index = adjIndex(index);
		int i = index >> ADDRESS_BITS;
		int m = 1 << (index & ADDRESS_MASK);
		return (bits[i] & m) != 0;
	}

	@Override
	public void setBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutability();
		
		int i = index >> ADDRESS_BITS;
		int m = 1 << (index & ADDRESS_MASK);
		if (value) {
			bits[i] |= m;
		} else {
			bits[i] &= ~m;
		}
	}
	
	// accelerators
	
	@Override
	public boolean getThenSetBit(int index, boolean value) {
		index = adjIndex(index);
		checkMutability();
		
		int i = index >> ADDRESS_BITS;
		int m = 1 << (index & ADDRESS_MASK);
		boolean v = (bits[i] & m) != 0;
		if (value) {
			bits[i] |= m;
		} else {
			bits[i] &= ~m;
		}
		return v;
	}
	
	// views
	
	@Override
	public BitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		from += start;
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		return new BytesBitStore(bits, from, to, mutable);
	}
	
	// mutability
	
	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public BitStore mutableCopy() {
		return copyAdj(start, finish, true);
	}

	@Override
	public BitStore immutableCopy() {
		return copyAdj(start, finish, false);
	}

	@Override
	public BitStore immutableView() {
		return new BytesBitStore(bits, start, finish, false);
	}

	// private helper methods
	
	private int adjIndex(int index) {
		return Bits.adjIndex(index, start, finish);
	}
	
	private BytesBitStore copyAdj(int from, int to, boolean mutable) {
		if (start == finish) return new BytesBitStore(NO_BITS, from, to, mutable);
		int i = start >> ADDRESS_BITS;
		int j = (finish - 1) >> ADDRESS_BITS;
		byte[] bytes = Arrays.copyOfRange(bits, i, j + 1);
		int k = start & ~ADDRESS_MASK;
		return new BytesBitStore(bytes, from - k, to - k, mutable);
	}
	
	private void checkMutability() {
		if (!mutable) throw new IllegalStateException("immutable");
	}
}

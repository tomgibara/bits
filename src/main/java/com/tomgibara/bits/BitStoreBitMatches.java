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

import java.util.SortedSet;

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;

//TODO implement a special sparse version
abstract class BitStoreBitMatches implements BitStore.BitMatches {

	final BitStore s;
	
	BitStoreBitMatches(BitStore s) {
		this.s = s;
	}

	@Override
	public BitStore store() {
		return s;
	}
	
	public Positions positions() {
		return Bits.newPositions(this, 0);
	}

	public Positions positions(int position) {
		return Bits.newPositions(this, position);
	}

	@Override
	public Positions disjointPositions() {
		return Bits.newDisjointPositions(this);
	}

	@Override
	public SortedSet<Integer> asSet() {
		return new BitStoreSet(this, 0);
	}

	static final class Ones extends BitStoreBitMatches {

		Ones(BitStore s) {
			super(s);
		}
		
		@Override
		public boolean bit() {
			return true;
		}
		
		@Override
		public ImmutableOne sequence() {
			return ImmutableOne.INSTANCE;
		}
		
		@Override
		public BitMatches range(int from, int to) {
			return s.range(from, to).ones();
		}
		
		@Override
		public boolean isAll() {
			return Bits.isAllOnes(s);
		}
		
		@Override
		public boolean isNone() {
			return Bits.isAllZeros(s);
		}

		@Override
		public int count() {
			//TODO could use a reader?
			int size = s.size();
			int count = 0;
			for (int i = 0; i < size; i++) {
				if (s.getBit(i)) count++;
			}
			return count;
		}

		@Override
		public int first() {
			int size = s.size();
			int i;
			for (i = 0; i < size && !s.getBit(i); i++);
			return i;
		}

		@Override
		public int last() {
			int size = s.size();
			int i;
			for (i = size - 1; i >= 0 && !s.getBit(i); i--);
			return i;
		}

		@Override
		public int next(int position) {
			return position + s.range(position, s.size()).ones().first();
		}

		@Override
		public int previous(int position) {
			return s.range(0, position).ones().last();
		}

		@Override
		public void replaceAll(boolean bits) {
			if (!bits) s.fillWithZeros();
		}
	}
	
	
	static final class Zeros extends BitStoreBitMatches {

		Zeros(BitStore s) {
			super(s);
		}
		
		@Override
		public boolean bit() {
			return false;
		}
		
		@Override
		public ImmutableZero sequence() {
			return ImmutableZero.INSTANCE;
		}
		
		@Override
		public BitMatches range(int from, int to) {
			return s.range(from, to).zeros();
		}
		
		@Override
		public boolean isAll() {
			return Bits.isAllZeros(s);
		}
		
		@Override
		public boolean isNone() {
			return Bits.isAllOnes(s);
		}

		@Override
		public int count() {
			//TODO could use a reader?
			int size = s.size();
			int count = 0;
			for (int i = 0; i < size; i++) {
				if (!s.getBit(i)) count++;
			}
			return count;
		}

		@Override
		public int first() {
			int size = s.size();
			int i;
			for (i = 0; i < size && s.getBit(i); i++);
			return i;
		}

		@Override
		public int last() {
			int size = s.size();
			int i;
			for (i = size - 1; i >= 0 && s.getBit(i); i--);
			return i;
		}

		@Override
		public int next(int position) {
			return position + s.range(position, s.size()).zeros().first();
		}

		@Override
		public int previous(int position) {
			return s.range(0, position).zeros().last();
		}

		@Override
		public void replaceAll(boolean bits) {
			if (bits) s.fillWithOnes();
		}

	}
}

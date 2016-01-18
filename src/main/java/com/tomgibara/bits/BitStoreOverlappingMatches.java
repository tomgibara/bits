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

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.OverlappingMatches;
import com.tomgibara.bits.BitStore.Positions;

//TODO could use better search algorithm
class BitStoreOverlappingMatches extends AbstractMatches implements OverlappingMatches {

	BitStoreOverlappingMatches(BitStore store, BitStore sequence) {
		super(store, sequence);
	}

	@Override
	public OverlappingMatches range(int from, int to) {
		return s.range(from, to).match(t);
	}

	@Override
	public DisjointMatches disjoint() {
		return new BitStoreDisjointMatches(this);
	}
	
	@Override
	public int count() {
		int count = 0;
		int previous = last();
		while (previous != -1) {
			count ++;
			previous = previous(previous);
		}
		return count;
	}

	@Override
	public int first() {
		return next(0);
	}

	@Override
	public int last() {
		return previous(sSize + 1);
	}

	@Override
	public int next(int position) {
		position = Math.max(position, 0);
		int limit = sSize - tSize;
		while (position <= limit) {
			if (matchesAt(position)) return position;
			position ++;
		}
		return sSize;
	}

	@Override
	public int previous(int position) {
		position = Math.min(position, sSize - tSize + 1);
		while (position > 0) {
			position --;
			if (matchesAt(position)) return position;
		}
		return -1;
	}
	
	@Override
	public Positions positions() {
		return Bits.newPositions(this);
	}

	@Override
	public Positions positions(int position) {
		return Bits.newPositions(this, position);
	}
	
	boolean matchesAt(int position) {
		return s.range(position, position + tSize).equals().store(t);
	}
}

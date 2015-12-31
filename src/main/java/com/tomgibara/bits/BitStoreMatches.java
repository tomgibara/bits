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

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Positions;

//TODO could use better search algorithm
class BitStoreMatches implements BitStore.Matches {

	private final BitStore s;
	private final BitStore t;
	private final int sSize;
	private final int tSize;
	
	BitStoreMatches(BitStore store, BitStore sequence) {
		s = store;
		t = sequence;
		sSize = s.size();
		tSize = t.size();
	}

	@Override
	public BitStore store() {
		return s;
	}

	@Override
	public BitStore sequence() {
		return t;
	}

	@Override
	public Matches range(int from, int to) {
		return s.range(from, to).match(t);
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
//		int limit = sSize - tSize;
//		if (limit < 0) return -1;
//		if (matchesAt(limit)) return limit;
//		return previous(limit);
		//TODO is this a risk? - simpler, but position could be rejected?
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
		return Bits.newPositions(this, 0);
	}

	@Override
	public Positions positions(int position) {
		return Bits.newPositions(this, position);
	}
	
	@Override
	public Positions disjointPositions() {
		return Bits.newDisjointPositions(this);
	}

	private boolean matchesAt(int position) {
		return s.range(position, position + tSize).equals().store(t);
	}
}

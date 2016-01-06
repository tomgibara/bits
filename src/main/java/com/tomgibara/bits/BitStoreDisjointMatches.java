package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.Positions;

class BitStoreDisjointMatches extends AbstractDisjointMatches {

	private final BitStore s;
	private final BitStore t;
	private final int sSize;
	private final int tSize;
	
	BitStoreDisjointMatches(BitStore store, BitStore sequence) {
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
	public DisjointMatches range(int from, int to) {
		return s.range(from, to).matchDisjoint(t);
	}

	@Override
	public int count() {
		int count = 0;
		int next = first();
		while (next != sSize) {
			count ++;
			next = next(next + tSize);
		}
		return count;
	}

	@Override
	public int first() {
		return next(0);
	}

	@Override
	public int last() {
		int position = first();
		if (position < sSize) {
			while (true) {
				int p = next(position + tSize);
				if (p == sSize) return position;
			}
		}
		return -1;
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
		return Bits.newDisjointPositions(this);
	}

	@Override
	public Positions positions(int position) {
		return Bits.newDisjointPositions(this, position);
	}
	
	@Override
	public boolean isAll() {
		return tSize * count() == sSize;
	}
	
	private boolean matchesAt(int position) {
		return s.range(position, position + tSize).equals().store(t);
	}

}

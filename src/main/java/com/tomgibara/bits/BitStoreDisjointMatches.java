package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.OverlappingMatches;

class BitStoreDisjointMatches extends AbstractDisjointMatches {

	private final OverlappingMatches matches;
	
	BitStoreDisjointMatches(OverlappingMatches matches) {
		super(matches);
		this.matches = matches;
	}

	BitStoreDisjointMatches(BitStoreOverlappingMatches matches) {
		super(matches);
		this.matches = matches;
	}

	@Override
	public OverlappingMatches overlapping() {
		return matches;
	}
	
	@Override
	public DisjointMatches range(int from, int to) {
		return s.range(from, to).match(t).disjoint();
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
	public boolean isAll() {
		return tSize * count() == sSize;
	}

	boolean matchesAt(int position) {
		return s.range(position, position + tSize).equals().store(t);
	}
}

package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.Positions;

abstract class AbstractDisjointMatches extends AbstractMatches implements DisjointMatches {

	@Override
	public boolean isAll() {
		return count() * sequence().size() == store().size();
	}
	
	@Override
	public boolean isNone() {
		return count() == 0;
	}
	
	@Override
	public void replaceAll(BitStore replacement) {
		if (replacement == null) throw new IllegalArgumentException("null replacement");
		int size = sequence().size();
		if (replacement.size() != size) throw new IllegalArgumentException("invalid replacement size");
		switch (size) {
		case 0: return;
		case 1: store().fillWith(replacement.getBit(0));
		default:
			for (Positions ps = positions(); ps.hasNext();) {
				ps.next();
				ps.replace(replacement);
			}
		}
	}

	@Override
	public void replaceAll(boolean bits) {
		switch (sequence().size()) {
		case 0: return;
		case 1: store().fillWith(bits);
		default:
			for (Positions ps = positions(); ps.hasNext();) {
				ps.next();
				ps.replace(bits);
			}
		}
	}

}

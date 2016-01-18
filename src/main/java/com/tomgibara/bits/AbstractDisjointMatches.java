package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Positions;

abstract class AbstractDisjointMatches extends AbstractMatches implements DisjointMatches {

	public AbstractDisjointMatches(AbstractMatches matches) {
		super(matches);
	}

	public AbstractDisjointMatches(Matches matches) {
		super(matches);
	}

	@Override
	public boolean isAll() {
		return count() * tSize == sSize;
	}

	@Override
	public boolean isNone() {
		return count() == 0;
	}

	@Override
	public void replaceAll(BitStore replacement) {
		if (replacement == null) throw new IllegalArgumentException("null replacement");
		if (replacement.size() != tSize) throw new IllegalArgumentException("invalid replacement size");
		switch (tSize) {
		case 0: return;
		case 1: store().setAll(replacement.getBit(0));
		default:
			for (Positions ps = positions(); ps.hasNext();) {
				ps.next();
				ps.replace(replacement);
			}
		}
	}

	@Override
	public void replaceAll(boolean bits) {
		switch (tSize) {
		case 0: return;
		case 1: store().setAll(bits);
		default:
			for (Positions ps = positions(); ps.hasNext();) {
				ps.next();
				ps.replace(bits);
			}
		}
	}

	@Override
	public Positions positions() {
		return Bits.newDisjointPositions(this);
	}

	@Override
	public Positions positions(int position) {
		return Bits.newDisjointPositions(this, position);
	}

}

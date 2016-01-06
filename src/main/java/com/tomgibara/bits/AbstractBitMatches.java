package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.BitMatches;

abstract class AbstractBitMatches extends AbstractDisjointMatches implements BitMatches {

	@Override
	public void replaceAll(BitStore replacement) {
		if (replacement == null) throw new IllegalArgumentException("null replacement");
		if (replacement.size() != 1) throw new IllegalArgumentException("invalid replacement size");
		replaceAll(replacement.getBit(0));
	}

	@Override
	public void replaceAll(boolean bits) {
		if (bits != bit()) store().fillWith(bits);
	}

}

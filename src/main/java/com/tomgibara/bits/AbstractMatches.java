package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.Matches;

abstract class AbstractMatches implements Matches {

	final BitStore s;
	final BitStore t;
	final int sSize;
	final int tSize;

	AbstractMatches(BitStore store, BitStore sequence) {
		s = store;
		t = sequence;
		sSize = s.size();
		tSize = t.size();
	}

	AbstractMatches(AbstractMatches that) {
		this.s = that.s;
		this.t = that.t;
		this.sSize = that.sSize;
		this.tSize = that.tSize;
	}

	AbstractMatches(Matches matches) {
		this(matches.store(), matches.sequence());
	}

	@Override
	public BitStore store() {
		return s;
	}

	@Override
	public BitStore sequence() {
		return t;
	}


}

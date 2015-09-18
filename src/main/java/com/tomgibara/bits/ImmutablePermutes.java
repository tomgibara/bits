package com.tomgibara.bits;

import java.util.Random;

import com.tomgibara.bits.BitStore.Permutes;

//TODO could just use a new BitStorePermutes instance
class ImmutablePermutes extends Permutes {

	static final ImmutablePermutes INSTANCE = new ImmutablePermutes();
	
	private ImmutablePermutes() { }
	
	@Override
	public void transpose(int i, int j) {
		failMutable();
	}

	@Override
	public void rotate(int distance) {
		failMutable();
	}

	@Override
	public void reverse() {
		failMutable();
	}

	@Override
	public void shift(int distance, boolean fill) {
		failMutable();
	}

	@Override
	public void shuffle(Random random) {
		failMutable();
	}

	private void failMutable() {
		throw new IllegalStateException("immutable");
	}

}

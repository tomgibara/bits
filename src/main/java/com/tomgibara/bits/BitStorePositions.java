package com.tomgibara.bits;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.tomgibara.bits.BitStore.Matches;

class BitStorePositions implements ListIterator<Integer> {

	private static final int NOT_SET = Integer.MIN_VALUE;

	private final Matches matches;
	private final int size;
	private final boolean bit;

	private int previous;
	private int next;
	private int nextIndex;
	private int recent = NOT_SET;
	
	BitStorePositions(Matches matches, int position) {
		this.matches = matches;
		size = matches.store().size();
		bit = matches.sequence().getBit(0);
		previous = matches.previous(position);
		next = matches.next(position);
		nextIndex = previous == -1 ? 0 : NOT_SET;
	}
	
	@Override
	public boolean hasPrevious() {
		return previous != -1;
	}

	@Override
	public boolean hasNext() {
		return next != size;
	}

	@Override
	public Integer previous() {
		if (previous == -1) throw new NoSuchElementException();
		recent = previous;
		next = recent;
		previous = matches.previous(recent);
		if (nextIndex != NOT_SET) nextIndex--;
		return next;
	}

	@Override
	public Integer next() {
		if (next == size) throw new NoSuchElementException();
		recent = next;
		previous = recent;
		next = matches.next(recent + 1);
		if (nextIndex != NOT_SET) nextIndex++;
		return previous;
	}

	@Override
	public int previousIndex() {
		return nextIndex() - 1;
	}

	@Override
	public int nextIndex() {
		return nextIndex == NOT_SET ? nextIndex = matches.range(0, next).count() : nextIndex;
	}

	@Override
	public void add(Integer e) {
		doAdd(e);
		recent = NOT_SET;
	}

	@Override
	public void remove() {
		doRemove();
		recent = NOT_SET;
	}

	@Override
	public void set(Integer e) {
		doRemove();
		doAdd(e);
		recent = NOT_SET;
	}

	private void doAdd(Integer e) {
		if (e == null) throw new IllegalArgumentException("null e");
		int i = e;
		if (i < previous) throw new IllegalArgumentException("e less than previous value: " + previous);
		if (i >= next) throw new IllegalArgumentException("e not less than next value: " + next);
		boolean changed = bit != matches.store().getThenSetBit(i, bit);
		if (changed) {
			if (nextIndex != NOT_SET) nextIndex ++;
			previous = i;
		}
	}

	private void doRemove() {
		if (recent == previous) { // we went forward
			previous = matches.range(0, recent).last();
			if (nextIndex != NOT_SET) nextIndex --;
		} else if (recent == next) { // we went backwards
			next = recent + 1 + matches.range(recent + 1, size).first();
		} else { // no recent value
			throw new IllegalStateException();
		}
		matches.store().setBit(recent, !bit);
	}

}

package com.tomgibara.bits;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.tomgibara.bits.ImmutableBit.ImmutableOne;

final class IntSetBitStore extends AbstractBitStore {

	private final SortedSet<Integer> set;
	private final int start;
	private final int finish;
	private final boolean mutable;
	
	// assumes set has already been 'restricted' to start/finish range
	IntSetBitStore(SortedSet<Integer> set, int start, int finish, boolean mutable) {
		this.set = set;
		this.start = start;
		this.finish = finish;
		this.mutable = mutable;
	}
	
	@Override
	public int size() {
		return finish - start;
	}

	@Override
	public boolean getBit(int index) {
		return set.contains(adjIndex(index));
	}

	@Override
	public long getBits(int position, int length) {
		int to = position + length;
		long bits = 0L;
		Positions positions = range(position, to).ones().positions();
		while (positions.hasNext()) {
			bits |= 1L << positions.nextPosition();
		}
		return bits;
	}
	
	@Override
	public void setBit(int index, boolean value) {
		Bits.checkMutable(mutable);
		index = adjIndex(index);
		if (value) {
			set.add(index);
		} else {
			set.remove(index);
		}
	}

	@Override
	public void flipBit(int index) {
		Bits.checkMutable(mutable);
		index = adjIndex(index);
		if (set.contains(index)) {
			set.remove(index);
		} else {
			set.add(index);
		}
	}
	
	@Override
	public boolean getThenSetBit(int index, boolean value) {
		Bits.checkMutable(mutable);
		index = adjIndex(index);
		return value ? !set.add(index) : set.remove(index);
	}
	
	@Override
	public void setBits(int position, long value, int length) {
		int to = position + length;
		IntSetBitStore range = range(position, to);
		range.fillWithZeros();
		for (int i = 0; i < length; i++, value >>= 1) {
			if ((value & 1) == 1) set.add(range.start + i);
		}
	}
	
	@Override
	public void setStore(int position, BitStore store) {
		int to = position + store.size();
		IntSetBitStore range = range(position, to);
		range.fillWithZeros();
		Positions positions = store.ones().positions();
		while (positions.hasNext()) {
			set.add(range.start + positions.nextPosition());
		}
	}
	
	@Override
	public void fillWithZeros() {
		Bits.checkMutable(mutable);
		if (size() != 0) set.clear();
	}
	
	// matching
	
	@Override
	public BitMatches ones() {
		return new Ones();
	}
	
	// views
	
	@Override
	public IntSetBitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to < from) throw new IllegalArgumentException();
		to += start;
		if (to > finish) throw new IllegalArgumentException();
		from += start;
		SortedSet<Integer> sub;
		if (to == from) {
			// seems we have to do this because subSet(x,x) is faulty?
			sub = Collections.emptySortedSet();
		} else if (to == start) {
			sub = from == finish ? set : set.tailSet(from);
		} else {
			sub = from == finish ? set.headSet(to) : set.subSet(from, to);
		}
		return new IntSetBitStore(sub, from, to, mutable);
	}

	// mutability
	
	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public IntSetBitStore mutableCopy() {
		return new IntSetBitStore(new TreeSet<>(set), start, finish, true);
	}

	@Override
	public IntSetBitStore immutableCopy() {
		return new IntSetBitStore(new TreeSet<>(set), start, finish, false);
	}

	@Override
	public IntSetBitStore immutableView() {
		return new IntSetBitStore(set, start, finish, false);
	}

	// private helper methods

	private int adjIndex(int index) {
		return Bits.adjIndex(index, start, finish);
	}
	
	private int adjPosition(int position) {
		return Bits.adjPosition(position, start, finish);
	}
	
	// inner classes
	
	private class Ones implements BitMatches {

		@Override
		public BitStore store() {
			return IntSetBitStore.this;
		}

		@Override
		public BitStore sequence() {
			return ImmutableOne.INSTANCE;
		}

		@Override
		public int count() {
			return set.size();
		}

		@Override
		public int first() {
			return first(set);
		}

		@Override
		public int last() {
			return last(set);
		}

		@Override
		public int next(int position) {
			position = adjPosition(position);
			if (position == finish) return size();
			if (position == finish - 1) return (set.contains(position) ? position : finish) - start;
			return first(set.tailSet(position));
		}

		@Override
		public int previous(int position) {
			position = adjPosition(position);
			if (position == start) return -1;
			if (position == start + 1) return set.contains(start) ? 0 : -1;
			return last(set.headSet(position));
		}

		@Override
		public Positions positions() {
			return new BitStorePositions(this, 0);
		}

		@Override
		public Positions positions(int position) {
			return new BitStorePositions(this, position);
		}

		@Override
		public BitMatches range(int from, int to) {
			return IntSetBitStore.this.range(from, to).ones();
		}

		@Override
		public boolean bit() {
			return true;
		}

		@Override
		public boolean isAll() {
			return set.size() == size();
		}

		@Override
		public boolean isNone() {
			return set.isEmpty();
		}

		@Override
		public SortedSet<Integer> asSet() {
			return mutable ? set : Collections.unmodifiableSortedSet(set);
		}

		private int first(SortedSet<Integer> set) {
			return set.isEmpty() ? size() : set.first() - start;
		}

		private int last(SortedSet<Integer> set) {
			return set.isEmpty() ? -1 : set.last() - start;
		}

	}
}

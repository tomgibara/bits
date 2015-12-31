package com.tomgibara.bits;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

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
		return new SparseOnes();
	}
	
	@Override
	public BitMatches zeros() {
		return new SparseZeros();
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
	
	private abstract class SparseMatches implements BitMatches {

		private final boolean bit;
		private final SortedSet<Integer> set;

		SparseMatches(boolean bit, SortedSet<Integer> set) {
			this.bit = bit;
			this.set = set;
		}

		@Override
		public BitStore store() {
			return IntSetBitStore.this;
		}

		@Override
		public BitStore sequence() {
			return ImmutableBit.instanceOf(bit);
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
			int size = size();
			if (position == size) return size;
			if (position == size - 1) return (set.contains(position) ? position : size);
			return first(set.tailSet(position));
		}

		@Override
		public int previous(int position) {
			if (position == 0) return -1;
			if (position == 1) return set.contains(0) ? 0 : -1;
			return last(set.headSet(position));
		}

		@Override
		public Positions positions() {
			return new BitStorePositions(this, false, 0);
		}

		@Override
		public Positions positions(int position) {
			return new BitStorePositions(this, false, position);
		}

		@Override
		public Positions disjointPositions() {
			return new BitStorePositions(this, true, 0);
		}

		@Override
		public void replaceAll(boolean bits) {
			if (bits != bit) IntSetBitStore.this.fillWith(bits);
		}

		@Override
		public BitMatches range(int from, int to) {
			return IntSetBitStore.this.range(from, to).match(bit);
		}

		@Override
		public boolean bit() {
			return bit;
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
			return set.isEmpty() ? size() : set.first();
		}

		private int last(SortedSet<Integer> set) {
			return set.isEmpty() ? -1 : set.last();
		}
	}
	
	private class SparseOnes extends SparseMatches {

		SparseOnes() { super(true, new OnesSet(set, start, finish)); }
	}
	
	private class SparseZeros extends SparseMatches {

		SparseZeros() { super(false, new ZerosSet(set, start, finish)); }

	}

	private class OnesSet extends AbstractSet<Integer> implements SortedSet<Integer> {

		private final int from;
		private final int to;
		private final SortedSet<Integer> set;

		OnesSet(SortedSet<Integer> set, int from, int to) {
			this.set = set;
			this.from = from;
			this.to = to;
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Integer)) return false;
			Integer i = start + (Integer) o;
			return i >= from && i < to && set.contains(i);
		}
		
		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Integer)) return false;
			Integer i = start + (Integer) o;
			return set.remove(i);
		}

		@Override
		public void clear() {
			set.clear();
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}
		
		@Override
		public boolean add(Integer e) {
			return set.add(e + start);
		}

		@Override
		public Comparator<? super Integer> comparator() {
			return set.comparator();
		}

		@Override
		public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
			int from = adjPosition(fromElement);
			int to = adjPosition(toElement);
			return new OnesSet(set.subSet(from , to), from, to);
		}

		@Override
		public SortedSet<Integer> headSet(Integer toElement) {
			int to = adjPosition(toElement);
			return new OnesSet(set.headSet(to), from, to);
		}

		@Override
		public SortedSet<Integer> tailSet(Integer fromElement) {
			int from = adjPosition(fromElement);
			return new OnesSet(set.tailSet(from), from, to);
		}

		@Override
		public Integer first() {
			return set.first() - start;
		}

		@Override
		public Integer last() {
			return set.last() - start;
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {

				private final Iterator<Integer> it = set.iterator();

				@Override
				public Integer next() {
					return it.next() - start;
				}

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public void remove() {
					it.remove();
				}

				@Override
				public void forEachRemaining(Consumer<? super Integer> action) {
					Iterator.super.forEachRemaining(i -> action.accept(i - start));
				}
			};
		}

		@Override
		public int size() {
			return set.size();
		}

	}

	private class ZerosSet extends AbstractSet<Integer> implements SortedSet<Integer> {
		
		private final int from;
		private final int to;
		private final SortedSet<Integer> set;
		
		ZerosSet(SortedSet<Integer> set, int from, int to) {
			this.set = set;
			this.from = from;
			this.to = to;
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Integer)) return false;
			Integer i = start + (Integer) o;
			return i >= from && i < to && !set.contains(i);
		}
		
		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Integer)) return false;
			Integer i = (Integer) o;
			return set.add(i + start);
		}

		@Override
		public void clear() {
			for (int i = from; i < to; i++) {
				set.add(i);
			}
		}

		@Override
		public boolean isEmpty() {
			return set.size() == to - from;
		}
		
		@Override
		public boolean add(Integer e) {
			return set.remove(e + start);
		}

		@Override
		public Comparator<? super Integer> comparator() {
			return set.comparator();
		}

		@Override
		public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
			int from = adjPosition(fromElement);
			int to = adjPosition(toElement);
			return new ZerosSet(set.subSet(from , to), from, to);
		}

		@Override
		public SortedSet<Integer> headSet(Integer toElement) {
			int to = adjPosition(toElement);
			return new ZerosSet(set.headSet(to), from, to);
		}

		@Override
		public SortedSet<Integer> tailSet(Integer fromElement) {
			int from = adjPosition(fromElement);
			return new ZerosSet(set.tailSet(from), from, to);
		}

		@Override
		public Integer first() {
			if (from == to) throw new NoSuchElementException();
			int exp = from;
			SortedSet<Integer> s = set;
			while (true) {
				Integer first = s.isEmpty() ? to : s.first();
				if (first != exp) return exp - start;
				exp = first + 1;
				s = s.tailSet(exp);
			}
		}

		@Override
		public Integer last() {
			if (from == to) throw new NoSuchElementException();
			int exp = to - 1;
			SortedSet<Integer> s = set;
			while (true) {
				Integer last = s.isEmpty() ? -1 : s.last();
				if (last != exp) return exp - start;
				exp = last - 1;
				s = s.headSet(last);
			}
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				int next = from - 1;
				int prev;
				
				{ advance(); }
				
				@Override
				public boolean hasNext() {
					return next != to;
				}
				
				@Override
				public Integer next() {
					if (!hasNext()) throw new NoSuchElementException();
					int i = next - start;
					advance();
					return i;
				}
				
				@Override
				public void remove() {
					if (prev == -1) throw new IllegalStateException();
					set.remove(prev);
					prev = -1;
				}
				
				private void advance() {
					prev = next;
					do {
						next ++;
					} while (next != to && set.contains(next));
				}
			};
		}

		@Override
		public int size() {
			return (to - from) - set.size();
		}
		
		
	}
}

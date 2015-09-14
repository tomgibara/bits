package com.tomgibara.bits;

import java.util.ListIterator;

import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;

//TODO implement a special sparse version
abstract class BitStoreMatches extends BitStore.BitMatches {

	final BitStore s;
	
	BitStoreMatches(BitStore s) {
		this.s = s;
	}
	
	@Override
	public BitStore store() {
		return s;
	}
	
	public ListIterator<Integer> positions() {
		return Bits.newListIterator(this, 0);
	}

	public ListIterator<Integer> positions(int position) {
		return Bits.newListIterator(this, position);
	}

	static final class Ones extends BitStoreMatches {

		Ones(BitStore s) {
			super(s);
		}
		
		@Override
		public boolean bit() {
			return true;
		}
		
		@Override
		public ImmutableOne sequence() {
			return ImmutableOne.INSTANCE;
		}
		
		@Override
		public Matches range(int from, int to) {
			return s.range(from, to).ones();
		}
		
		@Override
		public boolean isAll() {
			//TODO could use a reader?
			int size = s.size();
			for (int i = 0; i < size; i++) {
				if (!s.getBit(i)) return false;
			}
			return true;
		}

		@Override
		public int count() {
			//TODO could use a reader?
			int size = s.size();
			int count = 0;
			for (int i = 0; i < size; i++) {
				if (s.getBit(i)) count++;
			}
			return count;
		}

		@Override
		public int first() {
			int size = s.size();
			int i;
			for (i = 0; i < size && !s.getBit(i); i++);
			return i;
		}

		@Override
		public int last() {
			int size = s.size();
			int i;
			for (i = size - 1; i >= 0 && !s.getBit(i); i--);
			return i;
		}

		@Override
		public int next(int position) {
			return position + s.range(position, s.size()).ones().first();
		}

		@Override
		public int previous(int position) {
			return s.range(0, position).ones().last();
		}

	}
	
	
	static final class Zeros extends BitStoreMatches {

		Zeros(BitStore s) {
			super(s);
		}
		
		@Override
		public boolean bit() {
			return false;
		}
		
		@Override
		public ImmutableZero sequence() {
			return ImmutableZero.INSTANCE;
		}
		
		@Override
		public Matches range(int from, int to) {
			return s.range(from, to).zeros();
		}
		
		@Override
		public boolean isAll() {
			//TODO could use a reader?
			int size = s.size();
			for (int i = 0; i < size; i++) {
				if (s.getBit(i)) return false;
			}
			return true;
		}

		@Override
		public int count() {
			//TODO could use a reader?
			int size = s.size();
			int count = 0;
			for (int i = 0; i < size; i++) {
				if (!s.getBit(i)) count++;
			}
			return count;
		}

		@Override
		public int first() {
			int size = s.size();
			int i;
			for (i = 0; i < size && s.getBit(i); i++);
			return i;
		}

		@Override
		public int last() {
			int size = s.size();
			int i;
			for (i = size - 1; i >= 0 && s.getBit(i); i--);
			return i;
		}

		@Override
		public int next(int position) {
			return position + s.range(position, s.size()).zeros().first();
		}

		@Override
		public int previous(int position) {
			return s.range(0, position).zeros().last();
		}

	}
}

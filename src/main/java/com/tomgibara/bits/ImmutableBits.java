/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tomgibara.bits;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;

abstract class ImmutableBits extends AbstractBitStore {

	private final boolean ones;
	final int size;

	// constructors

	private ImmutableBits(boolean ones, int size) {
		this.ones = ones;
		this.size = size;
	}

	// fundamental methods

	@Override
	public int size() {
		return size;
	}

	// tests

	@Override
	public Tests equals() {
		return new UniformTests(Test.EQUALS);
	}

	@Override
	public Tests excludes() {
		return new UniformTests(Test.EXCLUDES);
	}

	@Override
	public Tests contains() {
		return new UniformTests(Test.CONTAINS);
	}

	@Override
	public Tests complements() {
		return new UniformTests(Test.COMPLEMENTS);
	}

	// object methods

	// can optimize?
	public int hashCode() {
		return Bits.bitStoreHasher().intHashValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore)) return false;
		BitStore that = (BitStore) obj;
		if (this.size() != that.size()) return false;
		return that.match(ones).isAll();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(size);
		char c = ones ? '1' : '0';
		for (int i = 0; i < size; i++) sb.append(c);
		return sb.toString();
	}

	// private helper methods

	void checkIndex(int index) {
		if (index < 0) throw new IllegalArgumentException();
		if (index > size) throw new IllegalArgumentException();
	}

	void checkPosition(int position) {
		if (position < 0) throw new IllegalArgumentException();
		if (position > size) throw new IllegalArgumentException();
	}

	void checkPositionLength(int position, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (position + length > size) throw new IllegalArgumentException();
		if (length < 0) throw new IllegalArgumentException();
		if (length > 64) throw new IllegalArgumentException();
	}

	// inner classes

	static class ImmutablesOnes extends ImmutableBits {

		ImmutablesOnes(int size) {
			super(true, size);
		}

		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return true;
		}

		@Override
		public long getBits(int position, int length) {
			checkPositionLength(position, length);
			return length == 64 ? -1L :  ~(-1L << length);
		}

		// matching methods

		@Override
		public BitMatches ones() {
			return new AllMatches();
		}

		@Override
		public BitMatches zeros() {
			return new NoneMatches();
		}

		// i/o methods

		@Override
		public BitReader openReader() {
			return new ConstantReader.OnesReader(size, 0);
		}

		@Override
		public BitReader openReader(int finalPos, int initialPos) {
			Bits.checkBounds(finalPos, initialPos, size);
			return new ConstantReader.OnesReader(initialPos - finalPos, 0);
		}

	}

	static class ImmutablesZeros extends ImmutableBits {

		ImmutablesZeros(int size) {
			super(false, size);
		}

		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return false;
		}

		@Override
		public long getBits(int position, int length) {
			checkPositionLength(position, length);
			return 0;
		}

		// matches methods

		@Override
		public BitMatches ones() {
			return new NoneMatches();
		}

		@Override
		public BitMatches zeros() {
			return new AllMatches();
		}

		// i/o methods

		@Override
		public BitReader openReader() {
			return new ConstantReader.ZerosReader(size, 0);
		}

		@Override
		public BitReader openReader(int finalPos, int initialPos) {
			Bits.checkBounds(finalPos, initialPos, size);
			return new ConstantReader.ZerosReader(initialPos - finalPos, 0);
		}

	}

	private class AllMatches implements BitMatches {

		@Override
		public BitMatches disjoint() {
			return this;
		}

		@Override
		public BitMatches overlapping() {
			return this;
		}

		@Override
		public BitStore store() {
			return ImmutableBits.this;
		}

		@Override
		public BitStore sequence() {
			return ones ? ImmutableOne.INSTANCE : ImmutableZero.INSTANCE;
		}

		@Override
		public int count() {
			return size;
		}

		@Override
		public int first() {
			return 0;
		}

		@Override
		public int last() {
			return size - 1;
		}

		@Override
		public int next(int position) {
			checkPosition(position);
			return position;
		}

		@Override
		public int previous(int position) {
			checkPosition(position);
			return position - 1;
		}

		@Override
		public Positions positions() {
			return new AllPositions(0);
		}

		@Override
		public Positions positions(int position) {
			checkPosition(position);
			return new AllPositions(position);
		}

		@Override
		public void replaceAll(BitStore replacement) {
			Bits.checkMutable();
		}

		@Override
		public void replaceAll(boolean bits) {
			Bits.checkMutable();
		}

		@Override
		public BitMatches range(int from, int to) {
			return ImmutableBits.this.range(from, to).match(ones);
		}

		@Override
		public boolean bit() {
			return ones;
		}

		@Override
		public boolean isAll() {
			return true;
		}

		@Override
		public boolean isNone() {
			return size != 0;
		}

		@Override
		public SortedSet<Integer> asSet() {
			return size == 0 ? Collections.emptySortedSet() : new AllSet();
		}

	}

	private class AllPositions implements Positions {

		private int position;

		AllPositions(int position) {
			this.position = position;
		}

		@Override
		public boolean isDisjoint() {
			return true;
		}

		@Override
		public boolean hasNext() {
			return position < size;
		}

		@Override
		public Integer next() {
			if (!hasNext()) throw new NoSuchElementException();
			return position ++;
		}

		@Override
		public boolean hasPrevious() {
			return position > 0;
		}

		@Override
		public Integer previous() {
			if (!hasPrevious()) throw new NoSuchElementException();
			return -- position;
		}

		@Override
		public int nextIndex() {
			return position;
		}

		@Override
		public int previousIndex() {
			return position - 1;
		}

		@Override
		public int nextPosition() {
			return nextIndex();
		}

		@Override
		public int previousPosition() {
			return previousIndex();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void replace(BitStore replacement) {
			Bits.checkMutable();
		}

		@Override
		public void replace(boolean bits) {
			Bits.checkMutable();
		}
	}

	private class AllSet extends AbstractSet<Integer> implements SortedSet<Integer> {

		public Comparator<? super Integer> comparator() {
			return null;
		}

		@Override
		public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
			return range(fromElement, toElement).match(ones).asSet();
		}

		@Override
		public SortedSet<Integer> headSet(Integer toElement) {
			return rangeTo(toElement).match(ones).asSet();
		}

		@Override
		public SortedSet<Integer> tailSet(Integer fromElement) {
			return rangeFrom(fromElement).match(ones).asSet();
		}

		@Override
		public Integer first() {
			return 0;
		}

		@Override
		public Integer last() {
			return size - 1;
		}

		@Override
		public Iterator<Integer> iterator() {
			return new AllPositions(0);
		}

		@Override
		public int size() {
			return size;
		}
	}

	private class NoneMatches implements BitMatches {

		@Override
		public BitMatches disjoint() {
			return this;
		}

		@Override
		public BitMatches overlapping() {
			return this;
		}

		@Override
		public BitStore store() {
			return ImmutableBits.this;
		}

		@Override
		public BitStore sequence() {
			return ones ? ImmutableZero.INSTANCE : ImmutableOne.INSTANCE;
		}

		@Override
		public int count() {
			return 0;
		}

		@Override
		public int first() {
			return size;
		}

		@Override
		public int last() {
			return -1;
		}

		@Override
		public int next(int position) {
			checkPosition(position);
			return size;
		}

		@Override
		public int previous(int position) {
			checkPosition(position);
			return -1;
		}

		@Override
		public Positions positions() {
			return new NonePositions();
		}

		@Override
		public Positions positions(int position) {
			return new NonePositions();
		}

		@Override
		public void replaceAll(BitStore replacement) {
			Bits.checkMutable();
		}

		@Override
		public void replaceAll(boolean bits) {
			Bits.checkMutable();
		}

		@Override
		public BitMatches range(int from, int to) {
			return ImmutableBits.this.range(from, to).match(ones);
		}

		@Override
		public boolean bit() {
			return !ones;
		}

		@Override
		public boolean isAll() {
			return size == 0;
		}

		@Override
		public boolean isNone() {
			return true;
		}

		@Override
		public SortedSet<Integer> asSet() {
			return Collections.emptySortedSet();
		}
	}

	private class NonePositions implements Positions {

		@Override
		public boolean isDisjoint() {
			return true;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Integer next() {
			throw new NoSuchElementException();
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public Integer previous() {
			throw new NoSuchElementException();
		}

		@Override
		public int nextIndex() {
			return size;
		}

		@Override
		public int previousIndex() {
			return -1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextPosition() {
			return size;
		}

		@Override
		public int previousPosition() {
			return -1;
		}

		@Override
		public void replace(BitStore replacement) {
			Bits.checkMutable();
		}

		@Override
		public void replace(boolean bits) {
			Bits.checkMutable();
		}

	}

	private class UniformTests implements Tests {

		private final Test test;

		UniformTests(Test test) {
			this.test = test;
		}

		@Override
		public Test getTest() {
			return test;
		}

		@Override
		public boolean store(BitStore store) {
			if (store == null) throw new IllegalArgumentException("null store");
			if (store.size() != size) throw new IllegalArgumentException("mismatched size");
			switch (test) {
			case COMPLEMENTS:
				return store.match(ones).isNone();
			case CONTAINS:
				return ones || store.zeros().isAll();
			case EQUALS:
				return store.match(ones).isAll();
			case EXCLUDES:
				return !ones || store.zeros().isAll();
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public boolean bits(long bits) {
			// TODO Auto-generated method stub
			return false;
		}

	}
}

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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import com.tomgibara.bits.BitStore.BitMatches;

class BitStoreSet extends AbstractSet<Integer> implements SortedSet<Integer> {

	private final BitMatches matches;
	private final int length;
	private final int offset; // the value that must be added to received values to map them onto the bits

	// constructors

	BitStoreSet(BitMatches matches, int offset) {
		this.matches = matches;
		this.length = matches.store().size();
		this.offset = offset;
	}

	// set methods

	@Override
	public int size() {
		return matches.count();
	}

	@Override
	public boolean isEmpty() {
		return matches.last() == -1;
	}

	@Override
	public boolean contains(Object o) {
		if (!(o instanceof Integer)) return false;
		int position = offset + (Integer) o;
		return position >= 0 && position < length && matches.store().getBit(position) == matches.bit();
	}

	@Override
	public boolean add(Integer e) {
		boolean bit = matches.bit();
		return matches.store().getThenSetBit(position(e), bit) != bit;
	}

	@Override
	public boolean remove(Object o) {
		if (!(o instanceof Integer)) return false;
		int i = offset + (Integer) o;
		if (i < 0 || i >= length) return false;
		boolean bit = matches.bit();
		return matches.store().getThenSetBit(i, !bit) == bit;
	}

	@Override
	public void clear() {
		matches.store().setAll(!matches.bit());
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			final Iterator<Integer> it = matches.positions();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Integer next() {
				//TODO build offset into positionIterator?
				return it.next() - offset;
			}

			@Override
			public void remove() {
				it.remove();
			}

		};
	}

	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		for (Integer e : c) position(e);
		Iterator<? extends Integer> it = c.iterator();
		boolean bit = matches.bit();
		boolean changed = false;
		BitStore store = matches.store();
		while (!changed && it.hasNext()) {
			changed = store.getThenSetBit(it.next() + offset, bit) != bit;
		}
		while (it.hasNext()) {
			store.setBit(it.next() + offset, bit);
		}
		return changed;
	}

	// sorted set methods

	@Override
	public Comparator<? super Integer> comparator() {
		return null;
	}

	@Override
	public Integer first() {
		int i = matches.first();
		if (i == length) throw new NoSuchElementException();
		return i - offset;
	}

	@Override
	public Integer last() {
		int i = matches.last();
		if (i == -1) throw new NoSuchElementException();
		return i - offset;
	}

	@Override
	public SortedSet<Integer> headSet(Integer toElement) {
		return subSet(0 - offset, (int) toElement);
	}

	@Override
	public SortedSet<Integer> tailSet(Integer fromElement) {
		return subSet((int) fromElement, length - offset);
	}

	@Override
	public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
		int fromInt = fromElement;
		int toInt = toElement;
		if (fromInt > toInt) throw new IllegalArgumentException("from exceeds to");
		return subSet(fromInt, toInt);
	}

	// object methods

	@Override
	public boolean equals(Object o) {
		//TODO optimize
//		if (c instanceof IntSet) {
//		}
		return super.equals(o);
	}

	// private methods

	private int position(Integer e) {
		if (e == null) throw new NullPointerException("null value");
		int i = e + offset;
		if (i < 0) throw new IllegalArgumentException("value less than lower bound");
		if (i >= length) throw new IllegalArgumentException("value greater than upper bound");
		return i;
	}

	private SortedSet<Integer> subSet(int from, int to) {
		if (from > to) throw new IllegalArgumentException("from exceeds to");
		int adjFrom = Math.min(from + offset, length);
		int adjTo = Math.max(to + offset, 0);
		return new BitStoreSet(matches.range(adjFrom, adjTo), -from);
	}


}

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

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

final class BitStoreList extends AbstractList<Boolean> {

	private final BitStore store;

	BitStoreList(BitStore store) {
		this.store = store;
	}

	@Override
	public boolean isEmpty() {
		return store.size() == 0;
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public boolean contains(Object o) {
		if (!(o instanceof Boolean)) return false;
		int size = size();
		if (size == 0) return false;
		int count = store.ones().count();
		return count != ( (Boolean)o ? 0 : size );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean ones = c.contains(Boolean.TRUE);
		boolean zeros = c.contains(Boolean.FALSE);
		int bools = 0;
		if (ones) bools++;
		if (zeros) bools++;
		if (c.size() > bools) return false; // must contain a non-boolean
        return switch (bools) {
            case 0 -> true; // empty collection
            case 1 -> !store.match(zeros).isAll();
            default -> {
                int count = store.ones().count();
                yield count != 0 && count != store.size();
            }
        };
	}

	@Override
	public Boolean get(int index) {
		return store.getBit(index);
	}

	@Override
	public Iterator<Boolean> iterator() {
		return new BitStoreIterator(store);
	}

	@Override
	public ListIterator<Boolean> listIterator() {
		return new BitStoreIterator(store);
	}

	@Override
	public ListIterator<Boolean> listIterator(int index) {
		return new BitStoreIterator(store, index);
	}

	@Override
	public int indexOf(Object object) {
		if (!(object instanceof Boolean)) return -1;
		int position = store.match((Boolean) object).first();
		return position == store.size() ? -1 : position;
	}

	@Override
	public int lastIndexOf(Object object) {
		if (!(object instanceof Boolean)) return -1;
		return store.match((Boolean) object).last();
	}

	@Override
	public Boolean set(int index, Boolean element) {
		boolean b = element;
		return store.getThenSetBit(index, b) != b;
	}

	@Override
	public List<Boolean> subList(int fromIndex, int toIndex) {
		return store.range(fromIndex, toIndex).asList();
	}

	@Override
	public void forEach(Consumer<? super Boolean> action) {
		int size = store.size();
		BitReader reader = store.openReader();
		for (int i = 0; i < size; i++) {
			action.accept(reader.readBoolean());
		}
	}

}

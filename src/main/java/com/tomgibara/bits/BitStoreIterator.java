package com.tomgibara.bits;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

//TODO make public and expose more efficient methods?
final class BitStoreIterator implements ListIterator<Boolean> {

	private final BitStore store;
	private int index;
	private int recent = -1;

	BitStoreIterator(BitStore store, int index) {
		this.store = store;
		this.index = index;
	}

	BitStoreIterator(BitStore store) {
		this(store, 0);
	}

	@Override
	public boolean hasNext() {
		return index < store.size();
	}

	@Override
	public Boolean next() {
		if (!hasNext()) throw new NoSuchElementException();
		recent = index;
		return Boolean.valueOf( store.getBit(index++) );
	}

	@Override
	public int nextIndex() {
		return hasNext() ? index : -1;
	}

	@Override
	public boolean hasPrevious() {
		return index > 0;
	}

	@Override
	public Boolean previous() {
		if (!hasPrevious()) throw new NoSuchElementException();
		recent = --index;
		return Boolean.valueOf( store.getBit(recent) );
	}

	@Override
	public int previousIndex() {
		return hasPrevious() ? index - 1 : -1;
	}

	@Override
	public void set(Boolean bit) {
		if (recent == -1) throw new IllegalStateException();
		store.setBit(recent, bit);
	}

	@Override
	public void add(Boolean bit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void forEachRemaining(Consumer<? super Boolean> action) {
		if (action == null) throw new IllegalArgumentException("null action");
		int size = store.size();
		while (index < size) {
			action.accept(store.getBit(index++));
		}
	}
}


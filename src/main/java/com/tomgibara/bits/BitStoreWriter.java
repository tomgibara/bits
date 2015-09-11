package com.tomgibara.bits;

abstract class BitStoreWriter implements BitWriter {

	final BitStore store;
	private final int initial;
	private int position;
	
	BitStoreWriter(BitStore store, int position) {
		this.store = store;
		this.position = position;
		initial = position;
	}
	
	@Override
	public int writeBit(int bit) throws BitStreamException {
		return writeBoolean((bit & 1) == 1);
	}

	@Override
	public int writeBoolean(boolean bit) throws BitStreamException {
		checkPosition();
		writeBit(--position, bit);
		return 1;
	}

	@Override
	public long getPosition() {
		return initial - position;
	}

	abstract void writeBit(int index, boolean bit);
	
	private void checkPosition() {
		if (position <= 0) throw new EndOfBitStreamException();
	}

	static final class Set extends BitStoreWriter {
		
		Set(BitStore store, int position) {
			super(store, position);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			store.setBit(index, value);
		}

	}
	
	static final class And extends BitStoreWriter {
		
		And(BitStore store, int position) {
			super(store, position);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (!value) store.setBit(index, false);
		}

	}
	
	static final class Or extends BitStoreWriter {
		
		Or(BitStore store, int position) {
			super(store, position);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (value) store.setBit(index, true);
		}

	}
	
	static final class Xor extends BitStoreWriter {
		
		Xor(BitStore store, int position) {
			super(store, position);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (value) store.flipBit(index);
		}

	}
	
}

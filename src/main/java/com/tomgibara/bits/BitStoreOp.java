package com.tomgibara.bits;

import com.tomgibara.bits.BitStore.Operation;

abstract class BitStoreOp extends BitStore.Op {

	private static void checkPosition(BitStore s, long position) {
		if (position < 0) throw new IllegalArgumentException();
		if (position > s.size()) throw new IllegalArgumentException();
	}
	
	final BitStore s;

	private BitStoreOp(BitStore store) {
		s = store;
	}

	@Override
	boolean getThenWithBit(int position, boolean value) {
		boolean previous = s.getBit(position);
		withBit(position, value);
		return previous;
	}

	@Override
	void withByte(int position, byte value) {
		setBitsImpl(position, value, 8);
	}

	@Override
	void withShort(int position, short value) {
		setBitsImpl(position, value, 16);
	}

	@Override
	void withInt(int position, short value) {
		setBitsImpl(position, value, 32);
	}

	@Override
	void withLong(int position, short value) {
		setBitsImpl(position, value, 64);
	}

	@Override
	void withBits(int position, long value, int length) {
		setBitsImpl(position, value, length);
	}

	@Override
	void withStore(BitStore store) {
		if (store.size() != s.size()) throw new IllegalArgumentException("different sizes");
		setStoreImpl(0, store);
	}

	@Override
	void withStore(int position, BitStore store) {
		setStoreImpl(position, store);
	}

	@Override
	void withBytes(int position, byte[] bytes, int offset, int length) {
		setStoreImpl(position, Bits.asBitStore(bytes, offset, length));
	}

	abstract void setStoreImpl(int position, BitStore store);

	abstract void setBitsImpl(int position, long value, int length);
	
	// inner classes
	
	final static class Set extends BitStoreOp {

		Set(BitStore store) {
			super(store);
		}
		
		@Override
		Operation getOperation() {
			return Operation.SET;
		}

		@Override
		void with(boolean value) {
			s.clearWith(value);
		}

		@Override
		void withBit(int position, boolean value) {
			s.setBit(position, value);
		}

		@Override
		boolean getThenWithBit(int position, boolean value) {
			return s.getThenSetBit(position, value);
		}
		
		@Override
		void setBitsImpl(int position, long value, int length) {
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			s.setStore(position, store);
		}
		
		@Override
		BitWriter openWriter(int position) {
			checkPosition(s, position);
			return new BitStoreWriter.Set(s, position);
		}
	}

	final static class And extends BitStoreOp {

		And(BitStore store) {
			super(store);
		}
		
		@Override
		Operation getOperation() {
			return Operation.AND;
		}

		@Override
		void with(boolean value) {
			if (!value) s.clearWithZeros();
		}

		@Override
		void withBit(int position, boolean value) {
			if (!value) s.setBit(position, false);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value &= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position + store.size()));
		}

		@Override
		BitWriter openWriter(int position) {
			checkPosition(s, position);
			return new BitStoreWriter.And(s, position);
		}

	}

	final static class Or extends BitStoreOp {

		Or(BitStore store) {
			super(store);
		}
		
		@Override
		Operation getOperation() {
			return Operation.OR;
		}

		@Override
		void with(boolean value) {
			if (value) s.clearWithOnes();
		}

		@Override
		void withBit(int position, boolean value) {
			if (value) s.setBit(position, true);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value |= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position + store.size()));
		}

		@Override
		BitWriter openWriter(int position) {
			checkPosition(s, position);
			return new BitStoreWriter.Or(s, position);
		}

	}

	final static class Xor extends BitStoreOp {

		Xor(BitStore store) {
			super(store);
		}

		@Override
		Operation getOperation() {
			return Operation.XOR;
		}

		@Override
		void with(boolean value) {
			if (value) s.flip();
		}

		@Override
		void withBit(int position, boolean value) {
			if (value) s.flipBit(position);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value ^= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position + store.size()));
		}

		@Override
		BitWriter openWriter(int position) {
			checkPosition(s, position);
			return new BitStoreWriter.Xor(s, position);
		}

	}

}

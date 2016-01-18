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


abstract class BitStoreOp implements BitStore.Op {

	private static void checkBounds(BitStore s, int finalPos, int initialPos) {
		if (finalPos < 0) throw new IllegalArgumentException("negative finalPos");
		if (initialPos > s.size()) throw new IllegalArgumentException("initialPos too large");
		if (initialPos < finalPos) throw new IllegalArgumentException("finalPos exceeds initialPos");
	}
	
	final BitStore s;

	private BitStoreOp(BitStore store) {
		s = store;
	}

	@Override
	public boolean getThenWithBit(int position, boolean value) {
		boolean previous = s.getBit(position);
		withBit(position, value);
		return previous;
	}

	@Override
	public void withByte(int position, byte value) {
		setBitsImpl(position, value, 8);
	}

	@Override
	public void withShort(int position, short value) {
		setBitsImpl(position, value, 16);
	}

	@Override
	public void withInt(int position, short value) {
		setBitsImpl(position, value, 32);
	}

	@Override
	public void withLong(int position, short value) {
		setBitsImpl(position, value, 64);
	}

	@Override
	public void withBits(int position, long value, int length) {
		setBitsImpl(position, value, length);
	}

	@Override
	public void withStore(BitStore store) {
		if (store.size() != s.size()) throw new IllegalArgumentException("different sizes");
		setStoreImpl(0, store);
	}

	@Override
	public void withStore(int position, BitStore store) {
		setStoreImpl(position, store);
	}

	@Override
	public void withBytes(int position, byte[] bytes, int offset, int length) {
		setStoreImpl(position, new ByteBits(bytes, offset, length));
	}

	abstract void setStoreImpl(int position, BitStore store);

	abstract void setBitsImpl(int position, long value, int length);
	
	// inner classes
	
	final static class Set extends BitStoreOp {

		Set(BitStore store) {
			super(store);
		}
		
		@Override
		public Operation getOperation() {
			return Operation.SET;
		}

		@Override
		public void with(boolean value) {
			s.setAll(value);
		}

		@Override
		public void withBit(int position, boolean value) {
			s.setBit(position, value);
		}

		@Override
		public boolean getThenWithBit(int position, boolean value) {
			return s.getThenSetBit(position, value);
		}
		
		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			checkBounds(s, finalPos, initialPos);
			return new BitStoreWriter.Set(s, finalPos, initialPos);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			s.setStore(position, store);
		}
		
	}

	final static class And extends BitStoreOp {

		And(BitStore store) {
			super(store);
		}
		
		@Override
		public Operation getOperation() {
			return Operation.AND;
		}

		@Override
		public void with(boolean value) {
			if (!value) s.clear();
		}

		@Override
		public void withBit(int position, boolean value) {
			if (!value) s.setBit(position, false);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			checkBounds(s, finalPos, initialPos);
			return new BitStoreWriter.And(s, finalPos, initialPos);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value &= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position, position + store.size()));
		}

	}

	final static class Or extends BitStoreOp {

		Or(BitStore store) {
			super(store);
		}
		
		@Override
		public Operation getOperation() {
			return Operation.OR;
		}

		@Override
		public void with(boolean value) {
			if (value) s.fill();
		}

		@Override
		public void withBit(int position, boolean value) {
			if (value) s.setBit(position, true);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			checkBounds(s, finalPos, initialPos);
			return new BitStoreWriter.Or(s, finalPos, initialPos);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value |= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position, position + store.size()));
		}

	}

	final static class Xor extends BitStoreOp {

		Xor(BitStore store) {
			super(store);
		}

		@Override
		public Operation getOperation() {
			return Operation.XOR;
		}

		@Override
		public void with(boolean value) {
			if (value) s.flip();
		}

		@Override
		public void withBit(int position, boolean value) {
			if (value) s.flipBit(position);
		}

		@Override
		public BitWriter openWriter(int finalPos, int initialPos) {
			checkBounds(s, finalPos, initialPos);
			return new BitStoreWriter.Xor(s, finalPos, initialPos);
		}

		@Override
		void setBitsImpl(int position, long value, int length) {
			value ^= s.getBits(position, length);
			s.setBits(position, value, length);
		}

		@Override
		void setStoreImpl(int position, BitStore store) {
			store.writeTo(openWriter(position, position + store.size()));
		}

	}

	//TODO examine optimizations for this
	private static final class ByteBits extends AbstractBitStore {
		
		final byte[] bytes;
		final int offset;
		final int length;
		
		ByteBits(byte[] bytes, int offset, int length) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public int size() {
			return length;
		}
		
		@Override
		public boolean getBit(int index) {
			index += offset;
			int i = bytes.length - 1 - (index >> 3);
			int m = 1 << (index & 7);
			return (bytes[i] & m) != 0;
		}
		
	}
	
}

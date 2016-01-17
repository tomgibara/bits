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

abstract class BitStoreWriter implements BitWriter {

	final BitStore store;
	private final int finalPos;
	private final int initialPos;
	private int position;
	
	BitStoreWriter(BitStore store, int finalPos, int initialPos) {
		this.store = store;
		this.finalPos = finalPos;
		this.initialPos = initialPos;
		position = initialPos;
	}
	
	@Override
	public int writeBit(int bit) throws BitStreamException {
		return writeBoolean((bit & 1) == 1);
	}

	@Override
	public int writeBoolean(boolean bit) throws BitStreamException {
		if (position <= finalPos) throw new EndOfBitStreamException();
		writeBit(--position, bit);
		return 1;
	}
	
	@Override
	public long writeBooleans(boolean value, long count) throws BitStreamException {
		if (count > Integer.MAX_VALUE) throw new EndOfBitStreamException();
		int size = (int) count;
		int from = position - size;
		if (from < 0) throw new EndOfBitStreamException();
		writeSpan(from, position, value);
		position  = from;
		return size;
	}
	
	@Override
	public int write(int bits, int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException();
		if (count > 32) throw new IllegalArgumentException();
		int index = position - count;
		if (index < 0) throw new EndOfBitStreamException();
		writeBits(index, bits, count);
		position = index;
		return count;
	}

	@Override
	public int write(long bits, int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException();
		if (count > 64) throw new IllegalArgumentException();
		int index = position - count;
		if (index < 0) throw new EndOfBitStreamException();
		writeBits(index, bits, count);
		position = index;
		return count;
	}

	@Override
	public long getPosition() {
		return initialPos - position;
	}
	
	@Override
	public long setPosition(long position) throws BitStreamException, IllegalArgumentException {
		BitStreams.checkPosition(position);
		position = Math.min(position, initialPos);
		this.position = (int) (initialPos - position);
		return position;
	}
	

	abstract void writeBit(int index, boolean bit);
	
	abstract void writeBits(int position, long value, int length);
	
	abstract void writeSpan(int from, int to, boolean value);
	
	static final class Set extends BitStoreWriter {
		
		Set(BitStore store, int finalPos, int initialPos) {
			super(store, finalPos, initialPos);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			store.setBit(index, value);
		}
		
		@Override
		void writeBits(int position, long value, int length) {
			store.set().withBits(position, value, length);
		}
		
		@Override
		void writeSpan(int from, int to, boolean value) {
			//TODO is this a good solution?
			store.range(from, to).setAll(value);
		}

	}
	
	static final class And extends BitStoreWriter {
		
		And(BitStore store, int finalPos, int initialPos) {
			super(store, finalPos, initialPos);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (!value) store.setBit(index, false);
		}
		
		@Override
		void writeBits(int position, long value, int length) {
			store.and().withBits(position, value, length);
		}
		
		@Override
		void writeSpan(int from, int to, boolean value) {
			if (!value) store.range(from, to).clear();
		}
	}
	
	static final class Or extends BitStoreWriter {
		
		Or(BitStore store, int finalPos, int initialPos) {
			super(store, finalPos, initialPos);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (value) store.setBit(index, true);
		}

		@Override
		void writeBits(int position, long value, int length) {
			store.or().withBits(position, value, length);
		}
		
		@Override
		void writeSpan(int from, int to, boolean value) {
			if (value) store.range(from, to).fill();
		}
	}
	
	static final class Xor extends BitStoreWriter {
		
		Xor(BitStore store, int finalPos, int initialPos) {
			super(store, finalPos, initialPos);
		}
		
		@Override
		void writeBit(int index, boolean value) {
			if (value) store.flipBit(index);
		}

		@Override
		void writeBits(int position, long value, int length) {
			store.xor().withBits(position, value, length);
		}
		
		@Override
		void writeSpan(int from, int to, boolean value) {
			if (value) store.range(from, to).flip();
		}
	}
	
}

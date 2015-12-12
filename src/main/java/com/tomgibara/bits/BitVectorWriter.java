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

import java.math.BigInteger;

/**
 * Writes bits to a {@link BitVector}, growing the vector as necessary.
 *
 * It's implementation is such that, if writes do not exceed the initial
 * capacity, no copies or new allocations of bit data will occur.
 *
 * @author Tom Gibara
 */

public class BitVectorWriter implements BitWriter {

	// statics

	private static final int MIN_INCREASE = 64;
	private static final int DEFAULT_CAPACITY = 64;

	// fields

	private int size;
	private BitVector vector;
	private BitWriter writer;

	// constructors

	/**
	 * Constructs a new writer with default capacity.
	 *
	 * The default capacity is currently 64 bits, but this is not guaranteed to
	 * remain unchanged between releases.
	 */

	public BitVectorWriter() {
		init(DEFAULT_CAPACITY);
	}

	/**
	 * Constructs a new writer with a specified capacity in bits.
	 *
	 * @param initialCapacity
	 *            the initial capacity in bits
	 */

	public BitVectorWriter(int initialCapacity) {
		init(initialCapacity);
	}

	private void init(int size) {
		this.size = size;
		vector = new BitVector(size);
		writer = vector.openWriter();
	}

	// vector writer methods

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * continue to be written to after this method has been called.
	 *
	 * @return an immutable {@link BitVector} containing the written bits
	 */

	public BitVector toImmutableBitVector() {
		return toBitVector(false);
	}

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * not be written to after this method has been called. Attempting to do so
	 * will raise an <code>IllegalStateException</code>.
	 *
	 * @return a mutable {@link BitVector} containing the written bits
	 */

	public BitVector toMutableBitVector() {
		BitVector v = toBitVector(true);
		vector = null;
		return v;
	}

	// writer methods

	@Override
	public int writeBit(int bit) throws BitStreamException {
		ensureAvailable(1);
		return writer.writeBit(bit);
	}

	@Override
	public int writeBoolean(boolean bit) throws BitStreamException {
		ensureAvailable(1);
		return writer.writeBoolean(bit);
	}

	@Override
	public long writeBooleans(boolean value, long count) throws BitStreamException {
		ensureAvailable(Math.max(count,0L)); // guard against bad guard
		return writer.writeBooleans(value, count);
	}

	@Override
	public int write(int bits, int count) throws BitStreamException {
		ensureAvailable(Math.max(count,0)); // guard against bad count
		return writer.write(bits, count);
	}

	@Override
	public int write(long bits, int count) throws BitStreamException {
		ensureAvailable(Math.max(count,0)); // guard against bad count
		return writer.write(bits, count);
	}

	@Override
	public int write(BigInteger bits, int count) throws BitStreamException {
		ensureAvailable(Math.max(count, 0)); // guard against bad count
		return writer.write(bits, count);
	}

	@Override
	public int flush() throws BitStreamException {
		return writer.flush();
	}

	@Override
	public int padToBoundary(BitBoundary boundary) throws UnsupportedOperationException, BitStreamException {
		int bits = -(int) getPosition() & boundary.mask;
		return (int) writeBooleans(false, bits);
	}

	@Override
	public long getPosition() {
		return writer.getPosition();
	}
	
	@Override
	// grows to accommodate position
	public long setPosition(long position) {
		BitStreams.checkPosition(position);
		ensureAvailable(position);
		return writer.setPosition(position);
	}

	// private helper methods

	private void checkAvailable() {
		if (vector == null) throw new IllegalStateException("BitVectorWriter has already been converted into a mutable BitVector");
	}

	private void ensureAvailable(long r) {
		checkAvailable();
		long position = getPosition();
		long test = position + r - size;
		if (test <= 0) return; // nothing to do
		// we could test r, but we defer test until we know we need to grow
		if (test > Integer.MAX_VALUE) throw new BitStreamException("Too many bits.");
		int growth = (int) test;
		growth = Math.max(growth, Math.max(MIN_INCREASE, size / 2));
		int newSize = size + growth;
		if (newSize < 0) { // overflow, check if we can grow less
			if (size + test > Integer.MAX_VALUE) throw new BitStreamException("Overflowed maximum BitVector size.");
			newSize = Integer.MAX_VALUE;
		}
		vector = vector.resizedCopy(newSize, true);
		writer = vector.openWriter();
		writer.setPosition(position);
		size = newSize;
	}

	private BitVector toBitVector(boolean mutable) {
		checkAvailable();
		int size = vector.size();
		int pos = (int) getPosition();
		return vector.duplicateRange(size - pos, size, false, mutable);
	}

}

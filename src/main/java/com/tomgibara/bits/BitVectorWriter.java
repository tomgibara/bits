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

	//TODO maintaining a separate offset could be avoided by availability of a skipBits method on BitWriter
	private int offset;
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
	 */
	
	public BitVectorWriter(int initialCapacity) {
		if (initialCapacity < 0) throw new IllegalArgumentException("negative initialCapacity");
		init(initialCapacity);
	}
	
	private void init(int size) {
		this.size = size;
		vector = new BitVector(size);
		writer = vector.openWriter();
	}
	
	// vector writer methods
	
	/**
	 * Obtain the bits which have been written by the writer.
	 * 
	 * @return an immutable {@link BitVector} containing the written bits
	 */
	
	public BitVector toBitVector() {
		int size = vector.size();
		int pos = (int) getPosition();
		return vector.immutableRangeView(size - pos, size);
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
		return offset + writer.getPosition();
	}

	// private helper methods

	private void ensureAvailable(long r) {
		long position = getPosition();
		long test = position + r - size;
		System.out.println(position + " " + r + " " + test + " (" + size + ")");
		if (test <= 0) return; // nothing to do
		// we could test r, but we defer test until we know we need to grow
		if (test > Integer.MAX_VALUE) throw new BitStreamException("Too many bits.");
		int growth = (int) test;
		growth = Math.max(growth, Math.max(MIN_INCREASE, size / 2));
		//System.out.println(growth);
		int newSize = size + growth;
		if (newSize < 0) { // overflow, check if we can grow less
			if (size + test > Integer.MAX_VALUE) throw new BitStreamException("Overflowed maximum BitVector size.");
			newSize = Integer.MAX_VALUE;
		}
		offset = (int) position;
		if (vector.size() < 1000) System.out.println("BEFORE: " + this + " " + vector);
		//TODO would like a better way of doing this copy - maybe support appending on bit vectors?
		BitVector newVector = new BitVector(newSize);
		newVector.setVector(newSize - offset, vector.rangeView(size - offset, size));
		vector = newVector;
		if (vector.size() < 1000) System.out.println("AFTER : " + this + " " + vector);
		writer = vector.openWriter(offset);
		size = newSize;
	}

}

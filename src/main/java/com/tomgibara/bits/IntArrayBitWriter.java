/*
 * Copyright 2007 Tom Gibara
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

/**
 * A {@link BitWriter} that writes bits into an array of integers. Bits are
 * written into the int array starting at index zero. Within each int, the most
 * signficant bits are written first.
 *
 * @author Tom Gibara
 *
 */

class IntArrayBitWriter implements BitWriter {

	// statics

	//hope that this will be inlined...
	private static int frontMask(int i) {
		return i == 0 ? 0 : -1 << (32 - i);
	}

	//hope that this will be inlined...
	private static int backMask(int i) {
		return i == 32 ? 0 : -1 >>> i;
	}

	//hope that this will be inlined...
	private static int preMask(int i) {
		return i == 0 ? 0 : -1 >>> (32 - i);
	}

	// fields

	private final int[] ints;
	private final long size;
	private long position = 0;
	private int bufferSize = 0;
	private int bufferBits = 0;

	// constructors

	/**
	 * Creates a new {@link BitWriter} which is backed by the specified int array.
	 * The size of the writer will equal the total number of bits in the array.
	 *
	 * @param ints
	 *            the ints to which bits will be written, not null
	 * @see #getSize()
	 */

	IntArrayBitWriter(int[] ints) {
		this.ints = ints;
		size = ((long) ints.length) << 5;
	}

	/**
	 * Creates a new {@link BitWriter} which is backed by the specified int
	 * array. Bits will be written to the int array up to the specified size.
	 *
	 * @param ints
	 *            the ints to which bits will be written, not null
	 * @param size
	 *            the number of bits that may be written, not negative and no
	 *            greater than the number of bits available in the array
	 */

	IntArrayBitWriter(int[] ints, long size) {
		this.ints = ints;
		this.size = size;
	}

	// bit writer methods

	@Override
	public int writeBit(int bit) throws BitStreamException {
		//TODO what is the correct behaviour here?
		//throw EOBSE or return reduced count
		if (position + bufferSize + 1 > size) throw new BitStreamException("array full");

		if (bufferSize == 0) {
			bufferBits = bit;
			bufferSize = 1;
		} else if (bufferSize + 1 <= 32) {
			bufferBits = (bufferBits << 1) | bit & 1;
			bufferSize ++;
		} else {
			flushBuffer();
			bufferBits = bit;
			bufferSize = 1;

		}

		return 1;
	}
	
	@Override
	public int write(int bits, int count) {
		if (count == 0) return 0;
		//TODO what is the correct behaviour here?
		//throw EOBSE or return reduced count
		if (position + bufferSize + count > size) throw new BitStreamException("array full");

		if (bufferSize == 0) {
			bufferBits = bits;
			bufferSize = count;
		} else if (bufferSize + count <= 32) {
			bufferBits = (bufferBits << count) | bits & preMask(count);
			bufferSize += count;
		} else {
			flushBuffer();
			bufferBits = bits;
			bufferSize = count;

		}

		return count;
	}

	//optimized implementation
	@Override
	public long writeBooleans(boolean value, long count) {
		count = Math.min(count, size - position - bufferSize);
		if (count == 0) return 0;

		final int bits = value ? -1 : 0;
		long c = count;
		while (c >= 32) {
			flushBuffer();
			bufferBits = bits;
			bufferSize = 32;
			c -= 32;
		}

		int d = (int) c;
		if (d == 0) {
			/* do nothing */
		} else if (bufferSize == 0) {
			bufferBits = bits;
			bufferSize = d;
		} else if (bufferSize + d <= 32) {
			bufferBits <<= d;
			if (value) bufferBits |= preMask(d);
			bufferSize += d;
		} else {
			flushBuffer();
			bufferBits = bits;
			bufferSize = d;
		}

		return count;
	}

	@Override
	public long getPosition() {
		flushBuffer();
		return position;
	}

	@Override
	public long setPosition(long position) {
		BitStreams.checkPosition(position);
		if (position != getPosition()) {
			this.position = Math.min(position, size);
		}
		return this.position;
	}

	@Override
	public int flush() {
		flushBuffer();
		return 0;
	}

	// accessors

	/**
	 * The maximum number of bits that may be written by this {@link BitWriter}.
	 *
	 * @return the least position at which there bits cannot be written, never
	 *         negative
	 */

	long getSize() {
		return size;
	}

	/**
	 * The int array the backs this {@link BitWriter}.
	 *
	 * @return the ints written by this {@link BitWriter}, never null
	 */

	int[] getInts() {
		return ints;
	}

	// object methods

	@Override
	public String toString() {
		int length = (int) Math.min(size, 100);
		StringBuilder sb = new StringBuilder(length);
		IntArrayBitReader reader = new IntArrayBitReader(ints, length);
		for (int i = 0; i < length; i++) {
			sb.append( reader.readBit() == 0 ? "0" : "1" );
		}
		if (length != size) sb.append("...");
		return sb.toString();
	}

	// private utlity methods

	private void flushBuffer() {
		if (bufferSize == 0) return;
		doWrite(bufferBits, bufferSize);
		position += bufferSize;
		//bufferBits = 0; we leave the buffer dirty
		bufferSize = 0;
	}

	//assumes count is non-zero
	private void doWrite(int bits, int count) {

		int frontBits = ((int) position) & 31;
		int firstInt = (int) (position >> 5);

		int sumBits = count + frontBits;
		if (sumBits <= 32) {
			int i = ints[firstInt];
			int mask = frontMask(frontBits) | backMask(sumBits);
			i &= mask;
			i |= (bits << (32 - sumBits)) & ~mask;
			ints[firstInt] = i;
		} else {
			int i = ints[firstInt];
			int mask = frontMask(frontBits);
			i &= mask;
			int lostBits = sumBits - 32;
			i |= (bits >> lostBits) & ~mask;
			ints[firstInt] = i;

			i = ints[firstInt + 1];
			mask = backMask(lostBits);
			i &= mask;
			i |= (bits << (32 - lostBits)) & ~mask;
			ints[firstInt + 1] = i;
		}
	}

}

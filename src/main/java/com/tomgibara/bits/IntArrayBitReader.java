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
 * A {@link BitReader} that sources its bits from an InputStream. Bits are read
 * from the int array starting at index zero. Within each int, the most
 * signficant bits are read first.
 *
 *
 * @author Tom Gibara
 */

//TODO optimize readUntil
class IntArrayBitReader implements BitReader {

	// statics

	//hope that this will be inlined...
	//i is number of 1's in lsbs
	private static int mask(int i) {
		return i == 0 ? 0 : -1 >>> (32 - i);
	}

	// fields

	private final int[] ints;
	private final long size;
	private long position = 0L;

	// constructors

	/**
	 * Creates a new {@link BitReader} which is backed by the specified int array.
	 * The size of the reader will equal the total number of bits in the array.
	 *
	 * @param ints
	 *            the ints from which bits will be read, not null
	 * @see #getSize()
	 */

	IntArrayBitReader(int[] ints) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		this.ints = ints;
		size = ((long) ints.length) << 5;
	}

	/**
	 * Creates a new {@link BitReader} which is backed by the specified int
	 * array. Bits will be read from the int array up to the specified size.
	 *
	 * @param ints
	 *            the ints from which bits will be read, not null
	 * @param size
	 *            the number of bits that may be read, not negative and no
	 *            greater than the number of bits supplied by the array
	 */

	public IntArrayBitReader(int[] ints, long size) {
		if (ints == null) throw new IllegalArgumentException("null ints");
		if (size < 0) throw new IllegalArgumentException("negative size");
		long maxSize = ((long) ints.length) << 5;
		if (size > maxSize) throw new IllegalArgumentException("size exceeds maximum permitted by array length");
		this.ints = ints;
		this.size = size;
	}

	// bit reader methods

	@Override
	public int readBit() {
		if (position >= size) throw new EndOfBitStreamException();
		int k = (ints[(int)(position >> 5)] >> (31 - (((int)position) & 31))) & 1;
		position++;
		return k;
	}

	@Override
	public int read(int count) {
		if (count == 0) return 0;
		if (position + count > size) throw new EndOfBitStreamException();
		int frontBits = ((int)position) & 31;
		int firstInt = (int)(position >> 5);
		int value;

		int sumBits = count + frontBits;
		if (sumBits <= 32) {
			value = (ints[firstInt] >> (32 - sumBits)) & mask(count);
		} else {
			value = ((ints[firstInt] << (sumBits - 32)) | (ints[firstInt + 1] >>> (64 - sumBits))) & mask(count);
		}

		position += count;
		return value;
	}

	@Override
	public long skipBits(long count) {
		count = count < 0 ?
				Math.max( - position, count) :
				Math.min(size - position, count);
		position += count;
		return count;
	}

	@Override
	public long getPosition() {
		return position;
	}

	public long setPosition(long position) {
		BitStreams.checkPosition(position);
		return this.position = Math.min(position, size);
	}


	// accessors

	/**
	 * The int array the backs this {@link BitReader}.
	 *
	 * @return the ints read by this {@link BitReader}, never null
	 */

	int[] getInts() {
		return ints;
	}

	/**
	 * The maximum number of bits that may be read by this {@link BitReader}.
	 *
	 * @return the least position at which there is no bit to read, never
	 *         negative
	 */

	long getSize() {
		return size;
	}

}

/*
 * Copyright 2011 Tom Gibara
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
 * A convenient base class for creating {@link BitWriter} implementations that
 * store their bits in a byte sequence.
 *
 * @author Tom Gibara
 *
 */

public abstract class ByteBasedBitWriter extends AbstractBitWriter {

	//stores up to 8 bits - higher bits may include garbage
	private int buffer = 0;
	// number of bits in buffer
	// buffer is flushed immediately when count reaches 8
	private int count = 0;
	// the position in the stream
	private long position = 0;

	// methods for implementation

	/**
	 * Writes a byte into the sequence. The byte to be written is provided as
	 * the least-significant byte of the supplied int.
	 *
	 * @param value
	 *            the value to write
	 * @throws BitStreamException
	 *             if an exception occurs when writing
	 */

	protected abstract void writeByte(int value) throws BitStreamException;

	/**
	 * Writes a single value repeatedly into the sequence.
	 *
	 * @param value
	 *            the value to be written
	 * @param count
	 *            the number of bytes to write
	 * @throws BitStreamException
	 *             if an exception occurs when writing
	 */

	protected abstract void fillBytes(int value, long count) throws BitStreamException;

	// bit writer methods

	@Override
	public long writeBooleans(boolean value, long count) {
		if (count < 0L) throw new IllegalArgumentException("negative count");
		int boundary = bitsToBoundary(BitBoundary.BYTE);
		int bits = value ? -1 : 0;
		if (count <= boundary) return write(bits, (int) count);

		long c = write(bits, boundary);
		long d = (count - c) >> 3;
		fillBytes(bits, d);
		d <<= 3;
		position += d;
		c += d;
		c += write(bits, (int) (count - c));

		return c;
	}

	@Override
	public int writeBit(int bit) {
		buffer = (buffer << 1) | (bit & 1);
		if (++count == 8) {
			writeByte(buffer);
			count = 0;
		}
		position++;
		return 1;
	}

	@Override
	public int write(int bits, int count) {
    	if (count < 0) throw new IllegalArgumentException("negative count");
    	if (count > 32) throw new IllegalArgumentException("count too great");
		if (count == 0) return 0;
		int c = count;
		// first buffer fill, we need to mix bits
		if (this.count + c >= 8) {
			int b = 8 - this.count;
			c -= b;
			writeByte( (buffer << b) | ((bits >> c) & (-1 >>> -b)) );
			this.count = 0;
		}
		while (c > 7) {
			c -= 8;
			writeByte(bits >> c);
		}
		if (this.count == 0) {
			buffer = bits;
			this.count = c;
		} else {
			buffer = (buffer << c) | (bits & (-1 >>> -c));
			this.count += c;
		}
		position += count;
		return count;
	}

	@Override
	public int flush() {
		if (count == 0) return 0;
		int c = 8 - count;
		writeByte(buffer << c);
		count = 0;
		position += c;
		return c;
	}

	@Override
	public long getPosition() {
		return position;
	}


}

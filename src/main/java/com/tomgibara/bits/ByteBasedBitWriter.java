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

import java.util.Arrays;


/**
 * A convenient base class for creating {@link BitWriter} implementations that
 * store their bits in a byte sequence.
 *
 * @author Tom Gibara
 *
 */

//TODO support setting position
abstract class ByteBasedBitWriter implements BitWriter {

	// statics
	
	private static byte[] sZerosBuffer = null;
	private static byte[] sOnesBuffer = null;

	private static final int PAD_BUFFER = 128;
	private static final int PAD_LIMIT = 3;

	// fields

	private final long size;
	//stores up to 8 bits - higher bits may include garbage
	private int buffer = 0;
	// number of bits in buffer
	// buffer is flushed immediately when count reaches 8
	private int count = 0;
	// the position in the stream
	private long position = 0;

	ByteBasedBitWriter() {
		size = Long.MAX_VALUE;
	}
	
	ByteBasedBitWriter(long size) {
		this.size = size;
	}
	
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

	protected void writeBytes(byte[] bytes, int offset, int length) throws BitStreamException {
		for (int i = 0; i < length; i++) {
			writeByte(bytes[offset + i]);
		}
	}

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

	protected void fillBytes(int value, long count) throws BitStreamException {
		// if it's short, write bytes directly
		if (count < PAD_LIMIT) {
			for (int i = 0; i < count; i++) writeByte(value);
			return;
		}

		// obtain an array we can use to write the bytes efficiently
		byte b = (byte) value;
		byte[] buffer = getBuffer(b);
		int len;
		if (buffer == null) {
			//TODO should all buffers be cached?
			len = count > PAD_BUFFER ? PAD_BUFFER : (int) count;
			buffer = new byte[len];
			Arrays.fill(buffer, b);
		} else {
			len = PAD_BUFFER;
		}

		// if we can, just do it with a single buffer
		if (count <= len) {
			writeBytes(buffer, 0, (int) count);
			return;
		}

		// write the buffer as many times as we need to
		long limit = count / len;
		for (long i = 0; i < limit; i++) {
			writeBytes(buffer, 0, len);
		}
		int r = (int) (count - limit * len);
		if (r != 0) writeBytes(buffer, 0, r);
	}

	// bit writer methods

	@Override
	public long writeBooleans(boolean value, long count) {
		if (count < 0L) throw new IllegalArgumentException("negative count");
		if (position + count > size) throw new EndOfBitStreamException();
		int boundary = BitBoundary.BYTE.bitsFrom(position);
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
		if (position >= size) throw new EndOfBitStreamException();
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
		if (position + count > size) throw new EndOfBitStreamException();
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
		//TODO looks questionable
		position += c;
		return c;
	}

	@Override
	public long getPosition() {
		return position;
	}

	// private utility methods

	private byte[] getBuffer(byte b) {
		if (b != 0 & b != -1) return null;
		byte[] buffer;
		switch (b) {
		case 0: buffer = sZerosBuffer; break;
		case -1 : buffer = sOnesBuffer; break;
		default: return null;
		}

		if (buffer == null) {
			buffer = new byte[PAD_BUFFER];
			if (b != 0) {
				Arrays.fill(buffer, b);
				sOnesBuffer = buffer;
			} else {
				sZerosBuffer = buffer;
			}
		}
		return buffer;
	}

}

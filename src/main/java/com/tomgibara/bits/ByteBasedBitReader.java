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
 * A convenient base class for creating {@link BitReader} implementations that
 * source their bits from a sequence of bytes.
 *
 * @author Tom Gibara
 *
 */

abstract class ByteBasedBitReader implements BitReader {

	// statics

	private static final int[] ZERO_COUNT_LOOKUP = new int[256 * 8];
	private static final int[] ONE_COUNT_LOOKUP = new int[256 * 8];

	private static int countBits(int buffer, int position, boolean ones) {
		final int e = ones ? 1 : 0;
		int i;
		for (i = position; i < 8 && ((buffer >> (7 - i)) & 1) == e; i++);
		return i - position;
	}

	static {
		for (int buffer = 0; buffer < 256; buffer++) {
			for (int position = 0; position < 8; position++) {
				int index = (buffer << 3) | position;
				ZERO_COUNT_LOOKUP[index] = countBits(buffer, position, false);
				ONE_COUNT_LOOKUP[index] = countBits(buffer, position, true);
			}
		}
	}

	// fields

	private final long size;
	private int buffer = 0;
	private long position = 0;

	// constructors

	ByteBasedBitReader() {
		this.size = Long.MAX_VALUE;
	}

	ByteBasedBitReader(long size) {
		this.size = size;
	}

	// methods for overriding

	/**
	 * The next byte in the stream.
	 *
	 * @return the next byte in the stream, or -1 if the end of the byte stream
	 *         has been reached
	 *
	 * @throws BitStreamException
	 *             if an exception occurs when reading
	 */

	protected abstract int readByte() throws BitStreamException;

	/**
	 * Instructs the byte stream to skip a specified number of bytes.
	 * Implementations are permitted to skip fewer, and possibly zero, bytes.
	 * Any attempt to skip past the end of the stream should curtail the number
	 * of bytes skipped and not necessarily raise a {@link BitStreamException}.
	 *
	 * Implementations that cannot support this method MAY safely return zero on
	 * every call.
	 *
	 * @param count
	 *            the number of bytes to skip, never zero
	 * @return the number of bytes skipped
	 * @throws BitStreamException
	 *             if an exception occurs when skipping
	 */

	protected abstract long skipBytes(long count) throws BitStreamException;

	/**
	 * Instructs the byte stream to reposition itself so that the next read will
	 * return the byte at the specified index. Attempting to seek beyond the
	 * length of the stream is not an error, in this case, implementations that
	 * support seeking SHOULD return the first index after that of the last
	 * byte.
	 *
	 * Implementations that cannot support this method SHOULD return
	 * consistently return -1L.
	 *
	 * @param index
	 *            the next byte to be read via {@link #readByte()}
	 * @return the index of the next byte that will be returned from
	 *         {@link #readByte()}
	 * @throws BitStreamException
	 *             if an exception occurs when seeking
	 */

	protected abstract long seekByte(long index) throws BitStreamException;

	// public methods

	public long setPosition(long position) {
		BitStreams.checkPosition(position);
		if (position != this.position) {
			// is this necessary? preferable? position = Math.min(position, size);
			long index = seekByte(position >> 3);
			if (index < 0L) { // seeking not supported - skip whole distance
				long count = position - this.position;
				if (count > 0L) skipBits(count);
			} else { // seeking is supported - skip remaining distance
				this.position = index << 3;
				skipBits(position - this.position);
			}
		}
		return this.position;
	}

	// bit reader methods

	@Override
	public int readBit() {
		if (position == size) throw new EndOfBitStreamException();
		int count = (int) position & 7;
		if (count == 0) { // need new bits
			buffer = readByte();
			if (buffer == -1) throw new EndOfBitStreamException();
		}
		position++;
		// other endian
		// return (buffer >> count) & 1;
		return (buffer >> (7 - count)) & 1;
	}

	@Override
	public int read(int count) {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count > 32) throw new IllegalArgumentException("count too great");
		if (count == 0) return 0;
		if (position + count > size) throw new EndOfBitStreamException();

		int value;
		int remainder = (8 - (int)position) & 7;
		if (remainder == 0) {
			value = 0;
		} else if (count > remainder) {
			// other endian
			// value = buffer >> (8 - remainder);
			value = buffer & ((1 << remainder) - 1);
			count -= remainder;
			position += remainder;
		} else {
			position += count;
			// other endian
			// return buffer & ((1 << count) - 1);
			return (buffer >> (remainder - count)) & ((1 << count) - 1);
		}

		while (true) {
			buffer = readByte();
			if (buffer == -1) throw new EndOfBitStreamException();
			if (count >= 8) {
				value = (value << 8) | buffer;
				count -= 8;
				position += 8;
				if (count == 0) return value;
			} else {
				// other endian
				// value = (value << count) | (buffer & ((1 << count) - 1));
				value = (value << count) | (buffer >> (8 - count));
				position += count;
				return value;
			}
		}
	}

	@Override
	public int readUntil(boolean one) {
		int total = 0;
		final int[] lookup = one ? ZERO_COUNT_LOOKUP : ONE_COUNT_LOOKUP;
		int count = (int) position & 7;
		while (true) {
			if (count == 0) { // need new bits
				buffer = readByte();
				if (buffer == -1) throw new EndOfBitStreamException();
			}
			int t = lookup[(buffer << 3) | count];
			if (count + t == 8) {
				position += t;
				if (position > size) {
					position = size;
					throw new EndOfBitStreamException();
				}
				total += t;
				count = 0;
			} else {
				position += t + 1;
				if (position > size) {
					position = size;
					throw new EndOfBitStreamException();
				}
				return total + t;
			}
		}
	}

	@Override
	public long skipBits(long count) {
		if (count < 0L) return BitReader.super.skipBits(count);
		count = Math.min(count, size - position);
		int boundary = BitBoundary.BYTE.bitsFrom(position);
		if (count <= boundary) {
			position += count;
			return count;
		}

		position += boundary;
		long bytes = (count - boundary) >> 3;
		long skipped = skipFully(bytes);
		long bits = skipped << 3;
		if (skipped < bytes) return boundary + bits;

		for (int remainder = (int)(count - boundary - bits); remainder > 0; remainder--) {
			try {
				readBit();
			} catch (EndOfBitStreamException e) {
				return count - remainder;
			}
		}
		return count;
	}

	@Override
	public long getPosition() {
		return position;
	}

	// private utility methods

	private long skipFully(long count) {
		long total = 0L;
		while (total < count) {
			long skipped = skipBytes(count);
			if (skipped == 0L) {
				if (readByte() < 0) {
					break;
				} else {
					skipped = 1L;
				}
			}
			total += skipped;
			position += skipped << 3;
		}
		return total;
	}

}

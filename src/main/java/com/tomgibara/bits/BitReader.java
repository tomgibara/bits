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

import java.math.BigInteger;

/**
 * An interface for reading bits from a stream.
 *
 * Default implementations are provided for all methods. Implementations
 * MUST implement either {@link #readBit()} or {@link #read(int)}, SHOULD
 * implement {@link #getPosition()} and/or {@link #setPosition(long)} where
 * practical and MAY override any other methods as necessary, say to improve
 * performance.
 *
 * @author Tom Gibara
 *
 */

public interface BitReader {

	/**
	 * Reads a single bit from a stream of bits.
	 *
	 * @return the value 0 or 1
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default int readBit() throws BitStreamException {
		return read(1);
	}

	/**
	 * Reads a single bit from a stream of bits.
	 *
	 * @return whether the bit was set
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default boolean readBoolean() throws BitStreamException {
		return readBit() == 1;
	}

	/**
	 * Read between 0 and 32 bits from a stream of bits. Bits are returned in
	 * the least significant places.
	 *
	 * @param count
	 *            the number of bits to read
	 * @return the read bits
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default int read(int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count > 32) throw new IllegalArgumentException("count too great");
		if (count == 0) return 0;
		int acc = readBit();
		while (--count > 0) {
			acc = acc << 1 | readBit();
		}
		return acc;
	}

	/**
	 * Read between 0 and 64 bits from a stream of bits. Bits are returned in
	 * the least significant places.
	 *
	 * @param count
	 *            the number of bits to read
	 * @return the read bits
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default long readLong(int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count > 64) throw new IllegalArgumentException("count too great");
		if (count == 0) return 0L;
		if (count <= 32) return read(count) & 0x00000000ffffffffL;
		return (((long)read(count - 32)) << 32) | (read(32) & 0x00000000ffffffffL);
	}

	/**
	 * Read a number of bits from a stream of bits.
	 *
	 * @param count
	 *            the number of bits to read
	 * @return the read bits as a big integer
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default BigInteger readBigInt(int count) throws BitStreamException {
		BitVector bits = new BitVector(count);
		bits.readFrom(this);
		return bits.toBigInteger();
	}

	/**
	 * Reads as many consecutive bits as possible together with a single
	 * terminating bit of the opposite value and returns the number of
	 * consecutive bits read.
	 *
	 * This means that at least one bit is always read (the terminating bit)
	 * unless the end of the stream has been reached in which case an
	 * {@link EndOfBitStreamException} is raised.
	 *
	 * The number returned does not include the terminating bit in the count.
	 *
	 * @param one
	 *            whether ones should be counted instead of zeros
	 * @return the number of consecutive bits read.
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default int readUntil(boolean one) throws BitStreamException {
		int count = 0;
		while (readBoolean() != one) count++;
		return count;
	}

	/**
	 * The position in the stream; usually (but not necessarily) the number of
	 * bits read. Implementations that cannot report their position should
	 * consistently return -1L.
	 *
	 * @return the position in the stream, or -1L
	 */

	default long getPosition() {
		return -1;
	}

	/**
	 * Attempts to move the position of the reader to the new position.
	 * Implementations that cannot adjust their position in this way should
	 * consistently return -1L.
	 *
	 * If this method is supported, the reader is obligated to move the position
	 * as close to the requested new position as it can. It may be that the
	 * position remains unchanged if the reader is already at the end of the
	 * stream, or if the new position is before the current position and
	 * backward seeking is not supported.
	 *
	 * @param newPosition
	 *            the desired position, not negative
	 * @return the resulting position in the stream, or -1L
	 * @throws IllegalArgumentException
	 *             if the new position is negative
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	default long setPosition(long newPosition) throws BitStreamException, IllegalArgumentException {
		return -1;
	}

	/**
	 * Skip the specified number of bits, possibly null. The number of bits
	 * skipped will only be less than the number requested in the event that an
	 * attempt is made to skip past the end of the stream.
	 *
	 * @param count
	 *            the number of bits to skip
	 * @return the number of bit skipped
	 * @throws BitStreamException
	 *             if an exception occurs when skipping
	 */

	default long skipBits(long count) throws BitStreamException {
		if (count < 0L) throw new IllegalArgumentException("negative count");
		long remaining = count;
		for (; remaining > 0; remaining--) {
			try {
				readBit();
			} catch (EndOfBitStreamException e) {
				return count - remaining;
			}
		}
		return count;
	}

	/**
	 * Skips the number of bits necessary to align subsequent reads to a
	 * boundary. Implementations that do not track their in-stream position may
	 * throw an {@link UnsupportedOperationException}.
	 *
	 * @param boundary
	 *            the 'size' of boundary
	 * @return the number of bits skipped to align input
	 * @throws UnsupportedOperationException
	 *             if the stream does not support alignment
	 * @throws BitStreamException
	 *             if an exception occurs when skipping
	 * @throws EndOfBitStreamException
	 *             if the end of the stream is encountered before the position
	 *             can be aligned
	 */

	default int skipToBoundary(BitBoundary boundary) throws UnsupportedOperationException, BitStreamException, EndOfBitStreamException {
		if (boundary == null) throw new IllegalArgumentException("null boundary");
		int count = boundary.bitsFrom(getPosition());
		skipBits(count);
		return count;
	}

}

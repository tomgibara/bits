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
 * <p>
 * An interface for reading bits from a stream.
 *
 * <p>
 * Implementors MUST implement the method {@link #readBit()}; default
 * implementations are provided for all other methods. Implementors SHOULD
 * implement {@link #getPosition()} and/or {@link #setPosition(long)} where
 * practical and MAY override any other methods as necessary to improve
 * performance. It is recommended that, where possible, an efficient
 * implementation is provided for {@link #read(int)}.
 *
 * @author Tom Gibara
 */

@FunctionalInterface
public interface BitReader extends BitStream {

	/**
	 * Reads a single bit from a stream of bits.
	 *
	 * @return the value 0 or 1
	 * @throws BitStreamException
	 *             if an exception occurs when reading the stream
	 */

	int readBit() throws BitStreamException;

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
		BitStore bits = Bits.store(count);
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

	default long skipBits(long count) throws UnsupportedOperationException, BitStreamException {
		long position = getPosition();
		if (position != -1L) return BitStream.super.skipBits(count);
		if (count < 0L) throw new UnsupportedOperationException("cannot skip backwards");
		return BitStreams.slowForwardSkip(this, count);
	}

}

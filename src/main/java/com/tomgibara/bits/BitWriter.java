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
 * An interface for writing bits to a stream.
 *
 * Default implementations are provided for all methods. Implementations MUST
 * implement either {@link #writeBit(int)} or {@link #write(int, int)}, SHOULD
 * implement {@link #getPosition()} where practical and MAY override any other
 * methods as necessary, say to improve performance.
 *
 * @author Tom Gibara
 */

public interface BitWriter {

	/**
	 * Writes the least significant bit of an int to the stream.
	 *
	 * @param bit
	 *            the value to write
	 *
	 * @return the number of bits written, always 1
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default int writeBit(int bit) throws BitStreamException {
		return write(bit, 1);
	}

	/**
	 * Write a single bit to the stream.
	 *
	 * @param bit
	 *            the value to write
	 *
	 * @return the number of bits written, always 1
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default int writeBoolean(boolean bit) throws BitStreamException {
		return writeBit(bit ? 1 : 0);
	}

	/**
	 * Writes the specified number of bits to the stream.
	 *
	 * @param value
	 *            the bit value to be written
	 * @param count
	 *            the number of bits to write
	 *
	 * @return the number of bits written
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default long writeBooleans(boolean value, long count) throws BitStreamException {
		if (count == 0) return 0;
		final int bits = value ? -1 : 0;
		if (count <= 32) return write(bits, (int) count);

		int c = 0;
		while (count > 32) {
			c += write(bits, 32);
			count -= 32;
		}
		return c;
	}

	/**
	 * Writes between 0 and 32 bits to the stream. Bits are read from the least
	 * significant places.
	 *
	 * @param bits
	 *            the bits to write
	 * @param count
	 *            the number of bits to write
	 *
	 * @return the number of bits written, always count
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default int write(int bits, int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count > 32) throw new IllegalArgumentException("count too great");
		if (count == 0) return 0;
		int c = 0;
		for (count--; count >= 0; count--) {
			c += writeBit(bits >>> count);
		}
		return c;
	}

	/**
	 * Writes between 0 and 64 bits to the stream. Bits are read from the least
	 * significant places.
	 *
	 * @param bits
	 *            the bits to write
	 * @param count
	 *            the number of bits to write
	 *
	 * @return the number of bits written, always count
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default int write(long bits, int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count > 64) throw new IllegalArgumentException("count too great");
		if (count <= 32) {
			return write((int) bits, count);
		} else {
			return write((int)(bits >> 32), count - 32) + write((int) bits, 32);
		}
	}

	/**
	 * Writes the specified number of bits to the stream. Bits are read from the least
	 * significant places.
	 *
	 * @param bits
	 *            the bits to write
	 * @param count
	 *            the number of bits to write
	 *
	 * @return the number of bits written, always count
	 * @throws BitStreamException
	 *             if an exception occurs when writing to the stream
	 */

	default int write(BigInteger bits, int count) throws BitStreamException {
		if (count < 0) throw new IllegalArgumentException("negative count");
		if (count <= 32) return write(bits.intValue(), count);
		if (count <= 64) return write(bits.longValue(), count);
		int c = 0;
		for (count--; count >= 0; count--) {
			c += writeBoolean( bits.testBit(count) );
		}
		return c;
	}

	/**
	 * Flushes this output stream and forces any buffered output bits to be
	 * written out to an underlying stream. This DOES NOT necessarily flush an
	 * underlying stream.
	 *
	 * NOTE: Implementations that write bits to an underlying medium that cannot
	 * persist individual bits may necessarily pad their output to make the
	 * flush possible.
	 *
	 * The number of bits with which the output stream was padded is returned
	 * from the call. Padding is always performed with zero bits.
	 *
	 * @return the number of bits with which the stream was padded to enable
	 *         flushing
	 * @throws BitStreamException
	 *             if an exception occurs flushing the stream
	 */

	default int flush() throws BitStreamException {
		return 0;
	}

	/**
	 * Pads the stream with zeros up to the specified boundary. If the stream is
	 * already positioned on the boundary, no bits will be written.
	 *
	 * NOTE: This method may not be supported by writers that cannot track their
	 * position in the bit stream.
	 *
	 * @param boundary
	 *            the 'size' of boundary
	 * @return the number of zero bits written to the stream
	 * @throws UnsupportedOperationException
	 *             if the stream does not support padding
	 * @throws BitStreamException
	 *             if an exception occurs when padding
	 */

	default int padToBoundary(BitBoundary boundary) throws UnsupportedOperationException, BitStreamException {
		if (boundary == null) throw new IllegalArgumentException("null boundary");
		int bits = boundary.bitsFrom(getPosition());
		if (bits == 0) return 0;
		return (int) writeBooleans(false, bits);
	}

	/**
	 * The position of the writer in the stream; usually, but not necessarily,
	 * the number of bits written. Implementations that cannot report their
	 * position should consistently return -1L.
	 *
	 * @return the position in the stream, or -1L
	 */

	default long getPosition() {
		return -1L;
	}
}

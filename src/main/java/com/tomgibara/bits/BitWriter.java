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

    int writeBit(int bit) throws BitStreamException;

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

    int writeBoolean(boolean bit) throws BitStreamException;

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

    long writeBooleans(boolean value, long count) throws BitStreamException;

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

    int write(int bits, int count) throws BitStreamException;

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

    int write(long bits, int count) throws BitStreamException;

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

    int write(BigInteger bits, int count) throws BitStreamException;

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

    int flush() throws BitStreamException;

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

    int padToBoundary(BitBoundary boundary) throws UnsupportedOperationException, BitStreamException;

	/**
	 * The position of the writer in the stream; usually, but not necessarily,
	 * the number of bits written. Implementations that cannot report their
	 * position should consistently return -1L.
	 *
	 * @return the position in the stream, or -1L
	 */

    long getPosition();
}

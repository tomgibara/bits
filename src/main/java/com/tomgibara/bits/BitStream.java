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


/**
 * Operations common to both reading and writing streams of bits.
 *
 * @author Tom Gibara
 *
 * @see BitReader
 * @see BitWriter
 */
public interface BitStream {

	/**
	 * The position in the stream; usually (but not necessarily) the number of
	 * bits read/written. Implementations that cannot report their position
	 * should consistently return -1L.
	 *
	 * @return the position in the stream, or -1L
	 */

	default long getPosition() {
		return -1L;
	}

	/**
	 * Attempts to move the position of the reader/writer to the new position.
	 * Implementations that cannot adjust their position in this way should
	 * consistently return -1L.
	 *
	 * If this method is supported, the reader/writer is obligated to move the
	 * position as close to the requested new position as it can. It may be that
	 * the position remains unchanged if the reader/writer is already at the end
	 * of the stream, or if the new position is before the current position and
	 * backward seeking is not supported.
	 *
	 * @param newPosition
	 *            the desired position, not negative
	 * @return the resulting position in the stream, or -1L
	 * @throws IllegalArgumentException
	 *             if the new position is negative
	 * @throws BitStreamException
	 *             if an exception occurs when reading/writing the stream
	 */

	default long setPosition(long newPosition) throws BitStreamException, IllegalArgumentException {
		return -1L;
	}

	/**
	 * <p>
	 * Skip the specified number of bits, possibly null. The number of bits
	 * skipped will only be less than the number requested in the event that an
	 * attempt is made to skip past the end of the stream.
	 *
	 * <p>
	 * It should be expected that all {@link BitReader} implementations can skip
	 * forwards over a stream, but in general, the level of support for this
	 * method may vary. {@link BitWriter} implementations that do not track
	 * their in-stream position may throw an
	 * {@link UnsupportedOperationException}.
	 *
	 * <p>
	 * A negative count can be supplied to skip backwards over a stream. Where
	 * this is unsupported an {@link UnsupportedOperationException} should be
	 * thrown.
	 *
	 * @param count
	 *            the number of bits to skip, possibly negative
	 * @return the number of bit skipped
	 * @throws UnsupportedOperationException
	 *             if the seek cannot be supported on this stream
	 * @throws BitStreamException
	 *             if an exception occurs when skipping
	 */
	default long skipBits(long count) throws UnsupportedOperationException, BitStreamException {
		if (count == 0L) return 0L;
		long oldPosition = getPosition();
		if (oldPosition == -1L) throw new UnsupportedOperationException("seeking unsupported");
		long newPosition = oldPosition + count;
		if (newPosition < 0L) newPosition = count < 0L ? 0L : Long.MAX_VALUE;
		newPosition = setPosition(newPosition);
		return newPosition - oldPosition;
	}

	/**
	 * Skips the number of bits necessary to align subsequent reads/writes to a
	 * boundary. Implementations that do not track their in-stream position may
	 * throw an {@link UnsupportedOperationException}.
	 *
	 * @param boundary
	 *            the 'size' of boundary
	 * @return the number of bits skipped to align input/output
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
		long position = getPosition();
		if (position == -1L) throw new UnsupportedOperationException();
		int count = boundary.bitsFrom(position);
		skipBits(count);
		return count;
	}

}

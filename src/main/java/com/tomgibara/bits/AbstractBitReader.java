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
 * A convenient base class for creating {@link BitReader} implementations.
 * Implementations MUST implement either {@link #readBit()} or
 * {@link #read(int)}, SHOULD implement {@link #getPosition()} and/or
 * {@link #setPosition(long)} where practical and MAY override any other methods
 * as necessary, say to improve performance.
 *
 * @author Tom Gibara
 *
 */

public abstract class AbstractBitReader implements BitReader {

    @Override
    public int readBit() {
        return read(1);
    }

    @Override
    public boolean readBoolean() {
    	return readBit() == 1;
    }

    @Override
    public int read(int count) {
    	if (count < 0) throw new IllegalArgumentException("negative count");
    	if (count > 32) throw new IllegalArgumentException("count too great");
        if (count == 0) return 0;
        int acc = readBit();
        while (--count > 0) {
            acc = acc << 1 | readBit();
        }
        return acc;
    }

    @Override
    public long readLong(int count) {
    	if (count < 0) throw new IllegalArgumentException("negative count");
    	if (count > 64) throw new IllegalArgumentException("count too great");
        if (count == 0) return 0L;
        if (count <= 32) return read(count) & 0x00000000ffffffffL;
        return (((long)read(count - 32)) << 32) | (read(32) & 0x00000000ffffffffL);
    }

    @Override
    public BigInteger readBigInt(int count) throws BitStreamException {
    	BitVector bits = new BitVector(count);
    	bits.readFrom(this);
    	return bits.toBigInteger();
    }

    @Override
    public int readUntil(boolean one) throws BitStreamException {
    	int count = 0;
    	while (readBoolean() != one) count++;
    	return count;
    }

	@Override
	public int skipToBoundary(BitBoundary boundary) {
		if (boundary == null) throw new IllegalArgumentException("null boundary");
		int count = bitsToBoundary(boundary);
		skipBits(count);
		return count;
	}

	@Override
	public long skipBits(long count) {
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

	@Override
	public long getPosition() {
		return -1;
	}

	@Override
	public long setPosition(long position) {
		return -1;
	}

	int bitsToBoundary(BitBoundary boundary) {
		long position = getPosition();
		if (position < 0) throw new UnsupportedOperationException("reader does not support position");
		return -(int)position & boundary.mask;
	}

}

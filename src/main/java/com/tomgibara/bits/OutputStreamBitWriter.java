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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A {@link BitWriter} that writes its bits to an OutputStream.
 *
 * @author Tom Gibara
 */

public class OutputStreamBitWriter extends ByteBasedBitWriter {

	// fields

	private static byte[] sZerosBuffer = null;
	private static byte[] sOnesBuffer = null;

	private static final int PAD_BUFFER = 128;
	private static final int PAD_LIMIT = 3;
	private final OutputStream out;

	// constructors

	public OutputStreamBitWriter(OutputStream out) {
		this.out = out;
	}

	// byte based methods

	@Override
	protected void writeByte(int value) throws BitStreamException {
		try {
			out.write(value);
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected void fillBytes(int value, long count) throws BitStreamException {
		try {
			// if it's short, write bytes directly
			if (count < PAD_LIMIT) {
				for (int i = 0; i < count; i++) out.write(value);
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

			// if can, just do it with a single buffer
			if (count <= len) {
				out.write(buffer, 0, (int) count);
				return;
			}

			// write the buffer as many times as we need to
			long limit = count / len;
			for (long i = 0; i < limit; i++) {
				out.write(buffer);
			}
			int r = (int) (count - limit * len);
			if (r != 0) out.write(buffer, 0, r);

		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}

	// accessors

	/**
	 * The OutputStream to which this {@link BitWriter} writes bits.
	 *
	 * @return an output stream, never null
	 */

	public OutputStream getOutputStream() {
		return out;
	}

	// private utility methods

	private byte[] getBuffer(byte b) {
		if (b != 0 & b != -1) return null;
		byte[] buffer;
		switch (b) {
		case 0: buffer = sZerosBuffer; break;
		case 1 : buffer = sOnesBuffer; break;
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

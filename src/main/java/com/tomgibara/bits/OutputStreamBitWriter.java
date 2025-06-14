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

/**
 * A {@link BitWriter} that writes its bits to an OutputStream.
 *
 * @author Tom Gibara
 */

class OutputStreamBitWriter extends ByteBasedBitWriter {

	// fields

	private final OutputStream out;

	// constructors

	OutputStreamBitWriter(OutputStream out) {
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
	protected void writeBytes(byte[] bytes, int offset, int length) throws BitStreamException {
		try {
			out.write(bytes, 0, length);
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

}

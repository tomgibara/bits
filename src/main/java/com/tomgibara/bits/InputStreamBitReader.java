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
import java.io.InputStream;

class InputStreamBitReader extends ByteBasedBitReader {

	private final InputStream in;

	InputStreamBitReader(InputStream in) {
		this.in = in;
	}

	@Override
	protected int readByte() throws BitStreamException {
		try {
			return in.read();
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected long skipBytes(long count) throws BitStreamException {
		try {
			return in.skip(count);
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected long seekByte(long index) throws BitStreamException {
		return -1L;
	}

	/**
	 * The InputStream from which this {@link BitReader} obtains bits.
	 *
	 * @return an input stream, never null
	 */

	public InputStream getInputStream() {
		return in;
	}

}

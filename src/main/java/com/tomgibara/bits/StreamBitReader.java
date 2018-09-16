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

import com.tomgibara.streams.EndOfStreamException;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamException;

class StreamBitReader extends ByteBasedBitReader {

	private final ReadStream stream;

	StreamBitReader(ReadStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		this.stream = stream;
	}

	@Override
	protected int readSourceByte() throws BitStreamException {
		try {
			return stream.readByte() & 0xff;
		} catch (EndOfStreamException e) {
			return -1;
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected long skipSourceBytes(long count) throws BitStreamException {
		long c = 0;
		while (c < count) try {
			stream.readByte();
			c ++;
		} catch (EndOfStreamException e) {
			return c;
		} catch (StreamException e) {
			throw new BitStreamException();
		}
		return c;
	}

	@Override
	protected long seekSourceByte(long index) throws BitStreamException {
		return -1L;
	}

}

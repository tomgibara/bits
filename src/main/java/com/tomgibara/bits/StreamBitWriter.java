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

import com.tomgibara.streams.StreamException;
import com.tomgibara.streams.WriteStream;

class StreamBitWriter extends ByteBasedBitWriter {

	private final WriteStream stream;

	StreamBitWriter(WriteStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		this.stream = stream;
	}

	@Override
	protected void writeByte(int value) throws BitStreamException {
		try {
			stream.writeByte((byte) value);
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected void writeBytes(byte[] bytes, int offset, int length) throws BitStreamException {
		try {
			stream.writeBytes(bytes, offset, length);
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}

}

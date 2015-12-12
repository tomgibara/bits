/*
 * Copyright 2012 Tom Gibara
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

class FileChannelBitReader extends ByteBasedBitReader {

	private final FileChannel channel;
	private final ByteBuffer buffer;
	private long bufferPosition;

	FileChannelBitReader(FileChannel channel, ByteBuffer buffer) {
		this.channel = channel;
		this.buffer = buffer;
		buffer.clear();
		// force buffer to be populated
		buffer.position(buffer.limit());
		bufferPosition = -1L;
	}

	@Override
	protected int readByte() throws BitStreamException {
		if (buffer.hasRemaining()) return buffer.get() & 0xff;
		buffer.limit(buffer.capacity()).position(0);
		try {
			bufferPosition = channel.position();
			channel.read(buffer);
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
		buffer.flip();
		return buffer.hasRemaining() ? buffer.get() & 0xff : -1;
	}

	@Override
	protected long seekByte(long index) throws BitStreamException {
		// first see if index is inside buffer
		if (bufferPosition >= 0) {
			long offset = index - bufferPosition;
			if (offset >= 0 && offset <= buffer.limit()) {
				buffer.position((int) offset);
				return index;
			}
		}
		return seekSlow(index);
	}

	@Override
	protected long skipBytes(long count) throws BitStreamException {
		// optimized code path, where skip fits inside buffer
		if (count <= buffer.remaining()) {
			buffer.position(buffer.position() + (int) count);
			return count;
		}

		// otherwise delegate to seek
		long position;
		if (bufferPosition >= 0) {
			// if we have a buffer, skip relative to it's resolved position
			position = bufferPosition + buffer.position();
		} else {
			try {
				position = channel.position();
			} catch (IOException e) {
				throw new BitStreamException(e);
			}
		}
		return seekSlow(position + count) - position;
	}

	/**
	 * The file channel underlying this BitReader
	 *
	 * @return a FileChannel, never null
	 */

	FileChannel getChannel() {
		return channel;
	}

	private long seekSlow(long index) throws BitStreamException {
		try {
			long length = channel.size();
			if (index >= length) index = length;
			channel.position(index);
			buffer.position(buffer.limit());
			bufferPosition = -1L;
			return index;
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}

}

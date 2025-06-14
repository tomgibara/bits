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

class ByteArrayBitReader extends ByteBasedBitReader {

	private final byte[] bytes;
	private int index;

	ByteArrayBitReader(byte[] bytes) {
		this.bytes = bytes;
		index = 0;
	}

	ByteArrayBitReader(byte[] bytes, long size) {
		super(size);
		this.bytes = bytes;
		index = 0;
	}

	@Override
	protected int readSourceByte() throws BitStreamException {
		return index == bytes.length ? -1 : bytes[index++] & 0xff;
	}

	@Override
	protected long skipSourceBytes(long count) throws BitStreamException {
		long limit = bytes.length - index;
		if (count >= limit) {
			index = bytes.length;
			return limit;
		}
		index += count;
		return count;
	}

	@Override
	protected long seekSourceByte(long index) throws BitStreamException {
		if (index >= bytes.length) {
			this.index = bytes.length;
			return bytes.length;
		} else {
			this.index = (int) index;
			return index;
		}
	}

	public byte[] getBytes() {
		return bytes;
	}

}

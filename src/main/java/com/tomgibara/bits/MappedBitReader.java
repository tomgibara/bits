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

import java.nio.MappedByteBuffer;

//TODO implement this properly
class MappedBitReader implements BitReader {

	private final MappedByteBuffer buffer;
	private long size;
	private long offset;

	private boolean currentSet = false;
	private byte current;

	// constructors

	//TODO should offer constructor without position?
	public MappedBitReader(MappedByteBuffer buffer, long size, long position) {
		this.buffer = buffer;
		setSize(size);
		buffer.position();
		//TODO should check for position > size?
		setPosition(position);
	}

	public void setSize(long size) {
		if ((size+7)/8 > buffer.limit()) throw new IllegalArgumentException();
		if (size < getPosition()) setPosition(size);
		this.size = size;
	}

	long getSize() {
		return size;
	}

	@Override
	public long getPosition() {
		if (currentSet) {
			return (buffer.position()-1) * 8L + offset;
		} else {
			return buffer.position() * 8L + offset;
		}
	}

	@Override
	public long setPosition(long position) {
		if (position < 0) throw new IllegalArgumentException();
		if (position > size) position = size;
		buffer.position((int) (position/8));
		offset = position % 8;
		updateCurrent();
		return position;
	}

	@Override
	public int read(int count) {
		if (count == 0) return 0;
		throw new UnsupportedOperationException();
	}

	private void updateCurrent() {
		if (buffer.hasRemaining()) {
			current = buffer.get();
			currentSet = true;
		} else {
			currentSet = false;
		}
	}

}

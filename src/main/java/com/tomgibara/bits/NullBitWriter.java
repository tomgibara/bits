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
 * A null bit stream that counts the number of bits written.
 *
 * This class is intended to be used in circumstances where adjusting writer
 * capacity may be less efficient than writing twice to a stream.
 *
 * @author Tom Gibara
 *
 */

public class NullBitWriter implements BitWriter {

	private long position = 0;

	@Override
	public int writeBit(int bit) {
		position ++;
		return 1;
	}

	@Override
	public int writeBoolean(boolean bit) {
		position ++;
		return 1;
	}

	@Override
	public long writeBooleans(boolean value, long count) {
		position += count;
		return count;
	}

	@Override
	public int write(int bits, int count) {
		position += count;
		return count;
	}

	@Override
	public int write(long bits, int count) {
		position += count;
		return count;
	}

	@Override
	public int write(BigInteger bits, int count) {
		position += count;
		return count;
	}

	@Override
	public long getPosition() {
		return position;
	}

	public long setPosition(long position) {
		BitStreams.checkPosition(position);
		return this.position = position;
	}

}

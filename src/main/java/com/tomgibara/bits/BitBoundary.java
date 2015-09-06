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

/**
 * A boundary between bits.
 *
 * @author Tom Gibara
 *
 */

public enum BitBoundary {

	/**
	 * A boundary between each bit.
	 */

	BIT(0),

	/**
	 * A boundary between every eight bits
	 */

	BYTE(3),

	/**
	 * A boundary between every sixteen bits.
	 */

	SHORT(4),

	/**
	 * A boundary between every thirty two bits.
	 */

	INT(5),

	/**
	 * A boundary between every sixty four bits.
	 */

	LONG(6);

	final int scale;
	final int mask;

	private BitBoundary(int scale) {
		this.scale = scale;
		this.mask = (1 << scale) - 1;
	}

	/**
	 * The number of bits between the supplied position and the next occurence
	 * of the boundary.
	 *
	 * @param position
	 *            a stream position, not negative
	 *
	 * @return the number of bits to the boundary
	 */

	public int bitsFrom(long position) {
		if (position < 0) throw new UnsupportedOperationException("no position");
		return -(int)position & mask;
	}
}
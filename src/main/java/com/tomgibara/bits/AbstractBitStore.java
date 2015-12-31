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

public abstract class AbstractBitStore implements BitStore {

	public int hashCode() {
		return Bits.bitStoreHasher().intHashValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore)) return false;
		BitStore that = (BitStore) obj;
		if (this.size() != that.size()) return false;
		if (!this.equals().store(that)) return false;
		return true;
	}

	public String toString() {
		int size = size();
		switch (size) {
		case 0 : return "";
		case 1 : return getBit(0) ? "1" : "0";
		default:
			StringBuilder sb = new StringBuilder(size);
			int to = size;
			int from = size & ~63;
			if (from != to) {
				int length = to - from;
				long bits = getBits(from, length) << (64 - length);
				for (; length > 0; length --) {
					sb.append(bits < 0L ? '1' : '0');
					bits <<= 1;
				}
			}
			while (from != 0) {
				from -= 64;
				long bits = getLong(from);
				for (int i = 0; i < 64; i++) {
					sb.append(bits < 0L ? '1' : '0');
					bits <<= 1;
				}
			}
			return sb.toString();
		}
	}

}

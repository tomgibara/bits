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

/**
 * A convenient base class for creating new {@link BitStore} implementations. In
 * addition to all of the default methods provided by the {@link BitStore}
 * interface, this class provides implementations of the standard
 * <code>Object</code> methods, <code>hashCode</code>, <code>equals</code> and
 * <code>toString</code>.
 *
 * @author Tom Gibara
 */

public abstract class AbstractBitStore implements BitStore {

	@Override
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

	@Override
	public String toString() {
		return Bits.toString(this);
	}

}

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

import java.math.BigInteger;

/**
 * An operation that can modify one bit (the destination) based on the value
 * of another (the source).
 */

public enum Operation {

	/**
	 * The destination bit is set to the value of the source bit.
	 */

	SET {
		@Override public boolean booleans(boolean a, boolean b) { return b; }
		@Override public int ints(int a, int b) { return b; }
		@Override public long longs(long a, long b) { return b; }
		@Override public BigInteger bigInts(BigInteger a, BigInteger b) { return b; }

		@Override public BitReader readers(BitReader a, BitReader b) {
			checkReaders(a, b);
			return new OpBitReader.Set(a, b);
		}

		@Override
		public BitStore stores(BitStore a, BitStore b) {
			checkBitStores(a, b);
			return b.immutableView();
		}
	},

	/**
	 * The destination bit is set to true if and only if both the source and destination bits are true.
	 */

	AND {
		@Override public boolean booleans(boolean a, boolean b) { return a && b; }
		@Override public int ints(int a, int b) { return a & b; }
		@Override public long longs(long a, long b) { return a & b; }
		@Override public BigInteger bigInts(BigInteger a, BigInteger b) { return a.and(b); }

		@Override public BitReader readers(BitReader a, BitReader b) {
			checkReaders(a, b);
			return new OpBitReader.And(a, b);
		}

		@Override
		public BitStore stores(BitStore a, BitStore b) {
			return new AbstractBitStore() {

				private final int size = checkBitStores(a, b);

				@Override
				public int size() {
					return size;
				}

				@Override
				public boolean getBit(int index) {
					return a.getBit(index) & b.getBit(index);
				}

				@Override
				public long getBits(int position, int length) {
					return a.getBits(position, length) & b.getBits(position, length);
				}

			};
		}
	},

	/**
	 * The destination bit is set to true if and only if the source and destination bits are not both false.
	 */

	OR {
		@Override public boolean booleans(boolean a, boolean b) { return a || b; }
		@Override public int ints(int a, int b) { return a | b; }
		@Override public long longs(long a, long b) { return a | b; }
		@Override public BigInteger bigInts(BigInteger a, BigInteger b) { return a.or(b); }

		@Override public BitReader readers(BitReader a, BitReader b) {
			checkReaders(a, b);
			return new OpBitReader.Or(a, b);
		}

		@Override
		public BitStore stores(BitStore a, BitStore b) {
			return new AbstractBitStore() {

				private final int size = checkBitStores(a, b);

				@Override
				public int size() {
					return size;
				}

				@Override
				public boolean getBit(int index) {
					return a.getBit(index) | b.getBit(index);
				}

				@Override
				public long getBits(int position, int length) {
					return a.getBits(position, length) | b.getBits(position, length);
				}

			};
		}
	},

	/**
	 * The destination bit is set to true if and only if exactly one of the source and destination bits is true.
	 */

	XOR {
		@Override public boolean booleans(boolean a, boolean b) { return a != b; }
		@Override public int ints(int a, int b) { return a ^ b; }
		@Override public long longs(long a, long b) { return a ^ b; }
		@Override public BigInteger bigInts(BigInteger a, BigInteger b) { return a.xor(b); }

		@Override public BitReader readers(BitReader a, BitReader b) {
			checkReaders(a, b);
			return new OpBitReader.Xor(a, b);
		}

		@Override
		public BitStore stores(BitStore a, BitStore b) {
			return new AbstractBitStore() {

				private final int size = checkBitStores(a, b);

				@Override
				public int size() {
					return size;
				}

				@Override
				public boolean getBit(int index) {
					return a.getBit(index) != b.getBit(index);
				}

				@Override
				public long getBits(int position, int length) {
					return a.getBits(position, length) ^ b.getBits(position, length);
				}

			};
		}
	};

	static final Operation[] values = values();

	static int checkBitStores(BitStore a, BitStore b) {
		if (a == null) throw new IllegalArgumentException("null a");
		if (b == null) throw new IllegalArgumentException("null b");
		int size = a.size();
		if (size != b.size()) throw new IllegalArgumentException("size mismatch");
		return size;
	}

	static void checkReaders(BitReader a, BitReader b) {
		if (a == null) throw new IllegalArgumentException("null a");
		if (b == null) throw new IllegalArgumentException("null b");
	}


	public abstract boolean booleans(boolean a, boolean b);

	public abstract int ints(int a, int b);

	public abstract long longs(long a, long b);

	public abstract BigInteger bigInts(BigInteger a, BigInteger b);

	public abstract BitReader readers(BitReader a, BitReader b);

	public abstract BitStore stores(BitStore a, BitStore b);
}
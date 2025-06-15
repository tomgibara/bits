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

//TODO could optimize further
abstract class ImmutableBit extends SingleBitStore {

	static ImmutableBit instanceOf(boolean bit) {
		return bit ? ImmutableOne.INSTANCE : ImmutableZero.INSTANCE;
	}

	// mutability methods

	@Override
	public BitStore immutableView() {
		return this;
	}

	static final class ImmutableOne extends ImmutableBit {

		static final ImmutableOne INSTANCE = new ImmutableOne();

		// fundamental methods

		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return true;
		}

		// accelerating methods

		@Override
		public long getBits(int position, int length) {
			return getBitsAsInt(position, length);
		}

		@Override
		public int getBitsAsInt(int position, int length) {
			switch (position) {
			case 0:
				switch (length) {
				case 0: return 0;
				case 1: return 1;
				}
				break;
			case 1:
				if (length == 0) return 0;
				break;
			}
			throw new IllegalArgumentException("invalid position and/or length");
		}

		// comparable methods

		@Override
		public int compareNumericallyTo(BitStore that) {
			int p = that.ones().last();
            return switch (p) {
                case -1 -> 1;
                case 0 -> 0;
                default -> -1;
            };
		}

		// view methods

		@Override
		public BitStore flipped() {
			return ImmutableZero.INSTANCE;
		}

		// package scoped methods

		@Override
		boolean getBit() {
			return true;
		}

	}

	static final class ImmutableZero extends ImmutableBit {

		static final ImmutableZero INSTANCE = new ImmutableZero();

		// fundamental methods

		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return false;
		}

		// accelerating methods

		@Override
		public long getBits(int position, int length) {
			return getBitsAsInt(position, length);
		}

		@Override
		public int getBitsAsInt(int position, int length) {
			if (position == 0 && (length == 0 || length == 1) || position == 0 && length == 0) return 0;
			throw new IllegalArgumentException("invalid position and/or length");
		}

		// view methods

		@Override
		public BitStore flipped() {
			return ImmutableOne.INSTANCE;
		}

		// comparable methods

		@Override
		public int compareNumericallyTo(BitStore that) {
			return that.zeros().isAll() ? 0 : -1;
		}

		@Override
		boolean getBit() {
			return false;
		}

	}

}

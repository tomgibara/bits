package com.tomgibara.bits;

//TODO could optimize further
abstract class ImmutableBit extends SingleBitStore {

	public static ImmutableBit instanceOf(boolean bit) {
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
			switch (position) {
			case 0:
				switch (length) {
				case 0: return 0L;
				case 1: return 1L;
				}
				break;
			case 1:
				if (length == 0L) return 0L;
				break;
			}
			throw new IllegalArgumentException();
		}
	
		// comparable methods
		
		@Override
		public int compareNumericallyTo(BitStore that) {
			int p = that.ones().last();
			switch (p) {
			case -1 : return  1;
			case  0 : return  0;
			default : return -1;
			}
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
			switch (position) {
			case 0:
				switch (length) {
				case 0: return 0L;
				case 1: return 0L;
				}
				break;
			case 1:
				if (length == 0L) return 0L;
				break;
			}
			throw new IllegalArgumentException();
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

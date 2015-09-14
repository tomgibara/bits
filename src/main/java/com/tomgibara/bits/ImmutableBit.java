package com.tomgibara.bits;

//TODO could optimize a lot
abstract class ImmutableBit implements BitStore {

	public static ImmutableBit instanceOf(boolean bit) {
		return bit ? ImmutableOne.INSTANCE : ImmutableZero.INSTANCE;
	}
	
	public int size() {
		return 1;
	}

	static final class ImmutableOne extends ImmutableBit {
		
		static final ImmutableOne INSTANCE = new ImmutableOne();
		
		@Override
		public boolean getBit(int index) {
			if (index != 0) throw new IllegalArgumentException();
			return true;
		}
	}

	static final class ImmutableZero extends ImmutableBit {

		static final ImmutableZero INSTANCE = new ImmutableZero();

		@Override
		public boolean getBit(int index) {
			if (index != 0) throw new IllegalArgumentException();
			return false;
		}

	}

}

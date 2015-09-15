package com.tomgibara.bits;

//TODO could optimize further
abstract class ImmutableBit extends AbstractBitStore {

	public static ImmutableBit instanceOf(boolean bit) {
		return bit ? ImmutableOne.INSTANCE : ImmutableZero.INSTANCE;
	}
	
	// fundamental methods
	
	public int size() {
		return 1;
	}
	
	static final class ImmutableOne extends ImmutableBit {
		
		static final ImmutableOne INSTANCE = new ImmutableOne();
		
		// fundamental methods
		
		@Override
		public boolean getBit(int index) {
			if (index != 0) throw new IllegalArgumentException();
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
	
	}

	static final class ImmutableZero extends ImmutableBit {

		static final ImmutableZero INSTANCE = new ImmutableZero();

		// fundamental methods
		
		@Override
		public boolean getBit(int index) {
			if (index != 0) throw new IllegalArgumentException();
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

	}

}

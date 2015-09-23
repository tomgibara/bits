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
	
	// view methods
	
	@Override
	public BitStore range(int from, int to) {
		if (from < 0) throw new IllegalArgumentException();
		if (to > 1) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		return from == to ? Bits.noBits() : this;
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
	
		// comparable methods
		
		@Override
		public int compareTo(BitStore that) {
			int p = that.ones().last();
			switch (p) {
			case -1 : return  1;
			case  0 : return  0;
			default : return -1;
			}
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

		// comparable methods
		
		@Override
		public int compareTo(BitStore that) {
			return that.zeros().isAll() ? 0 : -1;
		}
	}

}

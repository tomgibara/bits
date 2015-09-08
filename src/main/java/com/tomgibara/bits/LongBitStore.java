package com.tomgibara.bits;

final class LongBitStore implements BitStore {

	// statics

	private static long asLong(BitStore store, int maxSize) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size > maxSize) throw new IllegalArgumentException("store size greater than " + maxSize);
		long bits = 0L;
		for (int i = size - 1; i >= 0; i--) {
			bits <<= 1;
			if (store.getBit(i)) bits |= 1L;
		}
		return bits;
	}

	//TODO what's a better implementation?
	private static String toString(long bits, int size) {
		String str = Long.toBinaryString(bits);
		int pad = size - str.length();
		if (pad == 0) return str;
		StringBuilder sb = new StringBuilder(size);
		while (pad-- > 0) sb.append('0');
		sb.append(str);
		return sb.toString();
	}

	// fields

	private long bits;

	// constructors

	LongBitStore() {
		bits = 0L;
	}

	LongBitStore(long bits) {
		this.bits = bits;
	}

	LongBitStore(BitStore store) {
		this.bits = asLong(store, 64);
	}

	// methods

	public long longValue() {
		return bits;
	}

	// bitstore methods

	@Override
	public int size() {
		return 64;
	}

	@Override
	public boolean getBit(int index) {
		checkIndex(index);
		return getBitImpl(index);
	}

	@Override
	public void clear(boolean value) {
		bits = value ? -1L : 0L;
	}

	@Override
	public void setBit(int index, boolean value) {
		checkIndex(index);
		setBitImpl(index, value);
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		checkIndex(index);
		return getThenSetBitImpl(index, value);
	}

	@Override
	public void setStore(int index, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size == 0) return;
		if (index + size > 64) throw new IllegalArgumentException("store size too great");
		setStoreImpl(index, size, store);
	}

	@Override
	public int countOnes() {
		return Long.bitCount(bits);
	}

	@Override
	public boolean isAll(boolean value) {
		return value ? bits == -1L : bits == 0L;
	}

	@Override
	public boolean testEquals(BitStore store) {
		return bits == asLong(store, 64);
	}

	@Override
	public boolean testIntersects(BitStore store) {
		return (bits & asLong(store, 64)) != 0L;
	}

	@Override
	public boolean testContains(BitStore store) {
		return (~bits & asLong(store, 64)) == 0L;
	}

	@Override
	public boolean testComplements(BitStore store) {
		return (bits ^ asLong(store, 64)) == -1L;
	}

	@Override
	public int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return writer.write(bits, 64);
	}

	@Override
	public void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		bits = reader.readLong(64);
	}

	@Override
	public void flip() {
		bits = ~bits;
	}
	
	@Override
	public Ranged range(int from, int to) {
		//TODO create convenience method for this kind of check
		if (from < 0) throw new IllegalArgumentException();
		if (to > 64) throw new IllegalArgumentException();
		if (from > to) throw new IllegalArgumentException();
		return rangeImpl(from, to, true);
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public LongBitStore mutableCopy() {
		return new LongBitStore(bits);
	}

	@Override
	public Ranged immutableView() {
		return rangeImpl(0, 64, false);
	}
	
	@Override
	public String toString() {
		return toString(bits, 64);
	}
	
	// private utility methods

	private void checkIndex(int index) {
		if (index < 0 || index > 63) throw new IllegalArgumentException();
	}

	private boolean getBitImpl(int index) {
		return (1L << index & bits) != 0L;
	}

	private void setBitImpl(int index, boolean value) {
		long mask = 1L << index;
		if (value) {
			bits |= mask;
		} else {
			bits &= ~mask;
		}
	}

	private boolean getThenSetBitImpl(int index, boolean value) {
		long mask = 1L << index;
		long masked = mask & bits;
		if (masked == 0L) {
			// setting a zero bit to one
			if (value) bits |= mask;
			return false;
		} else {
			// setting a one bit to zero
			if (!value) bits &= ~mask;
			return true;
		}
	}
	
	private void setStoreImpl(int index, int size, BitStore store) {
		long acc = 0L;
		for (int i = size - 1; i >= 0; i++) {
			acc <<= 1;
			if (store.getBit(i)) acc |= 1L;
		}
		long mask = (1 << size) - 1L;
		acc <<= index;
		mask <<= index;
		bits = (bits & ~mask) | acc;
	}

	private Ranged rangeImpl(int from, int to, boolean mutable) {
		return new Ranged(from, to, mutable);
	}

	// inner classes
	
	final class Ranged implements BitStore {
		
		private final int from;
		private final int to;
		private final boolean mutable;
		private final long mask;
		
		Ranged(int from, int to, boolean mutable) {
			this.from = from;
			this.to = to;
			this.mutable = mutable;
			if (to == 64) {
				mask = from == 64 ? 0L : -1L << from;
			} else {
				mask = ~(-1L << to) ^ ~(-1L << from);
			}
		}
		
		@Override
		public int size() {
			return to - from;
		}
		
		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return getBitImpl(index + from);
		}

		@Override
		public void clear(boolean value) {
			checkMutable();
			if (value) {
				bits |= mask;
			} else {
				bits &= ~mask;
			}
		}

		@Override
		public void setBit(int index, boolean value) {
			checkIndex(index);
			checkMutable();
			setBitImpl(index + from, value);
		}

		@Override
		public boolean getThenSetBit(int index, boolean value) {
			checkIndex(index);
			checkMutable();
			return getThenSetBitImpl(index + from, value);
		}

		@Override
		public void setStore(int index, BitStore store) {
			if (store == null) throw new IllegalArgumentException("null store");
			int size = store.size();
			if (size == 0) return;
			index += from;
			if (index + size > to) throw new IllegalArgumentException("store size too great");
			setStoreImpl(index, size, store);
		}

		@Override
		public int countOnes() {
			return Long.bitCount(bits & mask);
		}

		@Override
		public boolean isAll(boolean value) {
			return (bits & mask) == (value ? mask : 0L);
		}

		@Override
		public boolean testEquals(BitStore store) {
			return shifted(bits) == asLong(store, to - from);
		}

		@Override
		public boolean testIntersects(BitStore store) {
			return (shifted(bits) & asLong(store, to - from)) != 0L;
		}

		@Override
		public boolean testContains(BitStore store) {
			return (~shifted(bits) & asLong(store, to - from)) == 0L;
		}

		@Override
		public boolean testComplements(BitStore store) {
			return (shifted(bits) ^ asLong(store, to - from)) == shifted(-1L);
		}

		@Override
		public int writeTo(BitWriter writer) {
			if (writer == null) throw new IllegalArgumentException("null writer");
			return writer.write(bits >> from, to - from);
		}

		@Override
		public void readFrom(BitReader reader) {
			if (reader == null) throw new IllegalArgumentException("null reader");
			checkMutable();
			long value = reader.readLong(to - from) << from;
			//TODO can we assume MSBs of value are zeros?
			bits = (bits & ~mask) | (value & mask);
		}

		@Override
		public void flip() {
			checkMutable();
			bits = (bits & ~mask) | (~bits & mask);
		}
		
		@Override
		public BitStore range(int from, int to) {
			if (from < 0) throw new IllegalArgumentException();
			if (from > to) throw new IllegalArgumentException();
			from += this.from;
			to += this.from;
			if (to > this.to) throw new IllegalArgumentException();
			return rangeImpl(from, to, mutable);
		}

		// mutability methods

		@Override
		public boolean isMutable() {
			return mutable;
		}

		@Override
		public Ranged mutableCopy() {
			return new LongBitStore(bits).rangeImpl(from, to, true);
		}
		
		@Override
		public BitStore immutableCopy() {
			return new LongBitStore().rangeImpl(from, to, false);
		}

		@Override
		public String toString() {
			return LongBitStore.toString(shifted(bits), to - from);
		}

		private void checkIndex(int index) {
			if (index < 0 || index + from > to) throw new IllegalArgumentException("invalid index");
		}

		private void checkMutable() {
			if (!mutable) throw new IllegalStateException("immutable");
		}

		private long shifted(long value) {
			return (value & mask) >>> from;
		}
	}
}

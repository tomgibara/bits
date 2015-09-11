package com.tomgibara.bits;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

final class LongBitStore extends AbstractBitStore {

	// statics

	private static long asLong(BitStore store, int maxSize) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size > maxSize) throw new IllegalArgumentException("store size greater than " + maxSize);
		return store.getBits(0, size);
	}

	//TODO what's a better implementation?
	private static String toString(long bits, int size) {
		if (size == 0) return "";
		String str = Long.toBinaryString(bits);
		int pad = size - str.length();
		if (pad == 0) return str;
		StringBuilder sb = new StringBuilder(size);
		while (pad-- > 0) sb.append('0');
		sb.append(str);
		return sb.toString();
	}
	
	// not does not mask off the supplied long - that is responsibility of caller
	static void writeBits(WriteStream writer, long bits, int count) {
		for (int i = (count - 1) & ~7; i >= 0; i -= 8) {
			writer.writeByte((byte) (bits >>> i));
		}
	}

	// not does not mask off the returned long - that is responsibility of caller
	static long readBits(ReadStream reader, int count) {
		long bits = 0L;
		for (int i = (count - 1) >> 3; i >= 0; i --) {
			bits <<= 8;
			bits |= reader.readByte() & 0xff;
		}
		return bits;
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
	public void setBits(int position, long value, int length) {
		if (length < 0) throw new IllegalArgumentException();
		checkPosition(position);
		if (position + length > 64) throw new IllegalArgumentException();
		setBitsImpl(position, value, length);
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
	public boolean isAllOnes() {
		return bits == -1L;
	}
	
	@Override
	public boolean isAllZeros() {
		return bits == 0L;
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

	//TODO could implement an optimized bit writer for openWriter()
	
	@Override
	public int writeTo(BitWriter writer) {
		if (writer == null) throw new IllegalArgumentException("null writer");
		return writer.write(bits, 64);
	}

	@Override
	public void writeTo(WriteStream writer) {
		writer.writeLong(bits);
	}
	
	@Override
	public void readFrom(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		bits = reader.readLong(64);
	}
	
	@Override
	public void readFrom(ReadStream reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		bits = reader.readLong();
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

	@Override
	public byte[] toByteArray() {
		return new byte[] {
				(byte) ( (bits >> 56) & 0xff),
				(byte) ( (bits >> 48) & 0xff),
				(byte) ( (bits >> 40) & 0xff),
				(byte) ( (bits >> 32) & 0xff),
				(byte) ( (bits >> 24) & 0xff),
				(byte) ( (bits >> 16) & 0xff),
				(byte) ( (bits >>  8) & 0xff),
				(byte) (  bits        & 0xff),
		};
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

	private void checkPosition(int position) {
		if (position < 0 || position > 64) throw new IllegalArgumentException();
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
	
	private void setBitsImpl(int position, long value, int length) {
		if (length == 0) return; // nothing to do
		if (length == 64) { // if whole long, we can just assign
			bits = value;
			return;
		}
		int from = position;
		long mask = (-1L << length);
		bits = bits & mask | value << from & ~mask;
	}
	
	private void setStoreImpl(int index, int size, BitStore store) {
		//TODO we get bits in store as a long and use that?
		long acc = 0L;
		for (int i = size - 1; i >= 0; i--) {
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
	
	final class Ranged extends AbstractBitStore {
		
		private final int start;
		private final int finish;
		private final boolean mutable;
		private final long mask;
		
		Ranged(int from, int to, boolean mutable) {
			this.start = from;
			this.finish = to;
			this.mutable = mutable;
			if (to == 64) {
				mask = from == 64 ? 0L : -1L << from;
			} else {
				mask = (-1L << to) ^ (-1L << from);
			}
		}
		
		@Override
		public int size() {
			return finish - start;
		}
		
		@Override
		public boolean getBit(int index) {
			checkIndex(index);
			return getBitImpl(index + start);
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
			setBitImpl(index + start, value);
		}

		@Override
		public boolean getThenSetBit(int index, boolean value) {
			checkIndex(index);
			checkMutable();
			return getThenSetBitImpl(index + start, value);
		}
		
		@Override
		public void setBits(int position, long value, int length) {
			//TODO use adj position method?
			if (position < 0) throw new IllegalArgumentException();
			if (length < 0) throw new IllegalArgumentException();
			position += start;
			if (position + length > 64) throw new IllegalArgumentException();
			checkMutable();
			setBitsImpl(position, value, length);
		}

		@Override
		public void setStore(int position, BitStore store) {
			if (store == null) throw new IllegalArgumentException("null store");
			int size = store.size();
			if (size == 0) return;
			if (position < 0) throw new IllegalArgumentException();
			position += start;
			if (position + size > finish) throw new IllegalArgumentException("store size too great");
			setStoreImpl(position, size, store);
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
		public boolean isAllOnes() {
			return (bits & mask) == mask;
		}
		
		@Override
		public boolean isAllZeros() {
			return (bits & mask) == 0L;
		}

		@Override
		public boolean testEquals(BitStore store) {
			return shifted(bits) == asLong(store, finish - start);
		}

		@Override
		public boolean testIntersects(BitStore store) {
			return (shifted(bits) & asLong(store, finish - start)) != 0L;
		}

		@Override
		public boolean testContains(BitStore store) {
			return (~shifted(bits) & asLong(store, finish - start)) == 0L;
		}

		@Override
		public boolean testComplements(BitStore store) {
			return (shifted(bits) ^ asLong(store, finish - start)) == shifted(-1L);
		}

		@Override
		public int writeTo(BitWriter writer) {
			if (writer == null) throw new IllegalArgumentException("null writer");
			return writer.write(bits >> start, finish - start);
		}
		
		@Override
		public void writeTo(WriteStream writer) {
			writeBits(writer, shifted(bits), finish - start);
		}
		
		@Override
		public void readFrom(BitReader reader) {
			if (reader == null) throw new IllegalArgumentException("null reader");
			checkMutable();
			long value = reader.readLong(finish - start) << start;
			//TODO can we assume MSBs of value are zeros?
			bits = (bits & ~mask) | (value & mask);
		}

		@Override
		public void readFrom(ReadStream reader) {
			if (reader == null) throw new IllegalArgumentException("null reader");
			checkMutable();
			long value = readBits(reader, finish - start) << start;
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
			from += start;
			to += start;
			if (to > finish) throw new IllegalArgumentException();
			return rangeImpl(from, to, mutable);
		}
		
		// mutability methods

		@Override
		public boolean isMutable() {
			return mutable;
		}

		@Override
		public Ranged mutableCopy() {
			return new LongBitStore(bits).rangeImpl(start, finish, true);
		}
		
		@Override
		public BitStore immutableCopy() {
			return new LongBitStore(bits).rangeImpl(start, finish, false);
		}

		@Override
		public String toString() {
			return LongBitStore.toString(shifted(bits), finish - start);
		}

		private void checkIndex(int index) {
			if (index < 0 || index + start > finish) throw new IllegalArgumentException("invalid index");
		}

		private void checkMutable() {
			if (!mutable) throw new IllegalStateException("immutable");
		}

		private long shifted(long value) {
			return (value & mask) >>> start;
		}
	}
}

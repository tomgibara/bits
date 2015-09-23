package com.tomgibara.bits;

import java.util.ListIterator;
import java.util.SortedSet;

import com.tomgibara.bits.ImmutableBit.ImmutableOne;
import com.tomgibara.bits.ImmutableBit.ImmutableZero;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

final class LongBitStore extends AbstractBitStore {

	// statics

	private static void checkIndex(int index) {
		if (index < 0 || index > 63) throw new IllegalArgumentException();
	}

	private static void checkPosition(int position) {
		if (position < 0 || position > 64) throw new IllegalArgumentException();
	}

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
	public long getBits(int position, int length) {
		if (position < 0) throw new IllegalArgumentException();
		if (position + length > 64) throw new IllegalArgumentException();
		switch (length) {
		case 0: return 0;
		case 64: return bits;
		default:
			return bits & ~(-1L << length);
		}
	}

	@Override
	public void clearWith(boolean value) {
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
	public void setStore(int position, BitStore store) {
		if (store == null) throw new IllegalArgumentException("null store");
		int size = store.size();
		if (size == 0) return;
		if (position + size > 64) throw new IllegalArgumentException("store size too great");
		setBitsImpl(position, store.getBits(0, size), size);
	}
	
	@Override
	public LongZeros zeros() {
		return new LongZeros();
	}
	
	@Override
	public LongOnes ones() {
		return new LongOnes();
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
	
	// comparable methods
	
	@Override
	public int compareNumericallyTo(BitStore that) {
		return Long.compareUnsigned(bits, that.asNumber().longValue());
	}
	
	// private utility methods

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
		long mask = ~(-1L << length);
		bits = bits & ~(mask << position) | (value & mask) << position;
	}
	
	private Ranged rangeImpl(int from, int to, boolean mutable) {
		return new Ranged(from, to, mutable);
	}

	// inner classes
	
	private abstract class LongMatches extends BitMatches {

		@Override
		public LongBitStore store() {
			return LongBitStore.this;
		}

		@Override
		public ListIterator<Integer> positions() {
			return new BitStorePositions(this, 0);
		}

		@Override
		public ListIterator<Integer> positions(int position) {
			checkPosition(position);
			return new BitStorePositions(this, position);
		}
		
		@Override
		public SortedSet<Integer> asSet() {
			return new BitStoreSet(this, 0);
		}

	}
	
	private final class LongOnes extends LongMatches {

		@Override
		public boolean bit() {
			return true;
		}
		
		@Override
		public ImmutableOne sequence() {
			return ImmutableOne.INSTANCE;
		}
		
		@Override
		public Ranged.RangedOnes range(int from, int to) {
			return LongBitStore.this.range(from, to).ones();
		}

		@Override
		public boolean isAll() {
			return bits == -1L;
		}
		
		@Override
		public int count() {
			return Long.bitCount(bits);
		}

		@Override
		public int first() {
			return Long.numberOfTrailingZeros(bits);
		}

		@Override
		public int last() {
			return 63 - Long.numberOfLeadingZeros(bits);
		}

		@Override
		public int next(int position) {
			checkPosition(position);
			if (position == 64) return 64;
			long value = bits >>> position;
			return value == 0L ? 64 : position + Long.numberOfTrailingZeros(value);
		}

		@Override
		public int previous(int position) {
			checkPosition(position);
			long value = bits << position;
			return value == 0L ? -1 : position - 1 - Long.numberOfLeadingZeros(value);
		}
		
	}
	
	private final class LongZeros extends LongMatches {

		@Override
		public boolean bit() {
			return false;
		}
		
		@Override
		public ImmutableZero sequence() {
			return ImmutableZero.INSTANCE;
		}
		
		@Override
		public boolean isAll() {
			return bits == 0L;
		}

		@Override
		public Ranged.RangedZeros range(int from, int to) {
			return LongBitStore.this.range(from, to).zeros();
		}

		@Override
		public int count() {
			return Long.bitCount(~bits);
		}

		@Override
		public int first() {
			return Long.numberOfTrailingZeros(~bits);
		}

		@Override
		public int last() {
			return 63 - Long.numberOfLeadingZeros(~bits);
		}

		@Override
		public int next(int position) {
			checkPosition(position);
			if (position == 64) return 64;
			long value = ~bits >>> position;
			return value == 0L ? 64 : position + Long.numberOfTrailingZeros(value);
		}

		@Override
		public int previous(int position) {
			checkPosition(position);
			long value = ~bits << position;
			return value == 0L ? -1 : position - 1 - Long.numberOfLeadingZeros(value);
		}

	}
	
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
		public long getBits(int position, int length) {
			if (position < 0) throw new IllegalArgumentException();
			if (position + length > size()) throw new IllegalArgumentException();
			return shifted(bits);
		}
		@Override
		public void clearWith(boolean value) {
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
			setBitsImpl(position, store.getBits(0, size), size);
		}
		
		@Override
		public RangedOnes ones() {
			return new RangedOnes();
		}
		
		@Override
		public RangedZeros zeros() {
			return new RangedZeros();
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
		public Ranged range(int from, int to) {
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

		// comparable methods
		
		@Override
		public int compareNumericallyTo(BitStore that) {
			return Long.compareUnsigned(shifted(bits), that.asNumber().longValue());
		}
		
		// private utility methods
		
		//TODO change to adjIndex
		private void checkIndex(int index) {
			if (index < 0 || index + start > finish) throw new IllegalArgumentException("invalid index");
		}
		
		private int adjPosition(int position) {
			if (position < 0) throw new IllegalArgumentException("negative position");
			position += start;
			if (position > finish) throw new IllegalArgumentException("position exceeds size");
			return position;
		}

		private void checkMutable() {
			if (!mutable) throw new IllegalStateException("immutable");
		}

		private long shifted(long value) {
			return (value & mask) >>> start;
		}
		
		// ranged inner classes

		private abstract class RangedMatches extends BitMatches {

			@Override
			public Ranged store() {
				return Ranged.this;
			}

			@Override
			public ListIterator<Integer> positions() {
				return new BitStorePositions(this, 0);
			}

			@Override
			public ListIterator<Integer> positions(int position) {
				checkPosition(position);
				return new BitStorePositions(this, position);
			}

			@Override
			public SortedSet<Integer> asSet() {
				return new BitStoreSet(this, 0);
			}

		}
		
		private final class RangedOnes extends RangedMatches {

			@Override
			public boolean bit() {
				return true;
			}

			@Override
			public ImmutableOne sequence() {
				return ImmutableOne.INSTANCE;
			}
			
			@Override
			public RangedOnes range(int from, int to) {
				return Ranged.this.range(from, to).ones();
			}

			@Override
			public boolean isAll() {
				return (bits & mask) == mask;
			}
			
			@Override
			public int count() {
				return Long.bitCount(bits & mask);
			}

			@Override
			public int first() {
				long value = bits & mask;
				return value == 0L ? size() : Long.numberOfTrailingZeros(value) - start;
			}

			@Override
			public int last() {
				long value = bits & mask;
				return value == 0L ? -1 : 63 - start - Long.numberOfLeadingZeros(value);
			}

			@Override
			public int next(int position) {
				int p = adjPosition(position);
				int s = size();
				if (p == finish) return s;
				long value = (bits & mask) >>> p;
				return value == 0L ? s : position + Long.numberOfTrailingZeros(value);
			}

			@Override
			public int previous(int position) {
				int p = adjPosition(position);
				if (p == start) return -1;
				long value = (bits & mask) << 64 - p;
				return value == 0L ? -1 : position - 1 - Long.numberOfLeadingZeros(value);
			}

		}

		private final class RangedZeros extends RangedMatches {

			@Override
			public boolean bit() {
				return false;
			}

			@Override
			public ImmutableZero sequence() {
				return ImmutableZero.INSTANCE;
			}
			
			@Override
			public RangedZeros range(int from, int to) {
				return Ranged.this.range(from, to).zeros();
			}

			@Override
			public boolean isAll() {
				return (~bits & mask) == mask;
			}

			@Override
			public int count() {
				return Long.bitCount(~bits & mask);
			}

			@Override
			public int first() {
				long value = ~bits & mask;
				return value == 0L ? size() : Long.numberOfTrailingZeros(value) - start;
			}

			@Override
			public int last() {
				long value = ~bits & mask;
				return value == 0L ? -1 : 63 - start - Long.numberOfLeadingZeros(value);
			}

			@Override
			public int next(int position) {
				int p = adjPosition(position);
				int s = size();
				if (p == finish) return s;
				long value = (~bits & mask) >>> p;
				return value == 0L ? s : position + Long.numberOfTrailingZeros(value);
			}

			@Override
			public int previous(int position) {
				int p = adjPosition(position);
				if (p == start) return -1;
				long value = (~bits & mask) << 64 - p;
				return value == 0L ? -1 : position - 1 - Long.numberOfLeadingZeros(value);
			}

		}

	}
}

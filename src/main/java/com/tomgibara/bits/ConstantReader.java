package com.tomgibara.bits;

abstract class ConstantReader implements BitReader {

	// fields
	
	private final long length;
	private long position;
	
	// constructors
	
	ConstantReader(long length, long position) {
		this.length = length;
		this.position = position;
	}
	
	// stream methods
	
	@Override
	public long getPosition() {
		return position;
	}
	
	@Override
	public long setPosition(long newPosition) {
		if (newPosition < 0L) throw new IllegalArgumentException();
		if (newPosition > length) throw new IllegalArgumentException();
		return position = newPosition;
	}
	
	// private methods
	
	void advance(long count) {
		long newpos = position + count;
		if (newpos > length) {
			throw new EndOfBitStreamException();
		}
		position = newpos;
	}

	// inner classes
	
	static class OnesReader extends ConstantReader {

		OnesReader(long length, long position) {
			super(length, position);
		}
		
		// reader methods
		
		@Override
		public int readBit() {
			advance(1);
			return 1;
		}
		
		@Override
		public boolean readBoolean() {
			advance(1);
			return true;
		}
		
		@Override
		public int read(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 32) throw new IllegalArgumentException("count too great");
			advance(count);
			return count == 32 ? -1 : ~(-1 << count);

		}
		
		@Override
		public long readLong(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			advance(count);
			return count == 64 ? -1L : ~(-1L << count);
		}
		
		@Override
		public int readUntil(boolean one) throws BitStreamException {
			if (one) {
				advance(1);
				return 1;
			} else {
				throw new EndOfBitStreamException();
			}
		}
		
	}

	static class ZerosReader extends ConstantReader {

		ZerosReader(long length, long position) {
			super(length, position);
		}
		
		// reader methods
		
		@Override
		public int readBit() {
			advance(1);
			return 0;
		}
		
		@Override
		public boolean readBoolean() {
			advance(1);
			return false;
		}
		
		@Override
		public int read(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 32) throw new IllegalArgumentException("count too great");
			advance(count);
			return 0;
		}
		
		@Override
		public long readLong(int count) {
			if (count < 0) throw new IllegalArgumentException("negative count");
			if (count > 64) throw new IllegalArgumentException("count too great");
			advance(count);
			return 0L;
		}
		
		@Override
		public int readUntil(boolean one) throws BitStreamException {
			if (one) {
				throw new EndOfBitStreamException();
			} else {
				advance(1);
				return 1;
			}
		}
		
	}

}

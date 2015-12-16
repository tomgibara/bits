package com.tomgibara.bits;

import static com.tomgibara.bits.Bits.checkBitsLength;

final class CharsBitStore extends AbstractBitStore {

	static final boolean isMutable(CharSequence chars) {
		return (chars instanceof StringBuilder) || (chars instanceof StringBuffer);
	}
	
	private final CharSequence chars;
	private final boolean mutable;
	
	CharsBitStore(CharSequence chars) {
		this(chars, isMutable(chars));
	}
	
	CharsBitStore(CharSequence chars, boolean mutable) {
		this.chars = chars;
		this.mutable = mutable;
	}
	
	@Override
	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public BitStore immutableCopy() {
		return new CharsBitStore(chars.toString(), false);
	}

	@Override
	public BitStore immutableView() {
		return new CharsBitStore(chars, false);
	}

	@Override
	public int size() {
		return chars.length();
	}

	@Override
	public boolean getBit(int index) {
		char c = chars.charAt( adjIndex(index) );
		switch (c) {
		case '0' : return false;
		case '1' : return true;
		default : throw new IllegalStateException("non-binary numeral ' + c + ' at index " + index);
		}
	}

	@Override
	public long getBits(int position, int length) {
		if (position < 0L) throw new IllegalArgumentException("negative position");
		checkBitsLength(length);
		position = chars.length() - position;
		if (position < length) throw new IllegalArgumentException("invalid position");
		if (length == 0) return 0L;
		return Long.parseUnsignedLong(chars.subSequence(position - length, position).toString(), 2);
	}
	
	@Override
	public void setBit(int index, boolean value) {
		if (!mutable) throw new IllegalStateException("immutable");
		index = adjIndex(index);
		if (chars instanceof StringBuilder) {
			((StringBuilder) chars).setCharAt(index, value ? '1' : '0');
		}
	}
	
	@Override
	public BitReader openReader() {
		return new CharBitReader(chars);
	}
	
	@Override
	public BitReader openReader(int position) {
		Bits.checkPosition(position, size());
		return new CharBitReader(chars, chars.length() - position);
	}
	
	@Override
	public String toString() {
		return chars.toString();
	}

	private int adjIndex(int index) {
		return chars.length() - 1 - index;
	}
}

package com.tomgibara.bits;

import java.math.BigInteger;

class BigIntegerBitStore extends AbstractBitStore {

	private final BigInteger bits;
	
	BigIntegerBitStore(BigInteger bits) {
		this.bits = bits;
	}
	
	// fundamentals
	
	@Override
	public int size() {
		return bits.bitLength();
	}
	
	@Override
	public boolean getBit(int index) {
		return bits.testBit(index);
	}
	
	// comparison
	
	@Override
	public int compareNumericallyTo(BitStore that) {
		return bits.compareTo(that.toBigInteger());
	}
	
	// views
	
	@Override
	public BigInteger toBigInteger() {
		return bits;
	}
	
	@Override
	public Number asNumber() {
		return bits;
	}

	// mutability
	
	@Override
	public boolean isMutable() {
		return false;
	}
	
	@Override
	public BitStore mutableCopy() {
		return BitVector.fromBigInteger(bits);
	}

	@Override
	public BitStore immutableCopy() {
		return this;
	}
	
}

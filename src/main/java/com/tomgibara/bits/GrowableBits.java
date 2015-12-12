package com.tomgibara.bits;

public class GrowableBits {

	private final BitVectorWriter writer;
	
	GrowableBits(BitVectorWriter writer) {
		this.writer = writer;
	}
	
	public BitWriter writer() {
		return writer;
	}

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * continue to be written to after this method has been called.
	 *
	 * @return an immutable {@link BitVector} containing the written bits
	 */

	public BitVector toImmutableBitVector() {
		return writer.toImmutableBitVector();
	}

	/**
	 * Obtain the bits which have been written by the writer. The bit writer may
	 * not be written to after this method has been called. Attempting to do so
	 * will raise an <code>IllegalStateException</code>.
	 *
	 * @return a mutable {@link BitVector} containing the written bits
	 */

	public BitVector toMutableBitVector() {
		return writer.toMutableBitVector();
	}
}

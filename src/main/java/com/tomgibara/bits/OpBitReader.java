package com.tomgibara.bits;

import java.math.BigInteger;

// can we produce versions that operate sparsely?
abstract class OpBitReader implements BitReader {

	private final BitReader a;
	private final BitReader b;
	private boolean eos = false;
	
	OpBitReader(BitReader a, BitReader b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public int readBit() {
		checkEos();
		int ai;
		int bi;
		try {
			ai = a.readBit();
			bi = b.readBit();
		} catch (EndOfBitStreamException e) {
			eos = true;
			throw e;
		}
		return combine(ai, bi);
	}
	
	@Override
	public int read(int count) {
		checkEos();
		int ai;
		int bi;
		try {
			ai = a.read(count);
			bi = b.read(count);
		} catch (EndOfBitStreamException e) {
			eos = true;
			throw e;
		}
		return combine(ai, bi);
	}
	
	@Override
	public boolean readBoolean() {
		checkEos();
		boolean ab;
		boolean bb;
		try {
			ab = a.readBoolean();
			bb = b.readBoolean();
		} catch (EndOfBitStreamException e) {
			eos = true;
			throw e;
		}
		return combine(ab, bb);
	}
	
	@Override
	public long readLong(int count) {
		checkEos();
		long al;
		long bl;
		try {
			al = a.readLong(count);
			bl = b.readLong(count);
		} catch (EndOfBitStreamException e) {
			eos = true;
			throw e;
		}
		return combine(al, bl);
	}
	
	@Override
	public BigInteger readBigInt(int count) throws BitStreamException {
		checkEos();
		BigInteger ai;
		BigInteger bi;
		try {
			ai = a.readBigInt(count);
			bi = b.readBigInt(count);
		} catch (EndOfBitStreamException e) {
			eos = true;
			throw e;
		}
		return combine(ai, bi);
	}

	@Override
	public long skipBits(long count) {
		if (count < 0L) throw new UnsupportedOperationException("negative count not supported");
		if (eos) return 0L;
		long ac = a.skipBits(count);
		long bc = a.skipBits(count);
		if (ac < count || bc < count) {
			eos = true;
			return Math.min(ac, bc);
		}
		return count;
	}
	
	abstract int combine(int i, int j);

	abstract long combine(long i, long j);

	abstract boolean combine(boolean i, boolean j);

	abstract BigInteger combine(BigInteger i, BigInteger j);
	
	private void checkEos() {
		if (eos) throw new EndOfBitStreamException();
	}
	
	static final class Set extends OpBitReader {

		Set(BitReader a, BitReader b) { super(a, b); }

		@Override
		int combine(int i, int j) { return j; }

		@Override
		long combine(long i, long j) { return j; }

		@Override
		boolean combine(boolean i, boolean j) { return j; }

		@Override
		BigInteger combine(BigInteger i, BigInteger j) { return j; }

	}

	static final class And extends OpBitReader {

		And(BitReader a, BitReader b) { super(a, b); }

		@Override
		int combine(int i, int j) { return i & j; }

		@Override
		long combine(long i, long j) { return i & j; }

		@Override
		boolean combine(boolean i, boolean j) { return i && j; }

		@Override
		BigInteger combine(BigInteger i, BigInteger j) { return i.and(j); }

	}

	static final class Or extends OpBitReader {

		Or(BitReader a, BitReader b) { super(a, b); }

		@Override
		int combine(int i, int j) { return i | j; }

		@Override
		long combine(long i, long j) { return i | j; }

		@Override
		boolean combine(boolean i, boolean j) { return i || j; }

		@Override
		BigInteger combine(BigInteger i, BigInteger j) { return i.or(j); }

	}

	static final class Xor extends OpBitReader {

		Xor(BitReader a, BitReader b) { super(a, b); }

		@Override
		int combine(int i, int j) { return i ^ j; }

		@Override
		long combine(long i, long j) { return i ^ j; }

		@Override
		boolean combine(boolean i, boolean j) { return i != j; }

		@Override
		BigInteger combine(BigInteger i, BigInteger j) { return i.xor(j); }

	}

}

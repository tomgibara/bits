package com.tomgibara.bits;

import java.math.BigInteger;

import com.tomgibara.streams.WriteStream;

final class ImmutableBitStore extends AbstractBitStore {

	/*
	private static final ImmOp IMM_AND = new ImmOp(Operation.AND);
	private static final ImmOp IMM_OR = new ImmOp(Operation.OR);
	private static final ImmOp IMM_XOR = new ImmOp(Operation.XOR);
	private static final ImmOp IMM_SET = new ImmOp(Operation.SET);
	*/

	private final BitStore store;
	
	ImmutableBitStore(BitStore store) {
		this.store = store;
	}
	
	@Override
	public int size() {
		return store.size();
	}

	@Override
	public boolean getBit(int index) {
		return store.getBit(index);
	}

	@Override
	public Matches ones() {
		return new ImmutableMatches(this, store.ones());
	}
	
	@Override
	public Matches zeros() {
		return new ImmutableMatches(this, store.zeros());
	}
	
	@Override
	public boolean testEquals(BitStore s) {
		return store.testEquals(s);
	}

	@Override
	public boolean testIntersects(BitStore s) {
		return store.testIntersects(s);
	}

	@Override
	public boolean testContains(BitStore s) {
		return store.testContains(s);
	}

	@Override
	public boolean testComplements(BitStore store) {
		return store.testComplements(store);
	}

	@Override
	public int writeTo(BitWriter writer) {
		return store.writeTo(writer);
	}

	@Override
	public void writeTo(WriteStream writer) {
		store.writeTo(writer);
	}

	@Override
	public BitReader openReader() {
		return store.openReader();
	}

	@Override
	public BitReader openReader(int position) {
		return store.openReader(position);
	}

	@Override
	public long getBits(int position, int length) {
		return store.getBits(position, length);
	}

	@Override
	public byte getByte(int position) {
		return store.getByte(position);
	}

	@Override
	public int getInt(int position) {
		return store.getInt(position);
	}

	@Override
	public short getShort(int position) {
		return store.getShort(position);
	}

	@Override
	public long getLong(int position) {
		return store.getLong(position);
	}

	/*
	@Override
	public Op and() {
		return IMM_AND;
	}
	
	@Override
	public Op or() {
		return IMM_OR;
	}
	
	@Override
	public Op xor() {
		return IMM_XOR;
	}
	
	@Override
	public Op set() {
		return IMM_SET;
	}
	*/

	@Override
	public BitStore range(int from, int to) {
		return store.range(from, to).immutableView();
	}

	@Override
	public BigInteger toBigInteger() {
		return store.toBigInteger();
	}

	@Override
	public String toString() {
		return store.toString();
	}

	@Override
	public byte[] toByteArray() {
		return store.toByteArray();
	}

	@Override
	public Number asNumber() {
		return store.asNumber();
	}

	@Override
	public BitStore mutableCopy() {
		return store.mutableCopy();
	}

	@Override
	public BitStore immutableCopy() {
		return store.immutableCopy();
	}

	@Override
	public BitStore immutableView() {
		return store.immutableView();
	}

	@Override
	public int hashCode() {
		return store.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return store.equals(obj);
	}

	/*
	private static class ImmOp extends BitStore.Op {

		private final Operation op;
		
		ImmOp(Operation op) {
			this.op = op;
		}
		
		@Override
		Operation getOperation() {
			return op;
		}

		@Override
		void with(boolean value) {
			failMutability();
		}

		@Override
		void withBit(int position, boolean value) {
			failMutability();
		}

		@Override
		boolean getThenWithBit(int position, boolean value) {
			failMutability();
			return false;
		}

		@Override
		void withByte(int position, byte value) {
			failMutability();
		}

		@Override
		void withShort(int position, short value) {
			failMutability();
		}

		@Override
		void withInt(int position, short value) {
			failMutability();
		}

		@Override
		void withLong(int position, short value) {
			failMutability();
		}

		@Override
		void withBits(int position, long value, int length) {
			failMutability();
		}

		@Override
		void withVector(BitVector vector) {
			failMutability();
		}

		@Override
		void withVector(int position, BitVector vector) {
			failMutability();
		}

		@Override
		void withStore(BitStore store) {
			failMutability();
		}

		@Override
		void withStore(int position, BitStore store) {
			failMutability();
		}

		@Override
		void withBytes(int position, byte[] bytes, int offset, int length) {
			failMutability();
		}
		
	}
	*/
	

}

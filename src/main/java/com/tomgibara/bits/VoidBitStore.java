/*
 * Copyright 2015 Tom Gibara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tomgibara.bits;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;


final class VoidBitStore implements BitStore {

	// statics

	private static final byte[] NO_BYTES = {};

	static final VoidBitStore MUTABLE = new VoidBitStore(true);
	static final VoidBitStore IMMUTABLE = new VoidBitStore(false);
	
	private static void checkPosition(int position) {
		if (position != 0) throw new IllegalArgumentException("invalid position");
	}
	
	// fields
	
	private final boolean mutable;

	// constructors
	
	private VoidBitStore(boolean mutable) {
		this.mutable = mutable;
	}
	
	// fundamental methods
	
	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean getBit(int index) {
		throw new IllegalArgumentException();
	}

	@Override
	public void setBit(int index, boolean value) {
		throw new IllegalArgumentException();
	}
	
	// accelerating methods
	
	@Override
	public long getBits(int position, int length) {
		return 0L;
	}
	
	// accelerating mutation methods

	@Override
	public void fill() { }
	
	@Override
	public void clear() { }
	
	@Override
	public void flip() { }
	
	// operations
	
	// matching
	
	// testing

	@Override
	public Tests equals() {
		return VoidTests.EQUALS;
	}
	
	@Override
	public Tests excludes() {
		return VoidTests.EXCLUDES;
	}
	
	@Override
	public Tests contains() {
		return VoidTests.CONTAINS;
	}
	
	@Override
	public Tests complements() {
		return VoidTests.COMPLEMENTS;
	}
	
	// IO
	
	@Override
	public BitWriter openWriter() {
		return VoidWriter.INSTANCE;
	}
	
	@Override
	public BitWriter openWriter(int finalPos, int initialPos) {
		checkPosition(finalPos);
		checkPosition(initialPos);
		return VoidWriter.INSTANCE;
	}
	
	@Override
	public BitReader openReader() {
		return VoidReader.INSTANCE;
	}

	@Override
	public BitReader openReader(int finalPos, int initialPos) {
		checkPosition(finalPos);
		checkPosition(initialPos);
		return VoidReader.INSTANCE;
	}
	
	@Override
	public int writeTo(BitWriter writer) { return 0; }
	
	@Override
	public void readFrom(BitReader reader) { }
	
	@Override
	public void writeTo(WriteStream writer) { }
	
	@Override
	public void readFrom(ReadStream reader) { }

	// views
	
	@Override
	public BitStore range(int from, int to) {
		if (from != 0 || to != 0) throw new IllegalArgumentException();
		return this;
	}
	
	@Override
	public byte[] toByteArray() { return NO_BYTES; }
	
	@Override
	public BigInteger toBigInteger() { return BigInteger.ZERO; }
	
	@Override
	public String toString(int radix) { return ""; }
	
	@Override
	public Number asNumber() { return VoidNumber.INSTANCE; }
	
	@Override
	public List<Boolean> asList() { return Collections.emptyList(); }
	
	// comparable methods

	@Override
	public int compareNumericallyTo(BitStore that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return that.zeros().isAll() ? 0 : -1;
	}
	
	@Override
	public int compareLexicallyTo(BitStore that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return that.size() == 0 ? 0 : -1;
	}
	
	// mutable methods
	
	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public BitStore mutableCopy() {
		return MUTABLE;
	}

	@Override
	public BitStore immutableCopy() {
		return IMMUTABLE;
	}

	@Override
	public BitStore immutableView() {
		return IMMUTABLE;
	}
	
	// object methods
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore)) return false;
		BitStore that = (BitStore) obj;
		return that.size() == 0;
	}
	
	@Override
	public String toString() {
		return "";
	}

	// inner classes
	
	private static class VoidTests implements Tests {
		
		static final VoidTests EQUALS = new VoidTests(Test.EQUALS);
		static final VoidTests EXCLUDES = new VoidTests(Test.EXCLUDES);
		static final VoidTests CONTAINS = new VoidTests(Test.CONTAINS);
		static final VoidTests COMPLEMENTS = new VoidTests(Test.COMPLEMENTS);
		
		private final Test test;
		
		private VoidTests(Test test) {
			this.test = test;
		}
		
		@Override
		public Test getTest() {
			return test;
		}
		
		@Override
		public boolean bits(long bits) {
			return true;
		}
		
		@Override
		public boolean store(BitStore store) {
			if (store == null) throw new IllegalArgumentException("null store");
			if (store.size() != 0) throw new IllegalArgumentException("store not zero length");
			return true;
		}
	}
	
	private static class VoidWriter implements BitWriter {
		
		static final VoidWriter INSTANCE = new VoidWriter();
		
		private VoidWriter() { }
		
		@Override
		public int writeBit(int bit) throws BitStreamException {
			throw new EndOfBitStreamException();
		}
		
		@Override
		public long getPosition() {
			return 0L;
		}
		
		@Override
		public long setPosition(long newPosition) {
			BitStreams.checkPosition(newPosition);
			return 0L;
		}
		
		@Override
		public int padToBoundary(BitBoundary boundary) {
			return 0;
		}

	}
	
	private static class VoidReader implements BitReader {

		static final VoidReader INSTANCE = new VoidReader();
		
		private VoidReader() { }
		
		@Override
		public int readBit() throws BitStreamException {
			throw new EndOfBitStreamException();
		}
		
		@Override
		public long getPosition() {
			return 0L;
		}
		
		@Override
		public long setPosition(long newPosition) {
			if (newPosition < 0L) throw new IllegalArgumentException();
			return 0L;
		}
		
		@Override
		public int skipToBoundary(BitBoundary boundary) {
			return 0;
		}
	}
	
	private static class VoidNumber extends Number {
		
		private static final long serialVersionUID = -8990283188899943608L;

		public static final VoidNumber INSTANCE = new VoidNumber();
		
		private VoidNumber() { }

		@Override
		public int intValue() { return 0; }

		@Override
		public long longValue() { return 0L; }

		@Override
		public float floatValue() { return 0.0f; }

		@Override
		public double doubleValue() { return 0.0; }

	}
}

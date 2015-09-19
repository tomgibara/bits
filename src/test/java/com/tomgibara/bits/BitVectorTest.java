/*
 * Copyright 2010 Tom Gibara
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;

import com.tomgibara.bits.BitStore.Op;
import com.tomgibara.bits.BitStore.Operation;
import com.tomgibara.bits.BitStore.Test;
import com.tomgibara.streams.InputReadStream;
import com.tomgibara.streams.OutputWriteStream;

public class BitVectorTest extends BitStoreTest {

	static BitVector[] randomVectorFamily(int length, int size) {
		BitVector v = randomVector(length);
		BitVector[] vs = new BitVector[size + 1];
		vs[0] = v;
		for (int i = 0; i < size; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			vs[i+1] = v.range(a, b);
		}
		return vs;
	}

	static BitVector[] randomVectorFamily(int size) {
		return randomVectorFamily(random.nextInt(1000), size);
	}

	static BitVector randomVector(int length) {
		//TODO optimize when factory methods are available
		BitVector vector = new BitVector(length);
		for (int i = 0; i < length; i++) {
			vector.setBit(i, random.nextBoolean());
		}
		return vector;
	}

	static BitVector randomVector() {
		return randomVector(random.nextInt(1000));
	}

	@Override
	BitVector newStore(int size) {
		return new BitVector(size);
	}
	
	@Override
	BitStore newStore(BitStore s) {
		return BitVector.fromStore(s);
	}

	public void testToString() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testToString(vs[j]);
			}
		}
	}

	private void testToString(BitVector v) {
		String str = v.toString();
		assertEquals(str.length(), v.size());
		assertEquals(v, new BitVector(str));
	}

	public void testGet() throws Exception {
		//72 long
		BitVector v = new BitVector("100101111011011100001101011001100000101110001011100001110011101101100010");
		assertEquals((byte)new BigInteger("10010111", 2).intValue(), v.getByte(64));
	}

	public void testToArrays() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testToByteArray(vs[j]);
				testToIntArray(vs[j]);
				testToLongArray(vs[j]);
			}
		}
	}

	private void testToByteArray(BitVector v) {
		String s = v.toString();
		int d = s.length() % 8;
		if (d != 0) {
			StringBuilder sb = new StringBuilder(s.length() + 8 - d);
			for (; d < 8; d++) sb.append('0');
			s = sb.append(s).toString();
		}
		byte[] bytes = new byte[s.length()/8];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(s.substring(i * 8, (i+1)*8), 2);
		}
		assertTrue(Arrays.equals(bytes, v.toByteArray()));
	}

	private void testToIntArray(BitVector v) {
		int size = v.size();
		int[] ints = v.toIntArray();
		int len = ints.length;
		if (size == 0 && len == 0) return;
		int offset = size % 32;
		if (offset == 0) offset = 32;
		for (int i = 0; i < len; i++) {
			int position = size - offset - i * 32;
			int bits = Math.min(size - position, 32);
			assertEquals((int) v.getBits(position, bits), ints[i]);
		}
		assertTrue(len * 32 >= size);
	}

	private void testToLongArray(BitVector v) {
		int size = v.size();
		long[] longs = v.toLongArray();
		int len = longs.length;
		if (size == 0 && len == 0) return;
		int offset = size % 64;
		if (offset == 0) offset = 64;
		for (int i = 0; i < len; i++) {
			int position = size - offset - i * 64;
			int bits = Math.min(size - position, 64);
			assertEquals(v.getBits(position, bits), longs[i]);
		}
		assertTrue(len * 64 >= size);
	}

	public void testToBigInteger() {
		BitVector v = new BitVector(1024);
		v.clearWithOnes();
		int f = 512;
		int t = 512;
		BigInteger i = BigInteger.ONE;
		while (f > 0 && t < 1024) {
			//evens
			BitVector e = v.range(f, t);
			assertEquals(i.subtract(BigInteger.ONE), e.toBigInteger());
			i = i.shiftLeft(1);
			f--;
			//odds
			BitVector o = v.range(f, t);
			assertEquals(i.subtract(BigInteger.ONE), o.toBigInteger());
			i = i.shiftLeft(1);
			t++;
		}

	}

	public void testBitCounts() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testBitCounts(vs[j]);
			}
		}
	}

	private void testBitCounts(BitVector v) {
		String str = v.toString();
		int totalOneCount = str.replace("0", "").length();
		int totalZeroCount = str.replace("1", "").length();
		assertEquals(v.size(), v.ones().count() + v.zeros().count());
		assertEquals(totalOneCount, v.ones().count());
		assertEquals(totalZeroCount, v.zeros().count());
		int reps = v.size();
		for (int i = 0; i < reps; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			String s = str.substring(str.length()-b, str.length()-a);
			int oneCount = s.replace("0", "").length();
			int zeroCount = s.replace("1", "").length();
			assertEquals(oneCount, v.range(a,b).ones().count());
			assertEquals(zeroCount, v.range(a, b).zeros().count());
		}
	}

	public void testOverlapping() {
		BitVector v = new BitVector("1010101010101010");
		BitVector w = v.range(0, 15);
		v.xor().withStore(1, w);
		assertEquals(new BitVector("1111111111111110"), v);

		v = new BitVector("1010101010101010");
		w = v.range(1, 16);
		v.xor().withStore(0, w);
		assertEquals(new BitVector("1111111111111111"), v);
	}

	public void testCloneViewAndCopy() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testCloneViewAndCopy(vs[j]);
			}
		}
	}

	private void testCloneViewAndCopy(BitVector v) {
		BitVector cl = v.clone();
		assertEquals(v, cl);
		assertNotSame(v, cl);

		BitVector cp = v.copy();
		assertEquals(v, cp);
		assertNotSame(v, cp);

		BitVector vw = v.view();
		assertEquals(v, vw);
		assertNotSame(v, vw);

		//check clone and view are backed by same data
		cl.xor().with(true);
		cp.xor().withStore(vw);
		assertEquals(cp.size(), cp.ones().count());

		assertTrue(v.isMutable());
		BitVector mu = v.mutable();
		assertSame(v, mu);
		BitVector im = v.immutable();
		assertNotSame(v, im);
		assertFalse(im.isMutable());
		mu = im.mutable();
		assertNotSame(im, mu);
		assertTrue(mu.isMutable());
	}


	public void testResizedCopy() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testResizedCopy(vs[j]);
			}
		}
	}

	private void testResizedCopy(BitVector v) {
		int size = v.size();

		int a = size == 0 ? 0 : random.nextInt(size);
		BitVector w = v.resizedCopy(a);
		assertEquals(v.range(0, w.size()), w);

		w = v.resizedCopy(size);
		assertEquals(v, w);

		a = size == 0 ? 1 : size + random.nextInt(size);
		w = v.resizedCopy(a);
		assertEquals(v, w.range(0, size));
		w.range(size, w.size()).isAllZeros();
	}

	public void testMutability() {
		BitVector v = new BitVector(1).immutable();
		for (Operation operation : Operation.values) {
			Op op = v.op(operation);
			try {
				op.with(true);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withBit(0, true);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withBits(0, 1L, 1);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withStore(v);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withStore(0, v);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
		}
		try {
			v.duplicate(false, true);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
	}

	public void testSerialization() throws Exception {
		BitVector v1 = randomVector(1000);
		BitVector w1 = v1.view();
		BitVector x1 = v1.copy();
		BitVector y1 = x1.immutableView();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(v1);
		oout.writeObject(w1);
		oout.writeObject(x1);
		oout.writeObject(y1);
		oout.close();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ObjectInputStream oin = new ObjectInputStream(in);
		BitVector v2 = (BitVector) oin.readObject();
		BitVector w2 = (BitVector) oin.readObject();
		BitVector x2 = (BitVector) oin.readObject();
		BitVector y2 = (BitVector) oin.readObject();
		oin.close();

		assertNotSame(v1, v2);
		assertNotSame(w1, w2);
		assertNotSame(x1, x2);
		assertNotSame(y1, y2);

		assertEquals(v1, v2);
		assertEquals(w1, w2);
		assertEquals(x1, x2);
		assertEquals(y1, y2);

		assertTrue(v2.isMutable());
		assertTrue(w2.isMutable());
		assertTrue(x2.isMutable());
		assertFalse(y2.isMutable());

		assertTrue(x2.equals(v2));
		w2.clearWithOnes();
		assertEquals(1000, v2.ones().count());
		assertFalse(x2.equals(v2));
		x2.clearWithOnes();
		assertEquals(1000, y2.ones().count());

	}

	public void testIsAll() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testIsAll(vs[j]);
			}
		}
	}

	private void testIsAll(BitVector v) {
		v.clearWithZeros();
		assertTrue(v.isAllZeros());
		assertFalse(v.size() != 0 && v.isAllOnes());
		v.clearWithOnes();
		assertTrue(v.isAllOnes());
		assertFalse(v.size() != 0 && v.isAllZeros());
		int reps = v.size();
		for (int i = 0; i < reps; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			v.range(a, b).clearWithZeros();
			assertTrue(v.range(a,b).isAllZeros());
			v.range(a, b).clearWithOnes();
			assertTrue(v.range(a, b).isAllOnes());
		}
	}

	public void testAlignedCopy() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testAlignedCopy(vs[j]);
			}
		}
	}

	private void testAlignedCopy(BitVector v) {
		BitVector cm = v.alignedCopy(true);
		BitVector ci = v.alignedCopy(false);
		assertTrue(cm.isAligned());
		assertTrue(cm.isMutable());
		assertTrue(ci.isAligned());
		assertFalse(ci.isMutable());
		assertEquals(v, cm);
		assertEquals(v, ci);
		assertNotSame(v, cm);
		assertNotSame(v, ci);
	}

	public void testCompare() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testCompare(vs[j]);
			}
		}
	}

	private void testCompare(BitVector v) {
		int size = v.size();
		assertTrue(v.equals().store(v));
		assertTrue(v.test(Test.EQUALS).store(v));
		assertTrue(v.contains().store(v));
		assertTrue(v.test(Test.CONTAINS).store(v));
		if (!v.isAllZeros()) {
			assertFalse(v.excludes().store(v));
			assertFalse(v.test(Test.EXCLUDES).store(v));
		}
		assertEquals(size == 0, v.test(Test.COMPLEMENTS).store(v));

		BitVector w = v.alignedCopy(true);
		assertTrue(v.equals().store(w));
		assertTrue(v.test(Test.EQUALS).store(w));
		assertTrue(w.equals().store(v));
		assertTrue(w.test(Test.EQUALS).store(v));
		assertTrue(v.contains().store(w));
		assertTrue(v.test(Test.CONTAINS).store(w));
		assertTrue(w.contains().store(v));
		assertTrue(w.test(Test.CONTAINS).store(v));
		if (!v.isAllZeros()) {
			assertFalse(v.excludes().store(w));
			assertFalse(v.test(Test.EXCLUDES).store(w));
			assertFalse(w.excludes().store(v));
			assertFalse(w.test(Test.EXCLUDES).store(v));
		}
		assertEquals(size == 0, w.test(Test.COMPLEMENTS).store(v));

		w = v.alignedCopy(true);
		for (int i = 0; i < size; i++) {
			w.setBit(i, true);
			assertTrue( w.contains().store(v) );
			assertTrue( w.test(Test.CONTAINS).store(v) );
			assertTrue( v.equals().store(w) || !v.contains().store(w) );
			assertTrue( v.test(Test.EQUALS).store(w) || !v.test(Test.CONTAINS).store(w) );
		}

		w = v.alignedCopy(true);
		for (int i = 0; i < size; i++) {
			w.setBit(i, false);
			assertTrue( v.contains().store(w) );
			assertTrue( v.test(Test.CONTAINS).store(w) );
			assertTrue( w.equals().store(v) || !w.contains().store(v) );
			assertTrue( w.test(Test.EQUALS).store(v) || !w.test(Test.CONTAINS).store(v) );
		}

		if (size != 0) {
			BitVector u = v.mutableCopy();
			u.flip();
			assertTrue(u.test(Test.COMPLEMENTS).store(v));
			assertTrue(v.test(Test.COMPLEMENTS).store(u));
			u = v.alignedCopy(true);
			u.flip();
			assertTrue(u.test(Test.COMPLEMENTS).store(v));
			assertTrue(v.test(Test.COMPLEMENTS).store(u));
		}
	}

	public void testLongTest() {
		BitVector v = new BitVector("100101010110");
		BitVector u = new BitVector("100101010111");
		BitVector w = new BitVector("011010101001");

		assertTrue(u.contains().store(v));
		assertTrue(u.contains().store(v));
		assertTrue(u.contains().bits(v.asNumber().longValue()));
		assertFalse(v.contains().store(u));
		assertFalse(v.contains().store(u));
		assertFalse(v.contains().bits(u.asNumber().longValue()));

		assertFalse(u.excludes().store(v));
		assertFalse(u.excludes().store(v));
		assertFalse(u.excludes().bits(v.asNumber().longValue()));
		assertFalse(u.excludes().bits(v.asNumber().longValue()));
		assertFalse(v.excludes().store(u));
		assertFalse(v.excludes().store(u));
		assertFalse(v.excludes().bits(u.asNumber().longValue()));

		assertTrue(v.excludes().store(w));
		assertTrue(v.excludes().bits(w.asNumber().longValue()));
		assertTrue(w.excludes().store(v));
		assertTrue(w.excludes().bits(v.asNumber().longValue()));

		assertTrue(w.complements().store(v));
		assertTrue(w.complements().bits(v.asNumber().longValue()));

		assertTrue(u.range(0, 1).equals().bits(1L));
		assertTrue(u.range(0, 1).contains().bits(1L));
		assertFalse(u.range(0, 1).excludes().bits(1L));
		assertFalse(u.range(0, 1).complements().bits(1L));
		assertFalse(v.range(0, 1).equals().bits(1L));
		assertFalse(v.range(0, 1).contains().bits(1L));
		assertTrue(v.range(0, 1).excludes().bits(1L));
		assertTrue(v.range(0, 1).complements().bits(1L));
	}

	public void testCompareTo() {
		BitVector[] vs = randomVectorFamily(500);
		for (int i = 1; i < vs.length; i++) {
			for (int j = 0; j < i; j++) {
				testCompareTo(vs[i], vs[j]);
			}
		}

	}

	private void testCompareTo(BitVector u, BitVector v) {
		int cn = Integer.signum(u.toBigInteger().compareTo(v.toBigInteger()));
		int cl = Integer.signum(u.toString().compareTo(v.toString()));
		assertEquals(cn, u.compareTo(v));
		assertEquals(cn, BitVector.sNumericComparator.compare(u, v));
		assertEquals(cl, BitVector.sLexicalComparator.compare(u, v));
		u = u.alignedCopy(false);
		v = v.alignedCopy(false);
		assertEquals(cn, u.compareTo(v));
		assertEquals(cn, BitVector.sNumericComparator.compare(u, v));
		assertEquals(cl, BitVector.sLexicalComparator.compare(u, v));

	}

	public void testReadAndWrite() throws Exception {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testReadAndWrite(vs[j]);
			}
		}
	}

	private void testReadAndWrite(BitVector v) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		v.writeTo(new OutputWriteStream(out));
		byte[] bytes = out.toByteArray();
		assertTrue(Arrays.equals(v.toByteArray(), bytes));

		BitVector w = new BitVector(v.size());
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		w.readFrom(new InputReadStream(in));

		assertEquals(v, w);

		out = new ByteArrayOutputStream();
		OutputStreamBitWriter writer = new OutputStreamBitWriter(out);
		v.writeTo(writer);
		writer.flush();
		bytes = out.toByteArray();

		w = new BitVector(v.size());
		in = new ByteArrayInputStream(bytes);
		InputStreamBitReader reader = new InputStreamBitReader(in);
		w.readFrom(reader);

		assertEquals(v, w);
	}
	
	public void testListIterator() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testListIterator(vs[j]);
			}
		}
	}

	private void testListIterator(BitVector v) {
		int size = v.size();

		final BitVector w = new BitVector(size);
		ListIterator<Boolean> i = v.listIterator();
		while (i.hasNext()) {
			w.setBit(i.nextIndex(), i.next());
		}
		assertEquals(v, w);

		final BitVector x = new BitVector(size);
		i = v.listIterator(size);
		while (i.hasPrevious()) {
			x.setBit(i.previousIndex(), i.previous());
		}
		assertEquals(v, x);

		final int a = random.nextInt(size + 1);
		i = v.listIterator(a);
		if (a == size) {
			assertEquals(-1, i.nextIndex());
		} else {
			assertEquals(a, i.nextIndex());
			assertEquals(v.getBit(a), i.next().booleanValue());
		}

		i = v.listIterator(a);
		if (a == 0) {
			assertEquals(-1, i.previousIndex());
		} else {
			assertEquals(a - 1, i.previousIndex());
			assertEquals(v.getBit(a - 1), i.previous().booleanValue());
		}
	}

	public void testFirstInRange() {
		for (int i = 0; i < 1000; i++) {
			BitVector v = new BitVector(1000);
			int vSize = v.size();
			int a = random.nextInt(vSize+1);
			int b = a + random.nextInt(vSize+1-a);
			BitVector w = v.range(a, b);
			int c;
			int wSize = w.size();
			if (wSize == 0) {
				c = -1;
			} else {
				c = random.nextInt(wSize);
				w.setBit(c, true);
			}

			if (c >= 0) {
				assertEquals(c, w.ones().first());
				assertEquals(c, w.ones().last());

				assertEquals(c, w.ones().next(c));
				assertEquals(-1, w.ones().previous(c));
				if (c > 0) assertEquals(c, w.ones().next(c-1));
				if (c < wSize) assertEquals(c, w.ones().previous(c+1));
				assertEquals(c, w.ones().next(0));
				assertEquals(c, w.ones().previous(wSize));
			} else {
				assertEquals(0, w.ones().first());
				assertEquals(-1, w.ones().last());
			}
			w.flip();
			if (c >= 0) {
				assertEquals(c, w.zeros().first());
				assertEquals(c, w.zeros().last());

				assertEquals(c, w.zeros().next(c));
				assertEquals(-1, w.zeros().previous(c));
				if (c > 0) assertEquals(c, w.zeros().next(c-1));
				if (c < wSize) assertEquals(c, w.zeros().previous(c+1));
				assertEquals(c, w.zeros().next(0));
				assertEquals(c, w.zeros().previous(wSize));
			} else {
				assertEquals(0, w.zeros().first());
				assertEquals(-1, w.zeros().last());
			}
		}
	}

	public void testFromBigInteger() {
		for (int i = 0; i < 1000; i++) {
			final int size = random.nextInt(1000);
			final BigInteger bigInt = new BigInteger(size, random);
			BitVector v = BitVector.fromBigInteger(bigInt);
			assertTrue(v.size() <= size);
			assertEquals(bigInt, v.toBigInteger());

			BitVector w = BitVector.fromBigInteger(bigInt, v.size() / 2);
			assertEquals(v.range(0, w.size()), w);

			BitVector x = BitVector.fromBigInteger(bigInt, size * 2);
			assertEquals(v, x.range(0, v.size()));

			if (bigInt.signum() != 0)
			try {
				BitVector.fromBigInteger(bigInt.negate());
				fail();
			} catch (IllegalArgumentException e) {
				/* expected */
			}
		}
	}

	public void testFromBitSet() {
		for (int i = 0; i < 1000; i++) {
			final int size = random.nextInt(1000);
			BitSet bitSet = new BitSet(size);
			for (int j = 0; j < size; j++) {
				bitSet.set(j, random.nextBoolean());
			}
			BitVector v = BitVector.fromBitSet(bitSet);
			assertTrue(v.size() <= size);
			assertEquals(bitSet, v.toBitSet());

			BitVector w = BitVector.fromBitSet(bitSet, v.size() / 2);
			assertEquals(v.range(0, w.size()), w);

			BitVector x = BitVector.fromBitSet(bitSet, size * 2);
			assertEquals(v, x.range(0, v.size()));
		}
	}

	public void testStringConstructor() {
		assertEquals(new BitVector("10", 10), new BitVector("1010"));

		for (int i = 0; i < 1000; i++) {
			BitVector v = randomVector();
			int r = random.nextInt(14) + 2;
			String str = v.toString(r);
			BitVector w = new BitVector(str, r);
			assertEquals(str, w.toString(r));
		}
	}

	public void testNextOne() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testNextOne(vs[j]);
			}
		}
	}

	private void testNextOne(BitVector v) {
		int count = 0;
		for (int i = v.ones().first(); i < v.size(); i = v.ones().next(i+1)) {
			count++;
		}
		assertEquals(v.ones().count(), count);
	}

	public void testFromByteArray() {
		for (int i = 0; i < 1000; i++) {
			testFromByteArray(randomVector());
		}
	}

	private void testFromByteArray(BitVector v) {
		byte[] array = v.toByteArray();
		BitVector w = BitVector.fromByteArray(array, v.size());
		assertEquals(v, w);
	}

	public void testSetBytes() {
		for (int i = 0; i < 1000; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testSetBytes(vs[j]);
			}
		}
	}

	private void testSetBytes(BitVector v) {
		if (v.size() < 8) return;
		BitVector r = randomVector(random.nextInt((v.size())/8*8));
		byte[] bytes = r.toByteArray();
		int position = random.nextInt( v.size() - r.size() + 1 );
		int length = random.nextInt(r.size() + 1);
		int offset = random.nextInt(r.size() - length + 1);
		v.set().withBytes(position, bytes, offset, length);
		assertEquals(r.range(offset, offset + length), v.range(position, position + length));
	}

	public void testBitIterator() {
		BitVector v = new BitVector("0100");
		ListIterator<Boolean> i = v.range(1, 3).listIterator();
		assertFalse(i.hasPrevious());
		assertFalse(i.next());
		assertTrue(i.next());
		assertFalse(i.hasNext());
		assertTrue(i.previous());
		assertFalse(i.previous());
		assertFalse(i.hasPrevious());
		i.next();
		i.set(true);
		assertEquals(new BitVector("0110"), v);

		i = v.immutableView().listIterator();
		try {
			i.next();
			i.set(true);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testAsSet() throws Exception {
		BitVector v = new BitVector(30);
		SortedSet<Integer> set = v.ones().asSet();
		set = set.tailSet(10);
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());
		set.remove(null);
		set.remove(1);
		set.retainAll(Collections.singleton("STR"));
		set.add(10);
		assertEquals(new BitVector("000000000000000000010000000000"), v);
		assertEquals(1, set.size());
		assertEquals(set, Collections.singleton(10));
		set.retainAll(Collections.singleton(10));
		assertFalse(set.isEmpty());
		assertTrue(set.iterator().hasNext());
		assertEquals(10, (int) set.iterator().next());
		set.addAll(Arrays.asList(15, 18));
		assertEquals(3, set.size());
		assertEquals(10, (int) set.first());
		assertEquals(18, (int) set.last());
		assertEquals(1, set.headSet(15).size());
		assertEquals(Collections.singleton(10), set.headSet(15));
		assertEquals(new HashSet<Integer>(Arrays.asList(15, 18)), set.tailSet(15));
		assertEquals(Collections.singleton(15), set.subSet(13, 17));

		try {
			v.immutableView().ones().asSet().add(1);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testGetBits() {
		for (int i = 0; i < 65; i++) {
			assertEquals(0L, new BitVector(65).range(i, 65).asNumber().longValue());
		}
	}

}

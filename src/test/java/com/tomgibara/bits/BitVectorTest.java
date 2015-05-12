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
import java.util.Random;
import java.util.SortedSet;

import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.InputStreamBitReader;
import com.tomgibara.bits.OutputStreamBitWriter;
import com.tomgibara.bits.BitVector.Operation;
import com.tomgibara.bits.BitVector.Test;

import junit.framework.TestCase;

public class BitVectorTest extends TestCase {

	private static final Random random = new Random(0);
	
	private static BitVector[] randomVectorFamily(int length, int size) {
		BitVector v = randomVector(length);
		BitVector[] vs = new BitVector[size + 1];
		vs[0] = v;
		for (int i = 0; i < size; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			vs[i+1] = v.rangeView(a, b);
		}
		return vs;
	}
	
	private static BitVector[] randomVectorFamily(int size) {
		return randomVectorFamily(random.nextInt(1000), size);
	}
	
	private static BitVector randomVector(int length) {
		//TODO optimize when factory methods are available
		BitVector vector = new BitVector(length);
		for (int i = 0; i < length; i++) {
			vector.setBit(i, random.nextBoolean());
		}
		return vector;
	}
	
	private static BitVector randomVector() {
		return randomVector(random.nextInt(1000));
	}
	
	public void testEqualityAndHash() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testEqualityAndHash(vs[j]);
			}
		}
	}
	
	private void testEqualityAndHash(BitVector v) {
		assertEquals(v, v);
		int size = v.size();
		BitVector w = new BitVector(size+1);
		w.setVector(0, v);
		assertFalse(w.equals(v));
		assertFalse(v.equals(w));
		BitVector x = new BitVector(size);
		for (int i = 0; i < size; i++) x.setBit(i, v.getBit(i));
		assertEquals(v, x);
		assertEquals(x, v);
		assertEquals(v.hashCode(), x.hashCode());
		
		for (int i = 0; i < size; i++) {
			x.flipBit(i);
			assertFalse(v.equals(x));
			assertFalse(x.equals(v));
			x.flipBit(i);
		}
		
		BitVector y = v.mutable();
		BitVector z = v.immutable();
		assertEquals(y.hashCode(), z.hashCode());
		assertEquals(y, z);
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

	public void testSetBit() throws Exception {
		BitVector v = new BitVector(100);
		for (int i = 0; i < 100; i++) {
			v.setBit(i, true);
			for (int j = 0; j < 100; j++) {
				assertEquals("Mismatch at " + j + " during " + i, j == i, v.getBit(j));
			}
			v.setBit(i, false);
		}
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

	public void testNumberMethods() {
		
		//check short vector
		BitVector v = new BitVector(1);
		testNumberMethods(v, 0);
		v.set(true);
		testNumberMethods(v, 1);
		
		//check long vector
		v = new BitVector(128);
		testNumberMethods(v, 0);
		v.set(true);
		testNumberMethods(v, -1);
		
		//check view vectors
		BitVector w = v.rangeView(64, 128);
		testNumberMethods(w, -1);
		w = v.rangeView(63, 128);
		testNumberMethods(w, -1);
		
		//check empty vector
		v = new BitVector(0);
		testNumberMethods(v, 0);
		
	}

	private void testNumberMethods(BitVector v, long value) {
		assertEquals((byte) value, v.byteValue());
		assertEquals((short) value, v.shortValue());
		assertEquals((int) value, v.intValue());
		assertEquals(value, v.longValue());
	}

	public void testToBigInteger() {
		BitVector v = new BitVector(1024);
		v.set(true);
		int f = 512;
		int t = 512;
		BigInteger i = BigInteger.ONE;
		while (f > 0 && t < 1024) {
			//evens
			BitVector e = v.rangeView(f, t);
			assertEquals(i.subtract(BigInteger.ONE), e.toBigInteger());
			i = i.shiftLeft(1);
			f--;
			//odds
			BitVector o = v.rangeView(f, t);
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
		assertEquals(v.size(), v.countOnes() + v.countZeros());
		assertEquals(totalOneCount, v.countOnes());
		assertEquals(totalOneCount, v.countOnes(0, v.size()));
		assertEquals(totalZeroCount, v.countZeros());
		assertEquals(totalZeroCount, v.countZeros(0, v.size()));
		int reps = v.size();
		for (int i = 0; i < reps; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			String s = str.substring(str.length()-b, str.length()-a);
			int oneCount = s.replace("0", "").length();
			int zeroCount = s.replace("1", "").length();
			assertEquals(oneCount, v.countOnes(a, b));
			assertEquals(zeroCount, v.countZeros(a, b));
		}
	}
	
	public void testSetGetBit() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testGetSetBit(vs[j]);
			}
		}
	}

	private void testGetSetBit(BitVector v) {
		if (v.size() == 0) return;
		BitVector c = v.copy();
		int i = random.nextInt(v.size());
		v.setBit(i, !v.getBit(i));
		c.xorVector(v);
		assertTrue(c.getBit(i));
		assertEquals(1, c.countOnes());
	}
	
	public void testOverlapping() {
		BitVector v = new BitVector("1010101010101010");
		BitVector w = v.rangeView(0, 15);
		v.xorVector(1, w);
		assertEquals(new BitVector("1111111111111110"), v);
		
		v = new BitVector("1010101010101010");
		w = v.rangeView(1, 16);
		v.xorVector(0, w);
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
		cl.xor(true);
		cp.xorVector(vw);
		assertEquals(cp.size(), cp.countOnes());
		
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
		assertEquals(v.rangeView(0, w.size()), w);
		
		w = v.resizedCopy(size);
		assertEquals(v, w);
		
		a = size == 0 ? 1 : size + random.nextInt(size);
		w = v.resizedCopy(a);
		assertEquals(v, w.rangeView(0, size));
		w.isAllZerosRange(size, w.size());
	}

	public void testMutability() {
		BitVector v = new BitVector(1).immutable();
		try {
			v.modify(BitVector.Operation.SET, true);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
		try {
			v.modifyRange(BitVector.Operation.SET, 0, 1, true);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
		try {
			v.modifyBit(BitVector.Operation.SET, 0, true);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
		try {
			v.modifyBits(BitVector.Operation.SET, 0, 1L, 1);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
		try {
			v.modifyVector(BitVector.Operation.SET, v);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
		try {
			v.modifyVector(BitVector.Operation.SET, 0, v);
			fail();
		} catch (IllegalStateException e) {
			//expected
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
		w2.set(true);
		assertEquals(1000, v2.countOnes());
		assertFalse(x2.equals(v2));
		x2.set(true);
		assertEquals(1000, y2.countOnes());
		
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
		v.set(false);
		assertTrue(v.isAllZeros());
		assertFalse(v.size() != 0 && v.isAllOnes());
		v.set(true);
		assertTrue(v.isAllOnes());
		assertFalse(v.size() != 0 && v.isAllZeros());
		int reps = v.size();
		for (int i = 0; i < reps; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			v.setRange(a, b, false);
			assertTrue(v.isAllZerosRange(a, b));
			v.setRange(a, b, true);
			assertTrue(v.isAllOnesRange(a, b));
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
		assertTrue(v.testEquals(v));
		assertTrue(v.test(Test.EQUALS, v));
		assertTrue(v.testContains(v));
		assertTrue(v.test(Test.CONTAINS, v));
		if (!v.isAllZeros()) {
			assertTrue(v.testIntersects(v));
			assertTrue(v.test(Test.INTERSECTS, v));
		}
		
		BitVector w = v.alignedCopy(true);
		assertTrue(v.testEquals(w));
		assertTrue(v.test(Test.EQUALS, w));
		assertTrue(w.testEquals(v));
		assertTrue(w.test(Test.EQUALS, v));
		assertTrue(v.testContains(w));
		assertTrue(v.test(Test.CONTAINS, w));
		assertTrue(w.testContains(v));
		assertTrue(w.test(Test.CONTAINS, v));
		if (!v.isAllZeros()) {
			assertTrue(v.testIntersects(w));
			assertTrue(v.test(Test.INTERSECTS, w));
			assertTrue(w.testIntersects(v));
			assertTrue(w.test(Test.INTERSECTS, v));
		}
		
		w = v.alignedCopy(true);
		for (int i = 0; i < size; i++) {
			w.setBit(i, true);
			assertTrue( w.testContains(v) );
			assertTrue( w.test(Test.CONTAINS, v) );
			assertTrue( v.testEquals(w) || !v.testContains(w) );
			assertTrue( v.test(Test.EQUALS, w) || !v.test(Test.CONTAINS, w) );
		}
		
		w = v.alignedCopy(true);
		for (int i = 0; i < size; i++) {
			w.setBit(i, false);
			assertTrue( v.testContains(w) );
			assertTrue( v.test(Test.CONTAINS, w) );
			assertTrue( w.testEquals(v) || !w.testContains(v) );
			assertTrue( w.test(Test.EQUALS, v) || !w.test(Test.CONTAINS, v) );
		}
		
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
		v.write(out);
		byte[] bytes = out.toByteArray();
		assertTrue(Arrays.equals(v.toByteArray(), bytes));
		
		BitVector w = new BitVector(v.size());
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		w.readFrom(in);
		
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
	
	public void testRotation() {
		BitVector v = new BitVector(32);
		v.setBit(0, true);
		for (int i = 0; i < 32; i++) {
			assertEquals(1 << i, v.intValue());
			v.rotate(1);
		}
		
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testRotation(vs[j]);
			}
		}
	}

	private void testRotation(BitVector v) {
		BitVector w = v.copy();
		int d = random.nextInt();
		for (int i = 0; i < v.size(); i++) v.rotate(d);
		assertEquals(w, v);
	}

	public void testShift() {
		BitVector v = new BitVector(32);
		v.setBit(0, true);
		for (int i = 0; i < 32; i++) {
			assertEquals(1 << i, v.intValue());
			v.shift(1, false);
		}
		
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testShift(vs[j]);
			}
		}
	}

	private void testShift(BitVector v) {
		int size = v.size();
		int scope = size == 0 ? 4 : size * 3;
		int d = random.nextInt(scope) - scope/2;
		BitVector w = v.copy();
		v.shift(d, true);
		if (d > 0) {
			if (d >= size) {
				assertTrue(v.isAllOnes());
			} else {
				assertTrue( v.isAllOnesRange(0, d) );
				assertTrue( v.rangeView(d, size).testEquals(w.rangeView(0, size - d)) );
			}
		} else {
			if (d <= -size) {
				assertTrue(v.isAllOnes());
			} else {
				assertTrue( v.isAllOnesRange(size + d, size));
				assertTrue( v.rangeView(0, size + d).testEquals(w.rangeView(-d, size)));
			}
		}
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

	public void testReverse() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testReverse(vs[j]);
			}
		}
	}

	private void testReverse(BitVector v) {
		BitVector w = v.copy();
		w.reverse();
		ListIterator<Boolean> i = v.listIterator();
		ListIterator<Boolean> j = w.listIterator(v.size());
		while (i.hasNext()) {
			assertEquals(i.next(), j.previous());
		}
		w.reverse();
		assertEquals(v, w);
	}

	public void testShuffle() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testShuffle(vs[j]);
			}
		}
	}

	private void testShuffle(BitVector v) {
		int size = v.size();
		for (int i = 0; i < 10; i++) {
			BitVector w = v.copy();
			int from;
			int to;
			if (i == 0) {
				from = 0;
				to = size;
				w.shuffle(random);
			} else {
				from = random.nextInt(size + 1);
				to = from + random.nextInt(size + 1 - from);
				w.shuffleRange(from, to, random);
			}
			assertEquals(v.rangeView(from, to).countOnes(), w.rangeView(from, to).countOnes());
		}
	}

	public void testShuffleIsFair() {
		{
			BitVector v = new BitVector(256);
			v.setRange(0, 16, true);
			testShuffleIsFair(v);
		}

		{
			BitVector v = new BitVector(100);
			v.setRange(0, 50, true);
			testShuffleIsFair(v);
		}
		
		{
			BitVector v = new BitVector(97);
			v.setRange(0, 13, true);
			testShuffleIsFair(v);
		}
	}

	private void testShuffleIsFair(BitVector v) {
		Random random = new Random();
		int[] freqs = new int[v.size()];
		int trials = 10000;
		for (int i = 0; i < trials; i++) {
			BitVector w = v.clone();
			w.shuffle(random);
			for (Integer index : w.asSet()) {
				freqs[index]++;
			}
		}
		float e = (float) v.countOnes() / v.size() * trials;
		double rms = 0f;
		for (int i = 0; i < freqs.length; i++) {
			float d = freqs[i] - e;
			rms += d * d;
		}
		rms = Math.sqrt(rms / trials);
		assertTrue(rms / e < 0.01);
	}

	public void testFirstInRange() {
		for (int i = 0; i < 1000; i++) {
			BitVector v = new BitVector(1000);
			int vSize = v.size();
			int a = random.nextInt(vSize+1);
			int b = a + random.nextInt(vSize+1-a);
			BitVector w = v.rangeView(a, b);
			int c;
			int wSize = w.size();
			if (wSize == 0) {
				c = -1;
			} else {
				c = random.nextInt(wSize);
				w.setBit(c, true);
			}

			if (c >= 0) {
				assertEquals(c, w.firstOne());
				assertEquals(c, w.lastOne());

				assertEquals(c, w.nextOne(c));
				assertEquals(-1, w.previousOne(c));
				if (c > 0) assertEquals(c, w.nextOne(c-1));
				if (c < wSize) assertEquals(c, w.previousOne(c+1));
				assertEquals(c, w.nextOne(0));
				assertEquals(c, w.previousOne(wSize));
			} else {
				assertEquals(0, w.firstOne());
				assertEquals(-1, w.lastOne());
			}
			w.flip();
			if (c >= 0) {
				assertEquals(c, w.firstZero());
				assertEquals(c, w.lastZero());

				assertEquals(c, w.nextZero(c));
				assertEquals(-1, w.previousZero(c));
				if (c > 0) assertEquals(c, w.nextZero(c-1));
				if (c < wSize) assertEquals(c, w.previousZero(c+1));
				assertEquals(c, w.nextZero(0));
				assertEquals(c, w.previousZero(wSize));
			} else {
				assertEquals(0, w.firstZero());
				assertEquals(-1, w.lastZero());
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
			assertEquals(v.rangeView(0, w.size()), w);
			
			BitVector x = BitVector.fromBigInteger(bigInt, size * 2);
			assertEquals(v, x.rangeView(0, v.size()));
			
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
			assertEquals(v.rangeView(0, w.size()), w);
			
			BitVector x = BitVector.fromBitSet(bitSet, size * 2);
			assertEquals(v, x.rangeView(0, v.size()));
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
		for (int i = v.firstOne(); i < v.size(); i = v.nextOne(i+1)) {
			count++;
		}
		assertEquals(v.countOnes(), count);
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
		v.setBytes(position, bytes, offset, length);
		assertEquals(r.rangeView(offset, offset + length), v.rangeView(position, position + length));
	}
	
	public void testGetAndModifyBit() {
		for (int i = 0; i < 1000; i++) {
			BitVector v = randomVector(100);
			testGetAndModifyBit(BitVector.Operation.AND, v);
		}
	}

	private void testGetAndModifyBit(Operation op, BitVector v3) {
		for (int i = 0; i < 100; i++) {
			int p = random.nextInt(v3.size());
			boolean v = random.nextBoolean();

			BitVector v1 = v3.copy();
			BitVector v2 = v3.copy();

			// expected result
			boolean b3 = v3.getBit(p);
			v3.modifyBit(op, p, v);

			// result using general method
			boolean b1 = v1.getThenModifyBit(op, p, v);
			
			// result using specific method
			boolean b2;
			switch (op) {
			case AND:
				b2 = v2.getThenAndBit(p, v);
				break;
			case OR:
				b2 = v2.getThenOrBit(p, v);
				break;
			case SET:
				b2 = v2.getThenSetBit(p, v);
				break;
			case XOR:
				b2 = v2.getThenXorBit(p, v);
				break;
				default : throw new IllegalStateException();
			}
			
			assertEquals(v3, v1);
			assertEquals(v3, v2);
			assertEquals(b3, b1);
			assertEquals(b3, b2);
		}
	}

	public void testBitIterator() {
		BitVector v = new BitVector("0100");
		ListIterator<Boolean> i = v.rangeView(1, 3).listIterator();
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
	
	public void testPositionIterator() {
		
		BitVector v = new BitVector("010010010010");
		v = v.rangeView(3, 9);
		ListIterator<Integer> it = v.positionIterator();
		assertTrue(it.hasNext());
		assertEquals(1, (int) it.next());
		it.add(2);
		assertTrue(it.hasPrevious());
		assertEquals(2, it.nextIndex());
		assertEquals(1, it.previousIndex());
		assertEquals(2, (int) it.previous());
		assertEquals(2, (int) it.next());
		assertEquals(4, (int) it.next());
		assertFalse(it.hasNext());
		it.remove();
		assertEquals(2, (int) it.previous());
		assertEquals(1, (int) it.previous());
		assertEquals(-1, it.previousIndex());
		it.remove();
		assertEquals(2, (int) it.next());
		assertFalse(it.hasNext());
		it.set(4);
		assertEquals(4, (int) it.previous());
		
		it = v.immutable().positionIterator();
		try {
			it.next();
			it.set(0);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}
	
	public void testAsList() {
		BitVector v = new BitVector(20);
		List<Boolean> list = v.rangeView(5, 15).asList();
		assertEquals(10, list.size());
		for (int i = 0; i < list.size(); i++) {
			assertTrue(list.set(i, true));
		}
		assertEquals(new BitVector("00000111111111100000"), v);
		for (int i = 0; i < list.size(); i++) {
			assertFalse(list.set(i, true));
		}
		{
			int i = 0;
			for (Boolean b : list) {
				assertEquals(v.getBit(5 + i++), (boolean) b);
			}
		}
		
		for (ListIterator<Boolean> i = list.listIterator(); i.hasNext();) {
			i.next();
			i.set(false);
		}
		assertEquals(new BitVector(20), v);

		list = v.immutableRangeView(5, 15).asList();
		try {
			list.set(0, true);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testAsSet() throws Exception {
		BitVector v = new BitVector(30);
		SortedSet<Integer> set = v.asSet();
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
			v.immutableView().asSet().add(1);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testBitVectorRead() {
		
	}

}

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
import java.util.ListIterator;

import com.tomgibara.streams.Streams;

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
		return new BitVector(random, length);
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

	public void testGet() throws Exception {
		//72 long
		BitVector v = new BitVector("100101111011011100001101011001100000101110001011100001110011101101100010");
		assertEquals((byte)new BigInteger("10010111", 2).intValue(), v.getByte(64));
	}

	public void testToArrays() {
		for (int i = 0; i < 10; i++) {
			BitVector[] vs = randomVectorFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testToIntArray(vs[j]);
				testToLongArray(vs[j]);
			}
		}
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

		BitVector cp = v.mutableCopy();
		assertEquals(v, cp);
		assertNotSame(v, cp);

		BitVector vw = v.clone();
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

		BitVector w = v.resizedCopy(a, false);
		assertEquals(a, w.size());
		assertEquals(v.rangeTo(a), w);
		
		w = v.resizedCopy(a, true);
		assertEquals(a, w.size());
		assertEquals(v.rangeFrom(size - a), w);

		w = v.resizedCopy(size, false);
		assertEquals(v, w);

		w = v.resizedCopy(size, true);
		assertEquals(v, w);

		a = size == 0 ? 1 : size + random.nextInt(size);

		w = v.resizedCopy(a, false);
		assertEquals(a, w.size());
		assertEquals(v, w.rangeTo(size));
		assertTrue( w.rangeFrom(size).zeros().isAll() );

		w = v.resizedCopy(a, true);
		assertEquals(a, w.size());
		assertEquals(v, w.rangeFrom(a - size));
		assertTrue( w.rangeTo(a - size).zeros().isAll() );
	}

	public void testMutability() {
		BitVector v = new BitVector(1).immutable();
		try {
			v.duplicate(false, true);
			fail();
		} catch (IllegalStateException e) {
			//expected
		}
	}

	public void testSerialization() throws Exception {
		BitVector v1 = randomVector(1000);
		BitVector w1 = v1.clone();
		BitVector x1 = v1.mutableCopy();
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
		w2.fillWithOnes();
		assertEquals(1000, v2.ones().count());
		assertFalse(x2.equals(v2));
		x2.fillWithOnes();
		assertEquals(1000, y2.ones().count());

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
		BitVector cm = v.alignedCopy();
		BitVector ci = v.immutableView().alignedCopy();
		assertTrue(cm.isAligned());
		assertTrue(cm.isMutable());
		assertTrue(ci.isAligned());
		assertFalse(ci.isMutable());
		assertEquals(v, cm);
		assertEquals(v, ci);
		assertNotSame(v, cm);
		assertNotSame(v, ci);
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
		v.writeTo(Streams.streamOutput(out));
		byte[] bytes = out.toByteArray();
		assertTrue(Arrays.equals(v.toByteArray(), bytes));

		BitVector w = new BitVector(v.size());
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		w.readFrom(Streams.streamInput(in));

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

	public void testBitIterator() {
		BitVector v = new BitVector("0100");
		ListIterator<Boolean> i = v.range(1, 3).asList().listIterator();
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

		i = v.immutableView().asList().listIterator();
		try {
			i.next();
			i.set(true);
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

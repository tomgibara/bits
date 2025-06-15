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

import com.tomgibara.bits.BitStore.DisjointMatches;
import com.tomgibara.bits.BitStore.Matches;
import com.tomgibara.bits.BitStore.Op;
import com.tomgibara.bits.BitStore.OverlappingMatches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.fundament.Alignable;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class BitStoreTest {

	static final Random random = new Random(0);

	// abstracted generation

	int validSize(int suggested) {
		return suggested;
	}

	boolean isValidSize(int size) {
		return size == validSize(size);
	}

	abstract BitStore newStore(int size);

	BitStore newStore(BitStore s) {
		BitStore store = newStore(s.size());
		store.set().withStore(s);
		return store;
	}

	BitStore randomStore(int size) {
		BitStore store = newStore(size);
		for (int i = 0; i < size; i++) {
			store.setBit(i, random.nextBoolean());
		}
		return store;
	}

	BitStore[] randomStoreFamily(int length, int size) {
		BitStore v = randomStore(validSize(length));
		BitStore[] vs = new BitStore[size + 1];
		vs[0] = v;
		for (int i = 0; i < size; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			vs[i+1] = v.range(a, b);
		}
		return vs;
	}

	BitStore[] randomStoreFamily(int size) {
		return randomStoreFamily(random.nextInt(1000), size);
	}

	@Test
	public void testSetBit() throws Exception {
		int size = validSize(100);
		BitStore v = newStore(size);
		for (int i = 0; i < size; i++) {
			v.setBit(i, true);
			for (int j = 0; j < size; j++) {
				assertEquals(j == i, v.getBit(j), "Mismatch at " + j + " during " + i);
			}
			v.setBit(i, false);
		}
	}

	@Test
	public void testEqualityAndHash() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testEqualityAndHash(vs[j]);
			}
		}
	}

	private void testEqualityAndHash(BitStore v) {
		assertEquals(v, v);
		int size = v.size();
		BitVector w = new BitVector(size+1);
		w.setStore(0, v);
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

		BitStore y = v.mutable();
		BitStore z = v.immutable();
		assertEquals(y.hashCode(), z.hashCode(),y.getClass() + " " + z.getClass());
		assertEquals(y, z);
	}

	@Test
	public void testStoreSetGetBit() {
		int size = validSize(100);
		BitStore s = randomStore(size);
		for (int j = 0; j < size; j++) {
			testStoreGetSetBit(s);
		}
	}

	private void testStoreGetSetBit(BitStore s) {
		if (s.size() == 0) return;
		BitVector c = BitVector.fromStore(s);
		int i = random.nextInt(s.size());
		s.setBit(i, !s.getBit(i));
		c.xor().withStore(s);
		assertTrue(c.getBit(i));
		assertEquals(1, c.ones().count());
	}

	@Test
	public void testStoreSet() {
		int size = validSize(100);
		BitStore s = newStore(size);
		BitStore t = newStore(size);
		s.set().with(true);
		t.fill();
		assertEquals(t, s);
		for (int i = 0; i < size; i++) {
			s.set().withBit(i, false);
			t.setBit(i, false);
			assertEquals(t, s);
		}
		assertTrue(s.zeros().isAll());
		BitStore h = newStore(size / 2);
		h.fill();
		s.set().withStore(size/4, h);
		assertEquals(h.size(), s.ones().count(), s.toString());
	}

	@Test
	public void testStoreOr() {
		int size = validSize(100);
		BitStore s = newStore(size);
		for (int i = 1; i < size; i += 2) {
			s.or().withBit(i, true);
		}
		assertEquals(size / 2, s.ones().count());
		BitStore t = s.immutableCopy();
		for (int i = 0; i < size; i++) {
			s.or().withBit(i, false);
		}
		assertEquals(t, s);
		if (size > 1) {
			t = t.range(1, t.size());
			s.or().withStore(0, t);
			assertTrue(s.ones().isAll(), s.toString());
		}
	}

	@Test
	public void testStoreMutability() {
		int size = validSize(1);
		BitStore s = newStore(size);
		BitStore t = s.mutableCopy();
		BitStore u = s.immutableView();
		assertTrue(s.isMutable());
		assertTrue(t.isMutable());
		assertFalse(u.isMutable());

		assertTrue(s.equals().store(t));
		assertTrue(s.equals().store(u));
		s.flip();
		assertFalse(s.equals().store(t));
		assertTrue(s.equals().store(u));
		assertTrue(s.isMutable());

		try {
			u.flip();
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}

		Positions sp = s.ones().positions();
		Positions up = u.ones().positions();
		sp.next();
		sp.set(0);
		try {
			up.next();
			up.set(0);
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}

		try {
			u.permute().reverse();
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}

		Positions ps = u.match(true).positions();
		ps.next();
		try {
			ps.replace(Bits.zeroBit());
		} catch (IllegalStateException e) {
			/* expected */
		}
	}

	@Test
	public void testStoreReadWrite() {
		for (int i = 0; i < 100; i++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int length = (size + 7) / 8;
			{
				byte[] bytes = new byte[length];
				ByteArrayBitWriter writer = new ByteArrayBitWriter(bytes);
				s.writeTo(writer);
				writer.flush();
				ByteArrayBitReader reader = new ByteArrayBitReader(bytes);
				BitStore t = newStore(size);
				t.readFrom(reader);
				assertTrue(s.equals().store(t), "\n" + s + "\n" + t);
			}
			{
				StreamBytes sb = Streams.bytes(length);
				try (WriteStream writer = sb.writeStream()) {
					s.writeTo(writer);
				}
				assertArrayEquals(s.toByteArray(), sb.directBytes());
				ReadStream reader = sb.readStream();
				BitStore t = newStore(size);
				t.readFrom(reader);
				if (!s.equals().store(t)) {
					System.out.println(s.getClass());
					System.out.println(BitVector.fromStore(s));
					System.out.println(t.getClass());
					System.out.println(BitVector.fromStore(t));
				}
				assertTrue(s.equals().store(t), "%s%n%s".formatted(s, t));
			}
		}
	}

	@Test
	public void testStoreTests() {
		for (int test = 0; test < 500; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = s.mutableCopy();
			BitStore b = newStore(size);
			assertTrue(s.excludes().store(b));
			assertTrue(b.excludes().store(s));
			int reps = (size - s.ones().count()) / 2;
			for (int i = 0; i < reps; i++) {
				int j;
                do {
                    j = random.nextInt(size);
                } while (s.getBit(j));
				t.setBit(j, true);
				assertFalse(s.equals().store(t));
				assertFalse(t.equals().store(s));
				assertTrue(t.contains().store(s));
				assertFalse(s.contains().store(t));
				assertEquals(s.zeros().isAll(), s.excludes().store(t), "\n" + s + "\n" + t);
				assertEquals(s.zeros().isAll(), t.excludes().store(s));
				s.setBit(j, true);
			}
		}
	}

	@Test
	public void testStoreClear() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = s.mutableCopy();
			s.fill();
			t.clear();
			assertTrue(s.ones().isAll());
			assertTrue(t.zeros().isAll());
			assertTrue(t.ones().isNone());
			assertTrue(s.zeros().isNone());
		}
	}

	@Test
	public void testStoreCount() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = canon(s);
			assertEquals(t.ones().count(), s.ones().count());
		}
	}

	@Test
	public void testStoreRange() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int from = random.nextInt(size + 1);
			int to = from + random.nextInt(size + 1 - from);
			BitStore r = s.range(from, to);
			BitStore t = canon(s).range(from, to);
			assertEquals(to - from, r.size());
			assertEquals(to - from, t.size());
			assertTrue(r.equals().store(t), r + " " + BitVector.fromStore(t));
		}
	}

	@Test
	public void testObjectMethods() {
		for (int test = 0; test < 1000; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int from = random.nextInt(size + 1);
			int to = from + random.nextInt(size + 1 - from);
			s = s.range(from, to);
			BitStore t = canon(s);
			String types = s.getClass() + " compared to " + t.getClass();
			assertEquals(t, s, types);
			assertEquals(s, t, types);
			assertEquals(t.hashCode(), s.hashCode(), types);
			assertEquals(s.toString(), t.toString(), types);
		}
	}

	@Test
	public void testStoreToByteArray() {
		for (int test = 0; test < 1000; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int from = random.nextInt(size + 1);
			int to = from + random.nextInt(size + 1 - from);
			s = s.range(from, to);
			BitStore t = canon(s);
			String types = s.getClass() + " compared to " + t.getClass();
			assertArrayEquals(t.toByteArray(), s.toByteArray(), types);
		}
	}

	@Test
	public void testNumberMethods() {

		//check short store
		BitStore v = newStore(validSize(1));
		testNumberMethods(v, 0);
		v.fill();
		testNumberMethods(v, 1);

		//check long store
		v = new BitVector(validSize(128));
		if (v.size() >= 64) {
			testNumberMethods(v, 0);
			v.fill();
			testNumberMethods(v, -1);
		}

		//check view store
		if (v.size() >= 128) {
			BitStore w = v.range(v.size() / 2, v.size());
			testNumberMethods(w, -1);
			w = v.range(v.size() / 2 - 1, v.size());
			testNumberMethods(w, -1);
		}

		//check empty store
		v = newStore(validSize(0));
		testNumberMethods(v, 0);

	}

	private void testNumberMethods(BitStore v, long value) {
		Number n = v.asNumber();
		assertEquals((byte) value, n.byteValue());
		assertEquals((short) value, n.shortValue());
		assertEquals((int) value, n.intValue());
		assertEquals(value, n.longValue());
	}

	@Test
	public void testGetAndModifyBit() {
		for (int i = 0; i < 1000; i++) {
			BitStore v = randomStore(validSize(100));
			testGetAndModifyBit(Operation.AND, v);
		}
	}

	private void testGetAndModifyBit(Operation op, BitStore v1) {
		for (int i = 0; i < 100; i++) {
			int p = random.nextInt(v1.size());
			boolean v = random.nextBoolean();

			BitStore v2 = v1.mutableCopy();

			// expected result
			boolean b1 = v1.getBit(p);
			v1.op(op).withBit(p, v);

			// result using op method
			boolean b2 = v2.op(op).getThenWithBit(p, v);

			assertEquals(v1, v2);
			assertEquals(b1, b2);
		}
	}

	@Test
	public void testPositionsRemove() {
		if (!isValidSize(5)) return;
		BitStore store = newStore(Bits.asStore("10101"));
		Positions ps = store.ones().positions();
		assertTrue(ps.hasNext());
		assertEquals(0, ps.nextPosition());
		assertTrue(ps.hasNext());
		assertEquals(2, ps.nextPosition());
		assertTrue(ps.hasNext());
		assertEquals(4, ps.nextPosition());
		ps.remove();
		assertFalse(ps.hasNext());
		assertEquals(Bits.asStore("00101"), store);
	}

	@Test
	public void testPositionIterator() {
		testPositionIterator(true);
		testPositionIterator(false);
	}

	private void testPositionIterator(boolean ones) {
		BitStore v = new BitVector("010010010010");
		if (v.size() != validSize(v.size())) return;
		v = newStore(v);
		if (!ones) v.flip();
		v = v.range(3, 9); // 010010
		Positions it = v.match(ones).positions();
		assertTrue(it.hasNext());
		assertEquals(1, (int) it.next());
		it.add(2); //011010
		assertTrue(it.hasPrevious());
		assertEquals(2, it.nextIndex());
		assertEquals(1, it.previousIndex());
		assertEquals(2, (int) it.previous());
		assertEquals(2, (int) it.next());
		assertEquals(4, (int) it.next());
		assertFalse(it.hasNext());
		assertEquals(it.nextPosition(), v.size());
		it.remove();
		assertEquals(2, (int) it.previous());
		assertEquals(1, (int) it.previous());
		assertEquals(-1, it.previousIndex());
		it.remove();
		assertEquals(2, (int) it.next());
		assertFalse(it.hasNext());
		it.set(4);
		assertEquals(4, (int) it.previous());

		it = v.immutable().ones().positions();
		try {
			it.next();
			it.set(0);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testRotation() {
		BitVector v = new BitVector(32);
		v.setBit(0, true);
		for (int i = 0; i < 32; i++) {
			assertEquals(1 << i, v.asNumber().intValue());
			v.permute().rotate(1);
		}

		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testRotation(vs[j]);
			}
		}
	}

	private void testRotation(BitStore v) {
		BitStore w = v.mutableCopy();
		int d = random.nextInt();
		for (int i = 0; i < v.size(); i++) v.permute().rotate(d);
		assertEquals(w, v);
	}

	@Test
	public void testShift() {
		if (validSize(32) != 32) return;
		BitStore v = newStore(32);
		v.setBit(0, true);
		for (int i = 0; i < 32; i++) {
			assertEquals(1 << i, v.asNumber().intValue());
			v.shift(1, false);
		}

		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testShift(vs[j]);
			}
		}
	}

	private void testShift(BitStore v) {
		int size = v.size();
		int scope = size == 0 ? 4 : size * 3;
		int d = random.nextInt(scope) - scope/2;
		BitStore w = v.mutableCopy();
		v.shift(d, true);
		if (d > 0) {
			if (d >= size) {
				assertTrue(v.ones().isAll());
			} else {
				assertTrue( v.range(0, d).ones().isAll() );
				assertTrue( v.range(d, size).equals().store(w.range(0, size - d)) );
			}
		} else {
			if (d <= -size) {
				assertTrue(v.ones().isAll());
			} else {
				assertTrue( v.range(size + d, size).ones().isAll());
				assertTrue( v.range(0, size + d).equals().store(w.range(-d, size)));
			}
		}
	}

	@Test
	public void testReverse() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testReverse(vs[j]);
			}
		}
	}

	private void testReverse(BitStore v) {
		BitStore w = v.mutableCopy();
		w.permute().reverse();
		ListIterator<Boolean> i = v.asList().listIterator();
		ListIterator<Boolean> j = w.asList().listIterator(v.size());
		while (i.hasNext()) {
			assertEquals(i.next(), j.previous());
		}
		w.permute().reverse();
		assertEquals(v, w);
	}

	@Test
	public void testShuffle() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testShuffle(v);
            }
		}
	}

	private void testShuffle(BitStore v) {
		int size = v.size();
		for (int i = 0; i < 10; i++) {
			BitStore w = v.mutableCopy();
			int from;
			int to;
			if (i == 0) {
				from = 0;
				to = size;
				w.permute().shuffle(random);
			} else {
				from = random.nextInt(size + 1);
				to = from + random.nextInt(size + 1 - from);
				w.range(from, to).permute().shuffle(random);
			}
			assertEquals(v.range(from, to).ones().count(), w.range(from, to).ones().count());
		}
	}

	@Test
	public void testShuffleIsFair() {
		{
			BitVector v = new BitVector(256);
			v.range(0, 16).fill();
			testShuffleIsFair(v);
		}

		{
			BitVector v = new BitVector(100);
			v.range(0, 50).fill();
			testShuffleIsFair(v);
		}

		{
			BitVector v = new BitVector(97);
			v.range(0, 13).fill();
			testShuffleIsFair(v);
		}
	}

	private void testShuffleIsFair(BitVector v) {
		Random random = new Random();
		int[] freqs = new int[v.size()];
		int trials = 10000;
		for (int i = 0; i < trials; i++) {
			BitVector w = v.clone();
			w.permute().shuffle(random);
			for (Integer index : w.ones().asSet()) {
				freqs[index]++;
			}
		}
		float e = (float) v.ones().count() / v.size() * trials;
		double rms = 0f;
		for (int i = 0; i < freqs.length; i++) {
			float d = freqs[i] - e;
			rms += d * d;
		}
		rms = Math.sqrt(rms / trials);
		assertTrue(rms / e < 0.01);
	}

	@Test
	public void testAsList() {
		if (validSize(20) != 20) return;
		BitStore v = newStore(20);
		List<Boolean> list = v.range(5, 15).asList();
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

		list = v.range(5, 15).immutable().asList();
		try {
			list.set(0, true);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testAsSet() throws Exception {
		if (validSize(30) != 30) return;
		BitStore v = newStore(30);
		SortedSet<Integer> set = v.ones().asSet();
		set = set.tailSet(10);
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());
		try {
			set.remove(null);
		} catch (NullPointerException e) {
			// TreeSet throws an NPE when attempting to remove null
		}
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
			v
			.immutableView()
			.ones()
			.asSet()
			.add(1);
			fail();
		} catch (IllegalStateException e) {
			// expected
		} catch (UnsupportedOperationException e) {
			// TODO would be nice to standardize this
			// but doing so means not using the tried and trusted unmodifiableSet().
		}

		SortedSet<Integer> zet = v.zeros().asSet();
		for (int i = 0; i < v.size(); i++) {
			assertTrue(zet.contains(i) ^ set.contains(i));
		}

		int count = 0;
		for (Integer i : zet) {
			assertFalse(set.contains(i));
			count ++;
		}

		assertEquals(v.size(), count + set.size());

		v.fill();
		assertTrue(zet.isEmpty());
		assertEquals(0, zet.size());

		v.clear();
		for (Iterator<?> i = zet.iterator(); i.hasNext(); ) {
			i.next();
			i.remove();
		}
		assertTrue(v.ones().isAll());
		assertTrue(zet.isEmpty());

		v.clear();
		for (int i = 0; i < 30; i++) {
			zet.remove(i);
		}
		assertTrue(v.ones().isAll());
		assertTrue(zet.isEmpty());
	}

	@Test
	public void testCompareTo() {
		BitStore[] vs = randomStoreFamily(500);
		for (int i = 1; i < vs.length; i++) {
			for (int j = 0; j < i; j++) {
				testCompareTo(vs[i], vs[j]);
			}
		}

	}

	private void testCompareTo(BitStore u, BitStore v) {
		int cn = Integer.signum(u.toBigInteger().compareTo(v.toBigInteger()));
		int cl = Integer.signum(u.toString().compareTo(v.toString()));
		String message = u + "\n" + v;
		assertEquals(cn, u.compareTo(v), message);
		assertEquals(cn, Bits.numericalComparator().compare(u, v), message);
		assertEquals(cl, Bits.lexicalComparator().compare(u, v), message);
		assertEquals(-cn, v.compareTo(u), message);
		assertEquals(-cn, Bits.numericalComparator().compare(v, u), message);
		assertEquals(-cl, Bits.lexicalComparator().compare(v, u), message);
		if ((v instanceof BitVector) && (u instanceof BitVector)) {
			u = ((BitVector) u).alignedCopy();
			v = ((BitVector) v).alignedCopy();
			assertEquals(cn, u.compareTo(v));
			assertEquals(cn, Bits.numericalComparator().compare(u, v), message);
			assertEquals(cl, Bits.lexicalComparator().compare(u, v), message);
			assertEquals(-cn, v.compareTo(u));
			assertEquals(-cn, Bits.numericalComparator().compare(v, u), message);
			assertEquals(-cl, Bits.lexicalComparator().compare(v, u), message);
		}
	}

	@Test
	public void testToString() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testToString(v);
            }
		}
	}

	private void testToString(BitStore v) {
		String str = v.toString();
		assertEquals(str.length(), v.size());
		assertEquals(v, new BitVector(str));
	}

	@Test
	public void testToByteArray() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testToByteArray(v);
            }
		}
	}

	private void testToByteArray(BitStore v) {
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
        assertArrayEquals(bytes, v.toByteArray());
	}

	@Test
	public void testToBigInteger() {
		int size = validSize(1024);
		if (size < 2) return;
		BitStore v = newStore(size);
		v.fill();
		int f = size / 2;
		int t = size / 2;
		BigInteger i = BigInteger.ONE;
		while (f > 0 && t < size) {
			//evens
			BitStore e = v.range(f, t);
			assertEquals(i.subtract(BigInteger.ONE), e.toBigInteger());
			i = i.shiftLeft(1);
			f--;
			//odds
			BitStore o = v.range(f, t);
			assertEquals(i.subtract(BigInteger.ONE), o.toBigInteger());
			i = i.shiftLeft(1);
			t++;
		}

	}

	@Test
	public void testBitCounts() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testBitCounts(v);
            }
		}
	}

	private void testBitCounts(BitStore v) {
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

	//TODO clean up
	@Test
	public void testStoreMutability2() {
		BitStore v = newStore(validSize(1)).immutable();
		// we repeat operations because some may not have an effect on the BitStore
		// and under the relaxed contract, we're not obliged to throw new ISE.
		for (Operation operation : Operation.values) {
			Op op = v.op(operation);
			try {
				op.with(true);
				op.with(false);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withBit(0, true);
				op.withBit(0, false);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withBits(0, 1L, 1);
				op.withBits(0, 1L, 0);
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withStore(Bits.oneBit());
				op.withStore(Bits.zeroBit());
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
			try {
				op.withStore(0, Bits.oneBit());
				op.withStore(0, Bits.zeroBit());
				fail();
			} catch (IllegalStateException e) {
				//expected
			}
		}
	}

	@Test
	public void testIsAll() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testIsAll(v);
            }
		}
	}

	private void testIsAll(BitStore v) {
		v.clear();
		assertTrue(v.zeros().isAll());
		assertFalse(v.size() != 0 && v.ones().isAll());
		v.fill();
		assertTrue(v.ones().isAll());
		assertFalse(v.size() != 0 && v.zeros().isAll());
		int reps = v.size();
		for (int i = 0; i < reps; i++) {
			int a = random.nextInt(v.size()+1);
			int b = a + random.nextInt(v.size()+1-a);
			v.range(a, b).clear();
			assertTrue(v.range(a,b).zeros().isAll());
			v.range(a, b).fill();
			assertTrue(v.range(a, b).ones().isAll());
		}
	}


	@Test
	public void testTests() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testTests(v);
            }
		}
	}

	private void testTests(BitStore v) {
		int size = v.size();
		assertTrue(v.equals().store(v));
		assertTrue(v.test(BitStore.Test.EQUALS).store(v));
		assertTrue(v.contains().store(v));
		assertTrue(v.test(BitStore.Test.CONTAINS).store(v));
		if (!v.zeros().isAll()) {
			assertFalse(v.excludes().store(v));
			assertFalse(v.test(BitStore.Test.EXCLUDES).store(v));
		}
		assertEquals(size == 0, v.test(BitStore.Test.COMPLEMENTS).store(v));

		if (v instanceof Alignable) {
			BitStore w;
			w = ((Alignable<BitStore>) v).alignedCopy();
			assertTrue(v.equals().store(w));
			assertTrue(v.test(BitStore.Test.EQUALS).store(w));
			assertTrue(w.equals().store(v));
			assertTrue(w.test(BitStore.Test.EQUALS).store(v));
			assertTrue(v.contains().store(w));
			assertTrue(v.test(BitStore.Test.CONTAINS).store(w));
			assertTrue(w.contains().store(v));
			assertTrue(w.test(BitStore.Test.CONTAINS).store(v));
			if (!v.zeros().isAll()) {
				assertFalse(v.excludes().store(w));
				assertFalse(v.test(BitStore.Test.EXCLUDES).store(w));
				assertFalse(w.excludes().store(v));
				assertFalse(w.test(BitStore.Test.EXCLUDES).store(v));
			}
			assertEquals(size == 0, w.test(BitStore.Test.COMPLEMENTS).store(v));

			w = ((Alignable<BitStore>) v).alignedCopy();
			for (int i = 0; i < size; i++) {
				w.setBit(i, true);
				assertTrue( w.contains().store(v) );
				assertTrue( w.test(BitStore.Test.CONTAINS).store(v) );
				assertTrue( v.equals().store(w) || !v.contains().store(w) );
				assertTrue( v.test(BitStore.Test.EQUALS).store(w) || !v.test(BitStore.Test.CONTAINS).store(w) );
			}

			w = ((Alignable<BitStore>) v).alignedCopy();
			for (int i = 0; i < size; i++) {
				w.setBit(i, false);
				assertTrue( v.contains().store(w) );
				assertTrue( v.test(BitStore.Test.CONTAINS).store(w) );
				assertTrue( w.equals().store(v) || !w.contains().store(v) );
				assertTrue( w.test(BitStore.Test.EQUALS).store(v) || !w.test(BitStore.Test.CONTAINS).store(v) );
			}

			if (size != 0) {
				BitStore u = v.mutableCopy();
				u.flip();
				assertTrue(u.test(BitStore.Test.COMPLEMENTS).store(v));
				assertTrue(v.test(BitStore.Test.COMPLEMENTS).store(u));
				u = ((Alignable<BitStore>) v).alignedCopy();
				u.flip();
				assertTrue(u.test(BitStore.Test.COMPLEMENTS).store(v));
				assertTrue(v.test(BitStore.Test.COMPLEMENTS).store(u));
			}
		}
	}

	@Test
	public void testListIterator() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testListIterator(vs[j]);
			}
		}
	}

	private void testListIterator(BitStore v) {
		int size = v.size();

		final BitVector w = new BitVector(size);
		ListIterator<Boolean> i = v.asList().listIterator();
		while (i.hasNext()) {
			w.setBit(i.nextIndex(), i.next());
		}
		assertEquals(v, w);

		final BitVector x = new BitVector(size);
		i = v.asList().listIterator(size);
		while (i.hasPrevious()) {
			x.setBit(i.previousIndex(), i.previous());
		}
		assertEquals(v, x);

		final int a = random.nextInt(size + 1);
		i = v.asList().listIterator(a);
		if (a == size) {
			assertEquals(size, i.nextIndex());
		} else {
			assertEquals(a, i.nextIndex());
			assertEquals(v.getBit(a), i.next().booleanValue());
		}

		i = v.asList().listIterator(a);
		if (a == 0) {
			assertEquals(-1, i.previousIndex());
		} else {
			assertEquals(a - 1, i.previousIndex());
			assertEquals(v.getBit(a - 1), i.previous().booleanValue());
		}
	}

	@Test
	public void testFirstInRange() {
		for (int i = 0; i < 1000; i++) {
			int vSize = validSize(1000);
			BitVector v = new BitVector(vSize);
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

	@Test
	public void testNextOne() {
		for (int i = 0; i < 10; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testNextOne(vs[j]);
			}
		}
	}

	private void testNextOne(BitStore v) {
		int count = 0;
		for (int i = v.ones().first(); i < v.size(); i = v.ones().next(i+1)) {
			count++;
		}
		assertEquals(v.ones().count(), count);
	}

	@Test
	public void testSetBytes() {
		for (int i = 0; i < 1000; i++) {
			BitStore[] vs = randomStoreFamily(10);
			for (int j = 0; j < vs.length; j++) {
				testSetBytes(vs[j]);
			}
		}
	}

	private void testSetBytes(BitStore v) {
		if (v.size() < 8) return;
		BitStore r = randomStore(random.nextInt((v.size())/8*8));
		byte[] bytes = r.toByteArray();
		int position = random.nextInt( v.size() - r.size() + 1 );
		int length = random.nextInt(r.size() + 1);
		int offset = random.nextInt(r.size() - length + 1);
		v.set().withBytes(position, bytes, offset, length);
		assertEquals(r.range(offset, offset + length), v.range(position, position + length));
	}

	@Test
	public void testWithBytes() {
		if (validSize(64) != 64) return;
		byte[] bytes = {0, 0, 0, 0, 0, 0, 0, 1};

		BitStore s = newStore(64);
		s.set().withBytes(0, bytes, 0, 64);
		assertEquals(0, s.ones().first());
		assertEquals(0, s.ones().last());
	}

	@Test
	public void testToBitSet() {
		int size = validSize(200);
		for (int i = 0; i < 1000; i++) {
			BitStore s = randomStore(size);
			if (random.nextBoolean()) {
				int from = random.nextInt(size);
				int to = from + random.nextInt(size - from);
				s = s.range(from, to);
			}
			BitSet bitSet = s.toBitSet();
			assertEquals(s.ones().last() + 1, bitSet.length());
			assertEquals(s, Bits.asStore(bitSet, s.size()));
		}
	}

	@Test
	public void testRegularMatches() {
		int q = 10;
		for (int n = 1; n < 5; n++) {
			int length = n * q;
			if (length != validSize(length)) continue;
			BitStore s = newStore(length);
			for (int i = 0; i < q; i++) {
				s.setBit(i * n, true);
			}
			BitStore seq = s.rangeTo(n);
			Matches matches = s.match(seq);
			assertEquals(q, matches.count());
			Positions positions;
			positions = matches.positions();
			for (int i = 0; i < q; i++) {
				assertEquals(n * i, positions.nextPosition());
			}
			positions = matches.positions(length);
			for (int i = q - 1; i >= 0; i--) {
				assertEquals(n * i, positions.previousPosition());
			}
			for (int i = 1; i < q; i++) {
				assertEquals(n * i, matches.next((i-1) * n + 1));
			}
			for (int i = q; i > 0; i--) {
				assertEquals(n * (i - 1), matches.previous(i * n), s.toString() + " @ " + i + " with " + seq);
			}
		}
	}

	@Test
	public void testVerySimpleMatching() {
		if (!isValidSize(9)) return;
		BitStore s = newStore(Bits.toStore("001001001"));
		Matches match = s.match(Bits.toStore("001"));

		do {
			assertEquals(3, match.count());

			assertEquals(0, match.next(0));
			assertEquals(3, match.next(1));
			assertEquals(3, match.next(2));
			assertEquals(3, match.next(3));
			assertEquals(6, match.next(4));
			assertEquals(6, match.next(5));
			assertEquals(6, match.next(6));
			assertEquals(9, match.next(7));
			assertEquals(9, match.next(8));
			assertEquals(9, match.next(9));

			assertEquals(6, match.previous(9));
			assertEquals(6, match.previous(8));
			assertEquals(6, match.previous(7));
			assertEquals(3, match.previous(6));
			assertEquals(3, match.previous(5));
			assertEquals(3, match.previous(4));
			assertEquals(0, match.previous(3));
			assertEquals(0, match.previous(2));
			assertEquals(0, match.previous(1));
			assertEquals(-1, match.previous(0));

			match = match instanceof OverlappingMatches ? ((OverlappingMatches) match).disjoint() : null;
		} while (match != null);

		DisjointMatches disjoint = s.match(Bits.toStore("001")).disjoint();
		disjoint.replaceAll(Bits.toStore("100"));
		assertEquals(Bits.toStore("100100100"), s);
		disjoint.replaceAll(false);
		assertEquals(Bits.toStore("100000000"), s);
		s.match(true).replaceAll(false);
		assertTrue(s.zeros().isAll());
	}

	@Test
	public void testMatches() {
		for (int i = 0; i < 100; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testMatches(v);
            }
		}
	}

	private void testMatches(BitStore v) {
		int size = v.size();
		int f = random.nextInt(1 + size);
		int t = f + random.nextInt(1 + size - f);
		if (f == t) return;
		BitStore seq = v.range(f,t);
		Matches m = v.match(seq);
		int check;

		// scan forwards
		boolean ff = false;
		int n = m.first();
		check = 0;
		while (true) {
			if (n == f) ff = true;
			if (n == size) break;
			int oldN = n;
			assertEquals(n, m.next(n));
			n = m.next(n + 1);
			assertTrue(n > oldN, "Expected more than " + oldN + " but got " + n);
			if (check ++ == 100000) throw new IllegalStateException("Possible endless loop: " + n + " in " + seq.size());
		}
		assertTrue(ff, seq + " fwd in " + v);

		// scan backwards
		boolean fb = false;
		int p = m.last();
		check = 0;
		while (true) {
			if (p == f) fb = true;
			if (p == -1) break;
			int oldP = p;
			assertEquals(p, m.previous(p + 1));
			p = m.previous(p);
			assertTrue(p < oldP, "Expected less than " + oldP + " but got " + p);
			if (check ++ == 100000) throw new IllegalStateException("Possible endless loop: " + p + " in " + seq.size());
		}
		assertTrue(fb, seq + " bck in " + v);

	}

	@Test
	public void testSimpleMatches() {
		if (validSize(8) != 8) return;
		BitStore bits = newStore(Bits.asStore("11010100"));
		BitStore seq = Bits.asStore("101");
		assertEquals(2, bits.match(seq).first());
		assertEquals(2, bits.match(seq).disjoint().first());
		assertEquals(bits.range(2, 5), seq);
		assertEquals(4, bits.match(seq).last());
		assertEquals(2, bits.match(seq).disjoint().last());
		Positions pos = bits.match(seq).positions();
		assertTrue(pos.hasNext());
		assertEquals(2, pos.next().intValue());
		assertTrue(pos.hasNext());
		assertEquals(4, pos.next().intValue());
		assertFalse(pos.hasNext());
		assertEquals(4, pos.previous().intValue());
		assertTrue(pos.hasPrevious());
		assertEquals(2, pos.previous().intValue());
		assertFalse(pos.hasPrevious());
	}

	@Test
	public void testMatchesIterator() {
		for (int i = 0; i < 100; i++) {
			BitStore[] vs = randomStoreFamily(10);
            for (BitStore v : vs) {
                testMatchesIterator(v);
            }
		}
	}

	private void testMatchesIterator(BitStore v) {
		BitStore b = Bits.toStore(3, random);
		ListIterator<Integer> it;
		Matches match = v.match(b);
		assertEquals(b, match.sequence());
		it = match.positions();
		Set<Integer> ps = new HashSet<>();
		// check it has only valid matches...
		while (it.hasNext()) {
			int p = it.next();
			assertTrue(v.range(p, p + b.size()).equals().store(b));
			ps.add(p);
		}
		// ...and all of them
		int limit = v.size() - b.size();
		for (int p = 0; p < limit; p++) {
			assertEquals(ps.contains(p), v.range(p, p + b.size()).equals().store(b));
		}
	}

	@Test
	public void testSimpleDisjoint() {
		if (!isValidSize(8)) return;
		BitStore s = newStore(Bits.toStore("10101010"));
		BitStore t = Bits.toStore("101");
		Matches matches = s.match(t).disjoint();
		Positions ps = matches.positions();
		assertTrue(ps.isDisjoint());
		assertEquals(1, ps.nextPosition());
		assertEquals(5, ps.nextPosition());
		assertEquals(8, ps.nextPosition());
		assertEquals(5, ps.previousPosition());
		assertEquals(1, ps.previousPosition());
		assertEquals(-1, ps.previousPosition());
		ps.nextPosition();
		ps.replace(Bits.toStore("000"));
		assertEquals(Bits.toStore("10100000"), s);
	}

	@Test
	public void testFlipped() {
		for (int i = 0; i < 1000; i++) {
			BitStore v = randomStore(validSize(random.nextInt(512)));
			BitStore u = v.flipped();
			assertTrue(Operation.XOR.stores(u, v).ones().isAll());

			assertEquals(v.size(), u.size());
			assertEquals(v.isMutable(), u.isMutable());
			assertEquals(v.immutable().isMutable(), v.immutable().flipped().isMutable());
		}
	}

	@Test
	public void testReaderPosition() {
		BitStore[] family = randomStoreFamily(10);
		Random r = new Random(0L);
		for (int i = 0; i < family.length; i++) {
			BitStore store = family[i];
			int size = store.size();
			int p = r.nextInt(size + 1);
			BitReader reader = store.openReader(0, p);
			assertEquals(0, reader.getPosition());
			if (p == 0) {
				try {
					reader.readBoolean();
					fail();
				} catch (EndOfBitStreamException e) {
					/* expected */
				}
			} else {
				boolean b = reader.readBoolean();
				assertEquals(store.getBit(p - 1), b);
				reader.skipBits(Long.MAX_VALUE);
			}
			assertEquals(p, reader.getPosition());
		}
	}

	@Test
	public void testBitMasking() {
		int size = validSize(10);
		BitStore s = newStore(size);
		s.fill();
		int len = size/2;
		for (int i = 0; i <= len; i++) {
			int length = len - i;
			long bits = s.getBits(i, length);
			assertEquals(~(-1 << length), bits);
		}
	}

	@Test
	public void testGetBits() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			// choose params
			int size = validSize(r.nextInt(200));
			int count = 1 + r.nextInt(63);
			int number = size / count;
			if (number == 0) continue;
			int to = r.nextInt(number + 1);
			int from = r.nextInt(to + 1);
			if (from == to) continue;
			long exp = (1L << (count-1)) - 1;
			// set up store
			BitStore s = newStore(size);
			s.fill();
			for (int j = from; j < to; j++) {
				s.setBit(j * count + count - 1, false);
			}
			// check values
			for (int j = from; j < to; j++) {
				long value = s.getBits(j * count, count);
				assertEquals(exp, value);
			}
		}
	}

	@Test
	public void testIntGetBits() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			// choose params
			int size = validSize(r.nextInt(200));
			int count = 1 + r.nextInt(31);
			int number = size / count;
			if (number == 0) continue;
			int to = r.nextInt(number + 1);
			int from = r.nextInt(to + 1);
			if (from == to) continue;
			long exp = (1 << (count-1)) - 1;
			// set up store
			BitStore s = newStore(size);
			s.fill();
			for (int j = from; j < to; j++) {
				s.setBit(j * count + count - 1, false);
			}
			// check values
			for (int j = from; j < to; j++) {
				int value = s.getBitsAsInt(j * count, count);
				assertEquals(exp, value);
			}
		}
	}

	@Test
	public void testSetBits() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			// choose params
			int size = validSize(r.nextInt(200));
			int length = r.nextInt(Math.min(65, size + 1));
			int position = r.nextInt(size - length + 1);
			long value = length == 0L ? 0 : r.nextLong() & ~(-1 << length);
			// set up store
			BitStore s = newStore(size);
			s.setBits(position, value, length);
			// check values
			assertEquals(value, s.getBits(position, length));
			assertTrue(s.rangeTo(position).zeros().isAll());
			assertTrue(s.rangeFrom(position + length).zeros().isAll());
		}
	}

	@Test
	public void testSetBitsAsInt() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			// choose params
			int size = validSize(r.nextInt(200));
			int length = r.nextInt(Math.min(33, size + 1));
			int position = r.nextInt(size - length + 1);
			int value = length == 0 ? 0 : r.nextInt() & ~(-1 << length);
			// set up store
			BitStore s = newStore(size);
			s.setBitsAsInt(position, value, length);
			// check values
			assertEquals(value, s.getBitsAsInt(position, length));
			assertTrue(s.rangeTo(position).zeros().isAll());
			assertTrue(s.rangeFrom(position + length).zeros().isAll());
		}
	}

	private BitStore canon(BitStore store) {
		return new AbstractBitStore() {

			@Override
			public int size() {
				return store.size();
			}

			@Override
			public boolean getBit(int index) {
				return store.getBit(index);
			}
		};
	}
}

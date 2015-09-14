package com.tomgibara.bits;

import java.util.ListIterator;
import java.util.Random;

import org.junit.Assert;

import com.tomgibara.bits.BitStore.Operation;
import com.tomgibara.streams.ByteReadStream;
import com.tomgibara.streams.ByteWriteStream;

import junit.framework.TestCase;

public abstract class BitStoreTest extends TestCase {

	static final Random random = new Random(0);

	// abstracted generation
	
	int validSize(int suggested) {
		return suggested;
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

	public void testSetBit() throws Exception {
		int size = validSize(100);
		BitStore v = newStore(size);
		for (int i = 0; i < size; i++) {
			v.setBit(i, true);
			for (int j = 0; j < size; j++) {
				assertEquals("Mismatch at " + j + " during " + i, j == i, v.getBit(j));
			}
			v.setBit(i, false);
		}
	}

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
		assertEquals(y.getClass() + " " + z.getClass(), y.hashCode(), z.hashCode());
		assertEquals(y, z);
	}

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
	
	public void testStoreSet() {
		int size = validSize(100);
		BitStore s = newStore(size);
		BitStore t = newStore(size);
		s.set().with(true);
		t.clear(true);
		assertEquals(t, s);
		for (int i = 0; i < size; i++) {
			s.set().withBit(i, false);
			t.setBit(i, false);
			assertEquals(t, s);
		}
		assertTrue(s.zeros().isAll());
		BitStore h = newStore(size / 2);
		h.clear(true);
		s.set().withStore(size/4, h);
		assertEquals(s.toString(), h.size(), s.ones().count());
	}

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
		t = t.range(1, t.size());
		s.or().withStore(0, t);
		assertTrue(s.toString(), s.ones().isAll());
	}

	public void testStoreMutability() {
		int size = validSize(1);
		BitStore s = newStore(size);
		BitStore t = s.mutableCopy();
		BitStore u = s.immutableView();
		assertTrue(s.isMutable());
		assertTrue(t.isMutable());
		assertFalse(u.isMutable());

		assertTrue(s.testEquals(t));
		assertTrue(s.testEquals(u));
		s.flip();
		assertFalse(s.testEquals(t));
		assertTrue(s.testEquals(u));
		assertTrue(s.isMutable());

		try {
			u.flip();
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}

		ListIterator<Integer> sp = s.ones().positions();
		ListIterator<Integer> up = u.ones().positions();
		sp.next();
		sp.set(0);
		try {
			up.next();
			up.set(0);
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
	}

	public void testStoreReadWrite() {
		for (int i = 0; i < 100; i++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int length = (size + 7) / 8;
			byte[] bytes;
			{
				bytes = new byte[length];
				ByteArrayBitWriter writer = new ByteArrayBitWriter(bytes);
				s.writeTo(writer);
				writer.flush();
				ByteArrayBitReader reader = new ByteArrayBitReader(bytes);
				BitStore t = newStore(size);
				t.readFrom(reader);
				assertTrue("\n" + s + "\n" + t, s.testEquals(t));
			}
			{
				try (ByteWriteStream writer = new ByteWriteStream(length)) {
					s.writeTo(writer);
					bytes = writer.getBytes(false);
				}
				Assert.assertArrayEquals(s.toByteArray(), bytes);
				ByteReadStream reader = new ByteReadStream(bytes);
				BitStore t = newStore(size);
				t.readFrom(reader);
				if (!s.testEquals(t)) {
					System.out.println(s.getClass());
					System.out.println(BitVector.fromStore(s));
					System.out.println(t.getClass());
					System.out.println(BitVector.fromStore(t));
				}
				assertTrue(String.format("%s%n%s", s, t), s.testEquals(t));
			}
		}
	}

	public void testStoreTests() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = s.mutableCopy();
			BitStore b = newStore(size);
			assertFalse(s.testIntersects(b));
			assertFalse(b.testIntersects(s));
			int reps = (size - s.ones().count()) / 2;
			for (int i = 0; i < reps; i++) {
				int j;
				while (true) {
					j = random.nextInt(size);
					if (!s.getBit(j)) break;
				}
				t.setBit(j, true);
				assertFalse(s.testEquals(t));
				assertFalse(t.testEquals(s));
				assertTrue(t.testContains(s));
				assertFalse(s.testContains(t));
				assertEquals(!s.zeros().isAll(), s.testIntersects(t));
				assertEquals(!s.zeros().isAll(), t.testIntersects(s));
				s.setBit(j, true);
			}
		}
	}

	public void testStoreClear() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = s.mutableCopy();
			s.clear(true);
			t.clear(false);
			assertTrue(s.ones().isAll());
			assertTrue(t.zeros().isAll());
		}
	}

	public void testStoreCount() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = canon(s);
			assertEquals(t.ones().count(), s.ones().count());
		}
	}
	
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
			assertTrue(r + " " + BitVector.fromStore(t), r.testEquals(t));
		}
	}

	public void testObjectMethods() {
		for (int test = 0; test < 1000; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int from = random.nextInt(size + 1);
			int to = from + random.nextInt(size + 1 - from);
			s = s.range(from, to);
			BitStore t = canon(s);
			String types = s.getClass() + " compared to " + t.getClass();
			assertEquals(types, t, s);
			assertEquals(types, s, t);
			assertEquals(types, t.hashCode(), s.hashCode());
			assertEquals(types, s.toString(), t.toString());
		}
	}
	
	public void testStoreToByteArray() {
		for (int test = 0; test < 1000; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			int from = random.nextInt(size + 1);
			int to = from + random.nextInt(size + 1 - from);
			s = s.range(from, to);
			BitStore t = canon(s);
			String types = s.getClass() + " compared to " + t.getClass();
			Assert.assertArrayEquals(types, t.toByteArray(), s.toByteArray());
		}
	}
	
	public void testNumberMethods() {

		//check short store
		BitStore v = newStore(validSize(1));
		testNumberMethods(v, 0);
		v.clear(true);
		testNumberMethods(v, 1);

		//check long store
		v = new BitVector(validSize(128));
		if (v.size() >= 64) {
			testNumberMethods(v, 0);
			v.clear(true);
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

	public void testGetAndModifyBit() {
		for (int i = 0; i < 1000; i++) {
			BitStore v = randomStore(validSize(100));
			testGetAndModifyBit(BitVector.Operation.AND, v);
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
		ListIterator<Integer> it = v.match(ones).positions();
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

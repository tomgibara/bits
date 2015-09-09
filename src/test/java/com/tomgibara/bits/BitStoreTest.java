package com.tomgibara.bits;

import java.util.Random;

import org.junit.Assert;

import com.tomgibara.streams.ByteReadStream;
import com.tomgibara.streams.ByteWriteStream;

import junit.framework.TestCase;

public abstract class BitStoreTest extends TestCase {

	static final Random random = new Random(0);

	abstract BitStore newStore(int size);

	BitStore randomStore(int size) {
		BitStore store = newStore(size);
		for (int i = 0; i < size; i++) {
			store.setBit(i, random.nextBoolean());
		}
		return store;
	}

	int validSize(int suggested) {
		return suggested;
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

	public void testStoreSetGetBit() {
		int size = validSize(100);
		BitStore s = randomStore(size);
		for (int j = 0; j < size; j++) {
			testGetSetBit(s);
		}
	}

	private void testGetSetBit(BitStore s) {
		if (s.size() == 0) return;
		BitVector c = BitVector.fromStore(s);
		int i = random.nextInt(s.size());
		s.setBit(i, !s.getBit(i));
		c.xor().withStore(s);
		assertTrue(c.getBit(i));
		assertEquals(1, c.countOnes());
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
				assertTrue(s.testEquals(t));
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
			int reps = (size - s.countOnes()) / 2;
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
				assertEquals(!s.isAll(false), s.testIntersects(t));
				assertEquals(!s.isAll(false), t.testIntersects(s));
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
			assertTrue(s.isAll(true));
			assertTrue(t.isAll(false));
		}
	}

	public void testStoreCount() {
		for (int test = 0; test < 50; test++) {
			int size = validSize(random.nextInt(200));
			BitStore s = randomStore(size);
			BitStore t = canon(s);
			assertEquals(t.countOnes(), s.countOnes());
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

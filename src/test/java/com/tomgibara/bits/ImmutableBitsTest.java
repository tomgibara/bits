package com.tomgibara.bits;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;

import junit.framework.TestCase;

public abstract class ImmutableBitsTest extends TestCase {

	abstract boolean isOnes();

	private final Random random = new Random(0L);
	
	BitStore newBits(int size) {
		return Bits.bits(isOnes(), size);
	}
	
	public void testGetBit() {
		for (int i = 0; i < 100; i++) {
			int size = random.nextInt(1000);
			BitStore s = newBits(size);
			for (int j = 0; j < size; j++) {
				assertEquals(isOnes(), s.getBit(j));
			}
		}
	}
	
	public void testImmutable() {
		try {
			newBits(2).setBit(0, !isOnes());
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
	}
	
	public void testReader() {
		for (int i = 0; i < 100; i++) {
			int size = random.nextInt(1000);
			int position = random.nextInt(size + 1);
			BitStore s = newBits(size);
			BitVector v = new BitVector(size);
			BitReader r = s.openReader(position);
			BitWriter w = v.openWriter(position);
			int length = size - position;
			Bits.transfer(r, w, length);
			assertEquals(v.rangeTo(length), s.rangeTo(length));
		}
	}
	
	public void testMatches() {
		BitStore bits = newBits(10);
		{
			BitMatches matches = bits.match(isOnes());
			SortedSet<Integer> set = matches.asSet();
			assertEquals(10, set.size());
			assertEquals(new TreeSet<Integer>(Arrays.asList(new Integer[] {0,1,2,3,4,5,6,7,8,9})), set);
			Iterator<Integer> it = set.iterator();
			for (int i = 0; i < 10; i++) {
				int p = it.next().intValue();
				assertEquals(i, p);
			}
			Positions ps = matches.positions(10);
			assertEquals(10, ps.nextIndex());
			for (int i = 9; i >= 0; i--) {
				assertEquals(i + 1, ps.nextIndex());
				assertEquals(i, ps.previousIndex());
				int p = ps.previous().intValue();
				assertEquals(i, p);
			}
			assertEquals(-1, ps.previousIndex());
			assertEquals(-1, ps.previousIndex());
		}
		{
			BitMatches matches = bits.match(!isOnes());
			assertTrue(matches.asSet().isEmpty());
			assertEquals(10, matches.positions().nextPosition());
			assertEquals(-1, matches.positions().previousPosition());
			assertFalse(matches.positions().hasPrevious());
			assertFalse(matches.positions().hasNext());
		}
	}
	
	public void testEquals() {
		for (int i = 0; i < 1000; i++) {
			BitVector v = new BitVector(random, 0.8f, random.nextInt(12));
			assertEquals(v.match(isOnes()).isAll(), newBits(v.size()).equals(v));
		}
	}
}

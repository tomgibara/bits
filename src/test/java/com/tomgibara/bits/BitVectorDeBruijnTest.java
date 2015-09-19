package com.tomgibara.bits;

import java.util.HashSet;
import java.util.Set;

import com.tomgibara.bits.BitVector;

import junit.framework.TestCase;

public class BitVectorDeBruijnTest extends TestCase {

	public void testDeBruijn() {
		for (int size = 1; size < 8; size++) {
			testDeBruijn(size);
		}
	}

	private void testDeBruijn(int size) {
		BitVector sequence = generateDeBruijn(size);
		HashSet<Integer> values = new HashSet<Integer>();
		for (int i = 0; i < sequence.size() - size; i++) {
			int value = (int) sequence.getBits(i, size);
			values.add(value);
		}
		assertEquals(1 << size, values.size());
	}

	// generates a binary De Bruijn sequence over words of length n
	private BitVector generateDeBruijn(int n) {
		// Check arguments
		if (n < 0) throw new IllegalArgumentException("n is negative");
		if (n > 31) throw new IllegalArgumentException("n exceeds 31");
		// There are 2^n words in the entire sequence
		int length = 1 << n;
		// Create a set that records which words we have already seen
		Set<Integer> memory = new BitVector(length).ones().asSet();
		// Store the sequence with an extra n bits
		// makes things easier for enumerating the values
		BitVector sequence = new BitVector(length + n);
		// Seed the sequence with the initial value (n 1s)
		sequence.range(0, n).clearWithOnes();
		// Populate the sequence
		for (int i = 0; i < length; i++) {
			// Extract the current word from the sequence
			int word = (int) sequence.getBits(i, n);
			// Record that we've seen it
			memory.add(word);
			// Shift the word left, populating the rightmost bit with zero
			// if we've seen the word before, use a one instead
			sequence.setBit(i + n, memory.contains(word >> 1));
		}
		return sequence;
	}

}

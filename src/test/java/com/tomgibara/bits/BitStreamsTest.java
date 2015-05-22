/*
 * Copyright 2011 Tom Gibara
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

import java.util.Random;

import com.tomgibara.bits.BitStreams;
import com.tomgibara.bits.IntArrayBitReader;
import com.tomgibara.bits.IntArrayBitWriter;

import junit.framework.TestCase;

public class BitStreamsTest extends TestCase {

	public void testIsSameBits() {
		Random rand = new Random(0L);
		for (int size = 0; size < 100; size++) {
			int[] ints = new int[(size + 7)/ 8];
			IntArrayBitWriter w = new IntArrayBitWriter(ints, size);
			for (int i = 0; i < size; i++) w.writeBoolean(rand.nextBoolean());
			IntArrayBitReader r = new IntArrayBitReader(ints, size);
			IntArrayBitReader s = new IntArrayBitReader(ints, size);
			assertTrue(BitStreams.isSameBits(r, s));
			r.setPosition(0);
			s.setPosition(0);
			assertTrue(BitStreams.isSameBits(r, s));

			if (size > 0) {
				s.setPosition(0);
				int[] tints = ints.clone();
				int bit = rand.nextInt(size);
				int index = bit >> 5;
				int mask = 1 << (31 - (bit & 31));
				tints[index] ^= mask;
				IntArrayBitReader t = new IntArrayBitReader(tints, size);
				assertFalse(BitStreams.isSameBits(s, t));
				s.setPosition(0);
				t.setPosition(0);
				assertFalse(BitStreams.isSameBits(t, s));

				if (size > 2) {
					int sub = 1 + rand.nextInt(size - 1);
					s.setPosition(0);
					t = new IntArrayBitReader(ints, size - sub);
					assertFalse(BitStreams.isSameBits(s, t));
					s.setPosition(0);
					t.setPosition(0);
					assertFalse(BitStreams.isSameBits(t, s));
				}
			}
		}
	}

}

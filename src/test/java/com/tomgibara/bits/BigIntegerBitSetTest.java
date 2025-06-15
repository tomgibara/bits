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
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BigIntegerBitSetTest {

	private final Random random = new Random(0L);

	@Test
	public void testGeneral() {
		for (int i = 0; i < 1000; i++) {
			int size = random.nextInt(500);
			BigInteger b = new BigInteger(size, random);
			BitStore s = new BigIntegerBitStore(b);
			assertFalse(s.isMutable());
			BitVector v = BitVector.fromBigInteger(b);
			assertEquals(s.size(), v.size());
			assertEquals(s,v);
			assertEquals(v,s);
			assertEquals(v, s.mutableCopy());
			size = s.size(); // may be shorter due to leading zeros
			for (int j = 0; j < size; j++) {
				assertEquals(v.getBit(j), s.getBit(j));
			}
			BitStore u = Bits.toStore(random.nextInt(500), random);
			assertEquals(v.compareNumericallyTo(u), s.compareNumericallyTo(u));
			assertEquals(v.asNumber().doubleValue(), s.asNumber().doubleValue());
		}
	}

}

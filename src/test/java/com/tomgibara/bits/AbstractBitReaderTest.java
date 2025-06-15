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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractBitReaderTest {

	abstract BitReader readerFor(BitStore vector);

	static boolean equal(BitReader r, BitReader s) {
		while (true) {
			int rv;
			int sv;
			try {
				rv = r.readBit();
			} catch (EndOfBitStreamException e) {
				rv = 2;
			}
			try {
				sv = s.readBit();
			} catch (EndOfBitStreamException e) {
				sv = 2;
			}
			if (rv != sv) return false;
			if (rv == 2) return true;
		}
	}


	@Test
	public void testReadBoolean() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = r.nextInt(25) * 32;
			BitStore source = Bits.toStore(size, r);
			BitReader reader = readerFor(source);

			for (int j = 0; j < size; j++) {
				assertEquals(source.getBit(j), reader.readBoolean(), "at bit " + j);
			}
		}
	}

	@Test
	public void testSkip() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {

			int size = r.nextInt(2500) * 32;
			BitStore source = Bits.toStore(size, r);
			BitReader reader = readerFor(source);

			long skipped = 0L;
			long read = 0L;
			while (true) {
				long oldpos = reader.getPosition();
				long toskip = r.nextInt(50);
				long actual = reader.skipBits(toskip);
				assertTrue(actual <= toskip);
				skipped += actual;
				long newpos = reader.getPosition();
				assertEquals(newpos, oldpos + actual);
				if (newpos == size) break;
				assertTrue(actual == toskip);
				int position = (int) reader.getPosition();
				assertEquals(source.getBit(position), reader.readBoolean(), "at bit " + position);
				read++;
			}
			assertEquals(size, skipped + read);
		}
	}

	@Test
	public void testRead() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = r.nextInt(25) * 32;
			BitStore source = Bits.toStore(size, r);
			BitStore reverse = source.mutableCopy();
			reverse.permute().reverse();
			BitReader reader = readerFor(source);

			while (true) {
				int oldpos = (int) reader.getPosition();
				int count = Math.min(size - oldpos, r.nextInt(33));
				int bits = reader.read(count);
				int newpos = (int) reader.getPosition();
				assertEquals(oldpos + count, newpos);
				int actual = (int) reverse.getBits(size - newpos, count);
				assertEquals(actual, bits);
				if (newpos == size) break;
			}
		}
	}

	@Test
	public void testReadUntil() {
		testReadUntil(true);
		testReadUntil(false);
	}

	private void testReadUntil(boolean ones) {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int a = r.nextInt(40);
			int b = r.nextInt(40);
			int c = 32 - ((a + b) & 31);
			BitVector bits = new BitVector(a + b + c);
			bits.range(0   , a   ).setAll( ones );
			bits.range(a  , a+b  ).setAll(!ones );
			bits.range(a+b, a+b+c).setAll( ones );
			for (int j = 0; j < a + b; j++) {
				BitReader reader = readerFor(bits);
				reader.skipBits(j);
				int read = reader.readUntil(ones);
				if (j < a) {
					assertEquals(0, read);
				} else {
					assertEquals(a + b - j, read);
				}
			}
		}
	}

	//TODO should test return value
	@Test
	public void testSetPosition() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = 64 + 64 * r.nextInt(10);
			BitStore source = Bits.toStore(size, r);
			BitReader reader = readerFor(source);

			for (int j = 0; j < 100; j++) {
				int position = r.nextInt(size);
				long newPosition = reader.setPosition(position);
				if (newPosition != position) {
					// assume it's that backward seek not supported
					reader = readerFor(source);
					reader.setPosition(position);
				}
				assertEquals(source.getBit(position), reader.readBoolean(), "at bit " + position);
			}
		}
	}

	@Test
	public void testSkipLimits() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = 64 + 64 * r.nextInt(10);
			BitStore source = Bits.toStore(size, r);
			BitReader reader = readerFor(source);

			assertEquals(size, reader.skipBits(size));
			assertEquals(0, reader.skipBits(1));
			long back = reader.skipBits(-size);
			if (back != 0L) { // may not be supported
				assertEquals(-size, back);
				assertEquals(0, reader.skipBits(-1));
				assertEquals(1, reader.skipBits(1));
				assertEquals(-1, reader.skipBits(-1));
			}
			assertEquals(0, readerFor(source).skipBits(Long.MIN_VALUE));
			assertEquals(size, readerFor(source).skipBits(Long.MAX_VALUE));
		}
	}
}

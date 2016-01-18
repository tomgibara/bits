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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tomgibara.bits.BitBoundary;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;

import junit.framework.TestCase;

// sizing is in multiples of 32 for memory bit writer
public abstract class AbstractBitWriterTest extends TestCase {

	static byte bite(String binary) {
		return (byte) Integer.parseInt(binary, 2);
	}

	abstract BitWriter newBitWriter(long size);

	abstract BitReader bitReaderFor(BitWriter writer);

	abstract BitBoundary getBoundary();

	public void testWriteBit() {
		Random r = new Random();
		for (int i = 0; i < 100; i++) {
			BitWriter writer = newBitWriter(64);
			long bits = r.nextLong();
			for (int j = 63; j >= 0; j--) {
				writer.writeBit((int) (bits >> j));
			}
			writer.flush();
			BitReader reader = bitReaderFor(writer);
			long back = reader.readLong(64);
			assertEquals(bits, back);
		}
	}

	public void testPass() {
		int size = 1000;
		for (long seed = 0; seed < 10; seed++) {
			testPass(size, seed);
		}
	}

	private void testPass(int size, long seed) {
		BitWriter writer = newBitWriter(size * 32);
		ArrayList<Point> list = new ArrayList<Point>(size);

		Random r = new Random(seed);
		for (int i = 0; i < size; i++) {
			int x = r.nextInt(33);
			int y = r.nextInt() & ((1 << x) - 1);
			writer.write(y, x);
			list.add( new Point(x, y) );
		}
		long pos = writer.getPosition();
		writer.flush();
		assertEquals(0, writer.padToBoundary(getBoundary()));

		BitReader reader = bitReaderFor(writer);
		for (int i = 0; i < size; i++) {
			Point pt = list.get(i);
			int v = reader.read(pt.x);
			if (pt.y != v) throw new RuntimeException("Failed at " + i + ": " + v + " is not " + pt.y);
		}
		if (reader.getPosition() != pos) throw new RuntimeException();
	}

	public void testRuns() {
		int size = 1000;
		for (long seed = 0; seed < 10; seed++) {
			testRuns(size, seed);
		}
	}

	private void testRuns(int size, long seed) {
		int maxrunlength = 8192;
		int asize = size * maxrunlength * 2;
		BitWriter writer = newBitWriter((asize + 31) / 32 * 32);
		ArrayList<Point> list = new ArrayList<Point>(size);

		Random r = new Random(1);
		for (int i = 0; i < size; i++) {
			long pos = writer.getPosition();
			int x = r.nextInt(maxrunlength);
			int y = r.nextInt(maxrunlength);
			writer.writeBooleans(false, x);
			assertEquals(pos + x, writer.getPosition());
			writer.writeBooleans(true, y);
			assertEquals(pos + x + y, writer.getPosition());
			list.add( new Point(x, y) );
		}
		long pos = writer.getPosition();
		writer.flush();
		assertEquals(0, writer.padToBoundary(getBoundary()));

		BitReader reader = bitReaderFor(writer);
		for (int i = 0; i < size; i++) {
			Point pt = list.get(i);
			for(int x = 0; x < pt.x; x++) {
				if (reader.readBit() != 0) throw new RuntimeException("Failed at " + i + ": expected 0");
			}
			for(int y = 0; y < pt.y; y++) {
				int v = reader.readBit();
				if (v != 1) throw new RuntimeException("Failed at " + i + ": expected 1, got " + v);
			}
		}
		assertEquals(pos, reader.getPosition());
	}

	public void testSetPosition() {
		BitWriter writer = newBitWriter(64);
		Random random = new Random(0);
		for (int i = 0; i < 100; i++) {
			List<Integer> ps = new ArrayList<Integer>(64);
			for (int p = 0; p < 64; p++) ps.add(p);
			Collections.shuffle(ps, random);

			for (int p : ps) {
				long np = writer.setPosition(p);
				if (np == -1L) return; //can't test this, setting position not supported
				assertEquals(p, np);
				writer.writeBoolean(true);
			}
			writer.flush();
			writer.setPosition(64);

			BitReader reader = bitReaderFor(writer);
			for (int j = 0; j < 64; j++) {
				assertTrue(reader.readBoolean());
			}
		}
	}

	public void testLength() {
		Random random = new Random(0);
		for (int i = 0; i < 100; i++) {
			int length = random.nextInt(1000);
			BitWriter writer = newBitWriter(length);
			for (int j = 0; j < length; j++) {
				writer.writeBoolean(true);
			}
			writer.flush();
			BitReader reader = bitReaderFor(writer);
			for (int j = 0; j < length; j++) {
				assertTrue(reader.readBoolean());
			}
			try {
				reader.read(getBoundary().mask + 1);
				fail();
			} catch (EndOfBitStreamException e) {
				/* expected */
			}
		}
	}
}

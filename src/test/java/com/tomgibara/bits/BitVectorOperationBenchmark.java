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

import java.util.Random;

public class BitVectorOperationBenchmark {

	private static final int big = 10000;
	private static final int reps = 100000;
	private static final boolean markdown = true;
	private static final int[] sizes = {10, 100, 1000, 10000};
	private static final boolean[] aligns = {true, false};
	private static final Operation[] ops = Operation.values;

	private static void out(String str) {
		if (markdown) {
			System.out.print('|');
			System.out.print(str.replace(',', '|'));
			System.out.println('|');
		} else {
			System.out.println(str);
		}
	}

	public static void main(String[] args) {
			out("Reps  ,    Size, Op     , Aligned, Time ms, Call ms, Bit ns   ");
		if (markdown)
			out("------,-------:,--------,--------,-------:,-------:,---------:");
		test(true);
		test(false);
	}

	private static void test(boolean warmup) {
        for (var size : sizes) {
            for (var operation : ops) {
                for (var aligned : aligns) {
                    test(warmup, size, operation, aligned, false);
                }
            }
        }
	}

	private static void test(boolean warmup, int size, Operation op, boolean aligned, boolean generic) {
		Random r = new Random(0);

		// set up vectors
		BitVector[] vs = new BitVector[64];
		BitVector[] ws = new BitVector[64];
		{
			BitVector v = new BitVector(r, 0.5f, big  + 64);
			BitVector w = new BitVector(r, 0.5f, size + 64);
			for (int i = 0; i < 64; i++) {
				int offset = aligned ? 0 : i;
				vs[i] = v.range(offset, big + offset);
				ws[i] = w.range(offset, size + offset);
			}
		}

		// set up offsets
		{
			int[] offsets = new int[reps];
			for (int i = 0; i < reps; i++) {
				offsets[i] = r.nextInt(64);
			}
		}

		// time operation
		long time;
		{
			long start = System.currentTimeMillis();
			if (generic) {
				for (int i = 0; i < reps; i++) {
					BitVector v = vs[i & 63];
					BitVector w = ws[i & 63];
					v.op(op).withStore(0, w);
				}
			} else {
				switch (op) {
				case AND:
					for (int i = 0; i < reps; i++) {
						BitVector v = vs[i & 63];
						BitVector w = ws[i & 63];
						v.and().withStore(0, w);
					}
					break;
				case OR:
					for (int i = 0; i < reps; i++) {
						BitVector v = vs[i & 63];
						BitVector w = ws[i & 63];
						v.or().withStore(0, w);
					}
					break;
				case SET:
					for (int i = 0; i < reps; i++) {
						BitVector v = vs[i & 63];
						BitVector w = ws[i & 63];
						v.set().withStore(0, w);
					}
					break;
				case XOR:
					for (int i = 0; i < reps; i++) {
						BitVector v = vs[i & 63];
						BitVector w = ws[i & 63];
						v.xor().withStore(0, w);
					}
					break;
				}
			}
			long finish = System.currentTimeMillis();
			time = finish - start;
		}

		// record result
		if (!warmup) {
			double opTime = (double) time / reps;
			double bitTime = 1000000.0 * time / reps / size;
			out(String.format("%6d, %7d, %7s, %7s, %7d, %7.5f, %9f", reps, size, op, aligned, time, opTime, bitTime));
		}
	}

}

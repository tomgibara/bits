package com.tomgibara.bits;

import java.util.Random;

import com.tomgibara.bits.BitVector.Operation;

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
		for (int size = 0; size < sizes.length; size++) {
			for (int op = 0; op < ops.length; op++) {
				for (int align = 0; align < aligns.length; align++) {
					test(sizes[size], ops[op], aligns[align]);
				}
			}
		}
	}

	private static void test(int size, Operation op, boolean aligned) {
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
			for (int i = 0; i < reps; i++) {
				BitVector v = vs[i & 63];
				BitVector w = ws[i & 63];
				v.op(op).withVector(0, w);
			}
			long finish = System.currentTimeMillis();
			time = finish - start;
		}

		// record result
		double opTime = (double) time / reps;
		double bitTime = 1000000.0 * time / reps / size;
		out(String.format("%6d, %7d, %7s, %7s, %7d, %7.5f, %9f", reps, size, op, aligned, time, opTime, bitTime));
	}

}

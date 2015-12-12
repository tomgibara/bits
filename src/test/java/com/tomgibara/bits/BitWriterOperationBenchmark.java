package com.tomgibara.bits;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.ByteArrayBitWriter;
import com.tomgibara.bits.IntArrayBitWriter;
import com.tomgibara.bits.NullBitWriter;
import com.tomgibara.bits.OutputStreamBitWriter;

public class BitWriterOperationBenchmark {

	private static final int size = 100 * 1024 * 1024;

	public static void main(String[] args) {
		timeWriters();
		timeWriters();
		timeWriters();
	}

	private static void timeWriters() {
		for (Op op : Arrays.asList(
				new WriteBoolean(),
				new WriteBit(),
				new Write9Bits(),
				new Write17Bits(),
				new Write33Bits(),
				new Pad255(),
				new Pad256())) {
			timeWriter(new NullBitWriter(), op);
			timeWriter(new ByteArrayBitWriter(new byte[(size + 7)/8]), op);
			timeWriter(new OutputStreamBitWriter(new ByteArrayOutputStream((size + 7) / 8)), op);
			timeWriter(new IntArrayBitWriter(new int[(size + 31)/32]), op);
			timeWriter(new BitVector(size).openWriter(), op);
		}
		System.out.println("--------");
	}

	private static void timeWriter(BitWriter writer, Op op) {
		long start = System.currentTimeMillis();
		op.run(writer);
		long finish = System.currentTimeMillis();
		writeResult(writer, op, finish - start);
	}

	private static void writeResult(BitWriter writer, Op op, long time) {
		String writerName = writer.getClass().getName().substring("com.tomgibara.crinch.bits.".length());
		String opName = op.getClass().getName().substring("com.tomgibara.crinch.bits.BitWriterOperationBenchmark$".length());
		System.out.println(writerName + "," + opName + "," + time);
	}

	private interface Op {

		void run(BitWriter writer);
	}

	private static class WriteBoolean implements Op {

		@Override
		public void run(BitWriter writer) {
			for (int i = 0; i < size; i++) {
				writer.writeBoolean(false);
			}
		}

	}

	private static class WriteBit implements Op {

		@Override
		public void run(BitWriter writer) {
			for (int i = 0; i < size; i++) {
				writer.writeBit(0);
			}
		}

	}

	private static class Write9Bits implements Op {

		@Override
		public void run(BitWriter writer) {
			int limit = size / 9;
			for (int i = 0; i < limit; i++) {
				writer.write(0, 9);
			}
		}

	}

	private static class Write17Bits implements Op {

		@Override
		public void run(BitWriter writer) {
			int limit = size / 17;
			for (int i = 0; i < limit; i++) {
				writer.write(0, 17);
			}
		}

	}

	private static class Write33Bits implements Op {

		@Override
		public void run(BitWriter writer) {
			int limit = size / 33;
			for (int i = 0; i < limit; i++) {
				writer.write(0L, 33);
			}
		}

	}

	private static class Pad255 implements Op {

		@Override
		public void run(BitWriter writer) {
			int limit = size / 255;
			for (int i = 0; i < limit; i++) {
				writer.writeBooleans(false, 255);
			}
		}

	}

	private static class Pad256 implements Op {

		@Override
		public void run(BitWriter writer) {
			int limit = size / 256;
			for (int i = 0; i < limit; i++) {
				writer.writeBooleans(false, 256);
			}
		}

	}

}

package com.tomgibara.bits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tomgibara.bits.BitVector;

public class BitVectorRangeViewBenchmark {

	private static void pause(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void gc() {
		System.gc();
		pause(1);
	}

	private static final int wordSize = 128;
	private static final int words = 100000;
	private static final int reps = 100;
	private static final int samples = 6;

	public static void main(String[] args) {
		runTests(createFastTests());
		runTests(createSlowTests());
	}

	private static Set<Test> createFastTests() {
		return new LinkedHashSet<BitVectorRangeViewBenchmark.Test>(Arrays.asList(new SetTest(), new FindOneTest(), new FindZeroTest(), new CountOnesTest(), new CountZerosTest()));
	}

	private static Set<Test> createSlowTests() {
		return new LinkedHashSet<BitVectorRangeViewBenchmark.Test>(Arrays.asList(new RotateTest()));
	}

	private static void runTests(Set<Test> tests) {
		for (Test test : tests) {
			test.clearTimes();
		}
		for (int i = 0; i < samples; i++) {
			timeTests(tests);
		}
		outputTests(tests);
	}

	private static void timeTests(Set<Test> tests) {
		for (Test test : tests) {
			new TestTimer(test).run();
		}
	}

	private static void outputTests(Set<Test> tests) {
		for (Test test : tests) {
			System.out.println(test);
		}
	}

	//TODO needs to be repaired
	private static abstract class Test {

		private final List<Long> rangeTimes = new ArrayList<Long>();
		private final List<Long> viewTimes = new ArrayList<Long>();

		abstract String getName();

		abstract void operateWithRange(BitVector v);

		abstract void operateWithView(BitVector v);

		void perform(boolean range) {
			BitVector v = new BitVector(wordSize * words);
			if (range) {
				for (int i = 0; i < reps; i++) operateWithRange(v);
			} else {
				for (int i = 0; i < reps; i++) operateWithView(v);
			}
		}

		void addTime(boolean range, long time) {
			(range ? rangeTimes : viewTimes).add(time);
		}

		void clearTimes() {
			rangeTimes.clear();
			viewTimes.clear();
		}

		@Override
		public String toString() {
			return String.format("%-15s: %7.3f, %7.3f", getName(), mean(rangeTimes), mean(viewTimes));
		}

		private double mean(List<Long> times) {
			switch (times.size()) {
			case 0: return 0L;
			case 1: return times.get(0);
			case 2: return (times.get(0) + times.get(1)) / 2.0;
			default:
				Collections.sort(times);
				int a = times.size() / 3;
				int b = 2 * times.size() / 3;
				long sum = 0L;
				for (int i = a; i < b; i++) {
					sum += times.get(i);
				}
				return (double) sum / (b - a);
			}

		}

	}

	private static class SetTest extends Test {

		@Override
		String getName() {
			return "Set";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).clear(false);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).clear(false);
			}
		}

	}

	private static class FindOneTest extends Test {

		@Override
		String getName() {
			return "Find one";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.firstOneInRange(i * wordSize, i * wordSize + wordSize);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).firstOne();
			}
		}

	}

	private static class FindZeroTest extends Test {

		@Override
		String getName() {
			return "Find zero";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.firstZeroInRange(i * wordSize, i * wordSize + wordSize);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).firstZero();
			}
		}

	}

	private static class CountOnesTest extends Test {

		@Override
		String getName() {
			return "Count ones";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.countOnes(i * wordSize, i * wordSize + wordSize);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).countOnes();
			}
		}

	}

	private static class CountZerosTest extends Test {

		@Override
		String getName() {
			return "Count zeros";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.countOnes(i * wordSize, i * wordSize + wordSize);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).countOnes();
			}
		}

	}

	private static class RotateTest extends Test {

		@Override
		String getName() {
			return "Rotate 1";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.rotateRange(i * wordSize, i * wordSize + wordSize, 1);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).rotate(1);
			}
		}

	}

	private static class TestTimer implements Runnable {

		private final Test test;

		public TestTimer(Test test) {
			this.test = test;
		}

		@Override
		public void run() {
			gc();
			time(true);
			gc();
			time(false);
		}

		private void time(boolean range) {
			long start = System.currentTimeMillis();
			test.perform(range);
			long end = System.currentTimeMillis();
			long time = end - start;
			test.addTime(range, time);
		}

	}

}

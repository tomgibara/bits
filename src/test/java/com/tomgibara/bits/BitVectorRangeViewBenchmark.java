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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
	private static final int warmups = 10;
	private static final int samples = 20;

	public static void main(String[] args) {
		runTests(createTests());
	}

	private static Set<Test> createTests() {
		return new LinkedHashSet<BitVectorRangeViewBenchmark.Test>(Arrays.asList(/*new ClearTest(),*/ new FindOneTest(), new FindZeroTest(), new CountOnesTest(), new CountZerosTest()));
	}

	private static void runTests(Set<Test> tests) {
		for (Test test : tests) {
			test.clearTimes();
		}
		for (int i = 0; i < samples; i++) {
			timeTests(i < warmups, tests);
		}
		outputTests(tests);
	}

	private static void timeTests(boolean warmup, Set<Test> tests) {
		for (Test test : tests) {
			new TestTimer(test, warmup).run();
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
			//BitVector v = new BitVector(wordSize * words);
			BitVector v = new BitVector(new Random(0L), wordSize * words);
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

	// exclude this test because it mutates the vector
	@SuppressWarnings("unused")
	private static class ClearTest extends Test {

		@Override
		String getName() {
			return "Clear";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.clear(i * wordSize, i * wordSize + wordSize, false);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				v.range(i * wordSize, i * wordSize + wordSize).clear();
			}
		}

	}

	private static class FindOneTest extends Test {

		int dummy = 0;

		@Override
		String getName() {
			return "Find one";
		}

		@Override
		void operateWithRange(BitVector v) {
			for (int i = 0; i < words; i++) {
				dummy += v.firstOneInRange(i * wordSize, i * wordSize + wordSize);
			}
		}

		@Override
		void operateWithView(BitVector v) {
			for (int i = 0; i < words; i++) {
				dummy += v.range(i * wordSize, i * wordSize + wordSize).ones().first();
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
				v.range(i * wordSize, i * wordSize + wordSize).zeros().first();
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
				v.range(i * wordSize, i * wordSize + wordSize).ones().count();
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
				v.range(i * wordSize, i * wordSize + wordSize).ones().count();
			}
		}

	}

	private static class TestTimer implements Runnable {

		private final Test test;
		private final boolean warmup;

		public TestTimer(Test test, boolean warmup) {
			this.test = test;
			this.warmup = warmup;
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
			if (!warmup) test.addTime(range, time);
		}

	}

}

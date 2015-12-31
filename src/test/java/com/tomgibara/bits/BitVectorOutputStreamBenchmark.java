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

import java.io.IOException;
import java.io.OutputStream;

import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public class BitVectorOutputStreamBenchmark {

	public static void main(String[] args) throws IOException {
		runBenchmark();
		runBenchmark();
	}
		
	private static void runBenchmark() throws IOException {
		BitVector[] vs = BitVectorTest.randomVectorFamily(1000, 200000);
		OutputStream out = new NullOutputStream();
		WriteStream stream = Streams.streamOutput(out);
		
		long timeA;
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100; i++) {
				for (BitVector v : vs) {
					v.writeTo(out);
				}
			}
			long finish = System.currentTimeMillis();
			timeA = finish - start;
		}

		long timeB;
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100; i++) {
				for (BitVector v : vs) {
					v.writeTo(stream);
				}
			}
			long finish = System.currentTimeMillis();
			timeB = finish - start;
		}
		System.out.println("OutputStream " + timeA + "ms");
		System.out.println("WriteStream  " + timeB + "ms");
	}


}

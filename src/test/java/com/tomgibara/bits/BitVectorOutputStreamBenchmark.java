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

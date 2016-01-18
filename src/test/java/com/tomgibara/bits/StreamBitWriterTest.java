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

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class StreamBitWriterTest extends AbstractByteBasedBitWriterTest {

	@Override
	StreamBitWriter newBitWriter(long size) {
		return new TestWriter( Streams.bytes((int) size) );
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		ReadStream readStream = ((TestWriter) writer).bytes.readStream();
		return new StreamBitReader(readStream);
	}

	@Override
	byte[] getWrittenBytes(BitWriter writer) {
		return ((TestWriter) writer).bytes.bytes();
	}

	private static class TestWriter extends StreamBitWriter {

		final StreamBytes bytes;

		TestWriter(StreamBytes bytes) {
			super(bytes.writeStream());
			this.bytes = bytes;
		}
	}
}

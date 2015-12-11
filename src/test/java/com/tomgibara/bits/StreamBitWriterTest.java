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

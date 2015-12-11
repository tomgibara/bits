package com.tomgibara.bits;

import com.tomgibara.streams.Streams;

public class StreamBitReaderTest extends AbstractBitReaderTest {

	@Override
	BitReader readerFor(BitVector vector) {
		vector = vector.mutableCopy();
		vector.permute().reverse();
		return new StreamBitReader(Streams.bytes(vector.toByteArray()).readStream());
	}

}

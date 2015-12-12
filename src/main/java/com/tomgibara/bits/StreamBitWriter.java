package com.tomgibara.bits;

import com.tomgibara.streams.StreamException;
import com.tomgibara.streams.WriteStream;

class StreamBitWriter extends ByteBasedBitWriter {

	private final WriteStream stream;
	
	StreamBitWriter(WriteStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		this.stream = stream;
	}
	
	@Override
	protected void writeByte(int value) throws BitStreamException {
		try {
			stream.writeByte((byte) value);
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}
	
	@Override
	protected void writeBytes(byte[] bytes, int offset, int length) throws BitStreamException {
		try {
			stream.writeBytes(bytes, offset, length);
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}

}

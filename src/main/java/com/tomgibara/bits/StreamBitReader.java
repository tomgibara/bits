package com.tomgibara.bits;

import com.tomgibara.streams.EndOfStreamException;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamException;

class StreamBitReader extends ByteBasedBitReader {

	private final ReadStream stream;
	
	StreamBitReader(ReadStream stream) {
		if (stream == null) throw new IllegalArgumentException("null stream");
		this.stream = stream;
	}
	
	@Override
	protected int readByte() throws BitStreamException {
		try {
			return stream.readByte() & 0xff;
		} catch (EndOfStreamException e) {
			return -1;
		} catch (StreamException e) {
			throw new BitStreamException(e);
		}
	}

	@Override
	protected long skipBytes(long count) throws BitStreamException {
		long c = 0;
		while (c < count) try {
			stream.readByte();
			c ++;
		} catch (EndOfStreamException e) {
			return c;
		} catch (StreamException e) {
			throw new BitStreamException();
		}
		return c;
	}

	@Override
	protected long seekByte(long index) throws BitStreamException {
		return -1L;
	}

}

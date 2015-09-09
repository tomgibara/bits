package com.tomgibara.bits;

import java.io.IOException;
import java.io.OutputStream;

final class NullOutputStream extends OutputStream {

	@Override
	public void write(int b) throws IOException {
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

}

package com.tomgibara.bits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.ByteBasedBitWriter;
import com.tomgibara.bits.InputStreamBitReader;
import com.tomgibara.bits.OutputStreamBitWriter;

public class OutputStreamBitWriterTest extends AbstractByteBasedBitWriterTest {

	@Override
	ByteBasedBitWriter newBitWriter(long size) {
		return new Writer(new ByteArrayOutputStream((int) (size + 7) / 8));
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		return new InputStreamBitReader(new ByteArrayInputStream(getWrittenBytes(writer)));
	}

	@Override
	byte[] getWrittenBytes(BitWriter writer) {
		Writer w = (Writer) writer;
		return w.out.toByteArray();
	}

	private static class Writer extends OutputStreamBitWriter {

		final ByteArrayOutputStream out;

		Writer(ByteArrayOutputStream out) {
			super(out);
			this.out = out;
		}

	}

}

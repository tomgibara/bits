package com.tomgibara.bits.sample;

import static com.tomgibara.streams.Streams.streamInput;
import static com.tomgibara.streams.Streams.streamOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;

import com.tomgibara.bits.AbstractBitStore;
import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStreamException;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.bits.EndOfBitStreamException;
import org.junit.jupiter.api.Test;

public class Examples {

	@Test
	public void testExamples() throws IOException {
		// preamble
		int distance = 0;
		boolean fill = false;
		BigInteger bigInt = BigInteger.ONE;
		byte[] bytes = new byte[] {};
		int[] ints = new int[] {};
		int size = 1;
		BitSet bitSet = new BitSet();
		String string = "";
		Random random = new Random(0L);
		int from = 0;
		int to = 1;
		BitStore store = Bits.store(1000);
		BitStore otherStore = Bits.store(1000);
		BitReader reader = Bits.zeroBits(100000).openReader();
		BitWriter writer = Bits.writerToNothing();
		boolean mutable = true;
		InputStream in = new ByteArrayInputStream(new byte[1000]);
		OutputStream out = new ByteArrayOutputStream();
		File file = File.createTempFile("bits", "example");
		file.deleteOnExit();
		FileChannel channel = new FileInputStream(file).getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(100);

		// Manipulate bits in a bit store
		store.setBit(0, true);
		store.getThenSetBit(1, true);
		store.flip();
		store.clear();
		store.xor().withLong(0, -1L);

		// Compare bit stores
		store.contains().store(otherStore);
		store.excludes().store(otherStore);
		store.compareLexicallyTo(otherStore);
		store.compareNumericallyTo(otherStore);

		// Convert a bit store into a host of common Java types
		store.toBigInteger();               Bits.asStore(bigInt);
		store.toByteArray();                Bits.asStore(bytes);
		store.toBitSet();                   Bits.asStore(bitSet, size);
		store.toString();                   Bits.asStore(string);

		// Apply a range of transformations to a bit store
		store.shift(distance, fill);
		store.permute().reverse();
		store.permute().rotate(distance);
		store.permute().shuffle(random);

		// Create live views of bit stores
		store.range(from, to);
		store.flipped();
		store.reversed();
		store.asList();
		store.asNumber();

		// Control mutability without necessarily copying the underlying bit data
		store.immutableCopy();              store.mutableCopy();
		store.immutable();                  store.mutable();
		store.immutableView();

		// Stream bit data
		store.openReader();                 store.openWriter();
		store.readFrom(reader);             store.writeTo(writer);
		store.readFrom(streamInput(in));    store.writeTo(streamOutput(out));

		// Obtain bit streams from common Java sources
		Bits.readerFrom(bytes);             Bits.writerTo(bytes);
		Bits.readerFrom(ints);              Bits.writerTo(ints);
		Bits.readerFrom(in);                Bits.writerTo(out);
		Bits.readerFrom(channel, buffer);   Bits.writerToNothing();
		Bits.readerFrom(string);            Bits.writerToStdout();

		// Treat sorted integer sets as bit stores and vice-versa
		SortedSet<Integer> set = store.ones().asSet();
		store = Bits.asStore(set, 0, 100, mutable);

		// Create a new bit store implementation
		List<Boolean> list = new ArrayList<Boolean>(50);
		store = new AbstractBitStore() {
			public int size()                            { return list.size();     }
			public boolean getBit(int index)             { return list.get(index); }
			public void setBit(int index, boolean value) { list.set(index, value); }
		};

		// Create a new bit reader implementation
		new BitReader() {
			Iterator<Boolean> i = list.iterator();
			public int readBit() throws BitStreamException {
				if (!i.hasNext()) throw new EndOfBitStreamException();
				return i.next() ? 1 : 0;
			}
		};

		// Create a new bit writer implementation
		new BitWriter() {
			public int writeBit(int bit) throws BitStreamException {
				list.add( (bit & 1) == 1 );
				return 1;
			}
		};

	}

}

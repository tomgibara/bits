/*
 * Copyright 2011 Tom Gibara
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

import java.io.PrintStream;
import java.math.BigInteger;

// not ready to be a public part of the API yet
//TODO implement a companion ProfiledBitWriter
//TODO revaluate name
//TODO record stats on distribution bit counts supplied to methods
class ProfiledBitReader implements BitReader {

	private static final int GP = 0;
	private static final int SP = 1;
	private static final int R = 2;
	private static final int RBI = 3;
	private static final int RB = 4;
	private static final int RZ = 5;
	private static final int RL = 6;
	private static final int RU = 7;
	private static final int SB = 8;
	private static final int STB = 9;
	
	private final BitReader reader;
	private final long[] calls = new long[10];
	
	public ProfiledBitReader(BitReader reader) {
		if (reader == null) throw new IllegalArgumentException("null reader");
		this.reader = reader;
	}

	@Override
	public long getPosition() {
		calls[GP]++;
		return reader.getPosition();
	}

	@Override
	public long setPosition(long newPosition) throws IllegalArgumentException {
		calls[SP]++;
		return reader.setPosition(newPosition);
	}
	
	@Override
	public int read(int count) throws BitStreamException {
		calls[R]++;
		return reader.read(count);
	}

	@Override
	public BigInteger readBigInt(int count) throws BitStreamException {
		calls[RBI]++;
		return reader.readBigInt(count);
	}

	@Override
	public int readBit() throws BitStreamException {
		calls[RB]++;
		return reader.readBit();
	}

	@Override
	public boolean readBoolean() throws BitStreamException {
		calls[RZ]++;
		return reader.readBoolean();
	}

	@Override
	public long readLong(int count) throws BitStreamException {
		calls[RL]++;
		return reader.readLong(count);
	}

	@Override
	public int readUntil(boolean one) {
		calls[RU]++;
		return reader.readUntil(one);
	}
	
	@Override
	public long skipBits(long count) {
		calls[SB]++;
		return reader.skipBits(count);
	}

	@Override
	public int skipToBoundary(BitBoundary boundary) {
		calls[STB]++;
		return reader.skipToBoundary(boundary);
	}

	public void dumpProfile(PrintStream out) {
		dump(out, "getPosition", GP);
		dump(out, "setPosition", SP);
		dump(out, "read", R);
		dump(out, "readBigInt", RBI);
		dump(out, "readBit", RB);
		dump(out, "readBoolean", RZ);
		dump(out, "readLong", RL);
		dump(out, "readUntil", RU);
		dump(out, "skipBits", SB);
		dump(out, "skipToBoundary", STB);
	}
	
	private void dump(PrintStream out, String label, int i) {
		out.print(label);
		out.print(": ");
		out.print(calls[i]);
		out.print("\n");
	}
	
}

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

class CharBitReader implements BitReader {

	private final CharSequence chars;
	private final int initialPos;
	private final int finalPos;
	private int position;

	CharBitReader(CharSequence chars) {
		this(chars, 0, chars.length());
	}

	CharBitReader(CharSequence chars, int initialPos, int finalPos) {
		this.chars = chars;
		this.initialPos = initialPos;
		this.finalPos = finalPos;
		this.position = initialPos;
	}

	@Override
	public int readBit() {
		return readBoolean() ? 1 : 0;
	}

	@Override
	public boolean readBoolean() {
		if (position >= finalPos) throw new EndOfBitStreamException();
		char c = chars.charAt(position);
        return switch (c) {
            case '0' -> { position++; yield false; }
            case '1' -> { position++; yield true;  }
            default -> throw new BitStreamException("Non binary character '" + c + "' found at index " + position);
        };
	}

	@Override
	public long getPosition() {
		return position - initialPos;
	}

	@Override
	public long setPosition(long newPosition) throws BitStreamException, IllegalArgumentException {
		if (newPosition < 0) {
			position = initialPos;
			return 0;
		}
		if (newPosition > Integer.MAX_VALUE) {
			position = finalPos;
			return finalPos - initialPos;
		}
		int newPos = (int) newPosition;
		if (newPos < initialPos) {
			newPos = initialPos;
			return 0;
		}
		if (newPos > finalPos) {
			newPos = finalPos;
			return finalPos - initialPos;
		}
		position = newPos;
		return newPos;
	}

	@Override
	public long skipBits(long count) throws BitStreamException {
		if (count == 0L) return 0L;
		int newPosition;
		if (count < 0) {
			newPosition = count >= Integer.MIN_VALUE ? Math.max(initialPos, (int) (position + count)) : 0;
		} else {
			newPosition = finalPos;
			if (count <= Integer.MAX_VALUE && position + count <= Integer.MAX_VALUE) {
				newPosition = Math.min(newPosition, (int) (position + count));
			}
		}
		int skipped = newPosition - position;
		position = newPosition;
		return skipped;
	}
}

/*
 * Copyright 2007 Tom Gibara
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

/**
 * A {@link BitWriter} that writes bits into an array of integers. Bits are
 * written into the int array starting at index zero. Within each int, the most
 * signficant bits are written first.
 * 
 * @author Tom Gibara
 * 
 */

public class IntArrayBitWriter extends AbstractBitWriter {

    // statics
    
    //hope that this will be inlined...
    private static int frontMask(int i) {
        return i == 0 ? 0 : -1 << (32 - i);
    }

    //hope that this will be inlined...
    private static int backMask(int i) {
        return i == 32 ? 0 : -1 >>> i;
    }

    //hope that this will be inlined...
    private static int preMask(int i) {
        return i == 0 ? 0 : -1 >>> (32 - i);
    }

    // fields
    
    private final int[] ints;
    private final long size;
    private long position = 0;
    private int bufferSize = 0;
    private int bufferBits = 0;
    
    // constructors

    /**
	 * Creates a new {@link BitWriter} which is backed by an int array with
	 * least capacity required to store the specified number of bits.
	 * 
	 * @param size
	 *            the number of bits that can be written, not negative, not
	 *            greater than greatest possible number of bits in an int array
	 * @see #getInts()
	 */
    
   public IntArrayBitWriter(long size) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        long length = (size + 31L) >> 5;
        if (length > Integer.MAX_VALUE) throw new IllegalArgumentException("size exceeds maximum possible array bits");
        ints = new int[(int) length];
        this.size = size;
    }
    
   /**
	 * Creates a new {@link BitWriter} which is backed by the specified int array.
	 * The size of the writer will equal the total number of bits in the array.
	 * 
	 * @param ints
	 *            the ints to which bits will be written, not null
	 * @see #getSize()
	 */

   public IntArrayBitWriter(int[] ints) {
    	if (ints == null) throw new IllegalArgumentException("null ints");
        this.ints = ints;
    	size = ((long) ints.length) << 5;
    }

	/**
	 * Creates a new {@link BitWriter} which is backed by the specified int
	 * array. Bits will be written to the int array up to the specified size.
	 * 
	 * @param ints
	 *            the ints to which bits will be written, not null
	 * @param size
	 *            the number of bits that may be written, not negative and no
	 *            greater than the number of bits available in the array
	 */

    public IntArrayBitWriter(int[] ints, long size) {
    	if (ints == null) throw new IllegalArgumentException("null ints");
        if (size < 0) throw new IllegalArgumentException("negative size");
        long maxSize = ((long) ints.length) << 5;
        if (size > maxSize) throw new IllegalArgumentException("size exceeds maximum permitted by array length");
        this.ints = ints;
        this.size = size;
    }

    // bit writer methods
    
    @Override
    public int write(int bits, int count) {
        if (count == 0) return 0;
        //TODO what is the correct behaviour here?
        //throw EOBSE or return reduced count
        if (position + bufferSize + count > size) throw new BitStreamException("array full");

        if (bufferSize == 0) {
            bufferBits = bits;
            bufferSize = count;
        } else if (bufferSize + count <= 32) {
            bufferBits = (bufferBits << count) | bits & preMask(count);
            bufferSize += count;
        } else {
            flushBuffer();
            bufferBits = bits;
            bufferSize = count;
            
        }
        
        return count;
    }
    
    //optimized implementation
    @Override
    public long writeBooleans(boolean value, long count) {
    	count = Math.min(count, size - position - bufferSize);
        if (count == 0) return 0;

        final int bits = value ? -1 : 0;
        long c = count;
        while (c >= 32) {
            flushBuffer();
            bufferBits = bits;
            bufferSize = 32;
            c -= 32;
        }

        int d = (int) c;
        if (d == 0) {
            /* do nothing */
        } else if (bufferSize == 0) {
            bufferBits = bits;
            bufferSize = d;
        } else if (bufferSize + d <= 32) {
            bufferBits <<= d;
            if (value) bufferBits |= preMask(d);
            bufferSize += d;
        } else {
            flushBuffer();
            bufferBits = bits;
            bufferSize = d;
        }

        return count;
    }
    
    @Override
    public long getPosition() {
        flushBuffer();
        return position;
    }
    
    @Override
    public int flush() {
        flushBuffer();
        return 0;
    }
    
    // accessors

	/**
	 * Changes the position of to which the next bit will be written.
	 * 
	 * @param position
	 *            the position to which the next bit will be written, not
	 *            negative and not exceeding the size
	 */
    
    public void setPosition(long position) {
        if (position < 0) throw new IllegalArgumentException();
        if (position > size) throw new IllegalArgumentException();
        flushBuffer();
        this.position = position;
    }
    
	/**
	 * The maximum number of bits that may be written by this {@link BitWriter}.
	 * 
	 * @return the least position at which there bits cannot be written, never
	 *         negative
	 */
    
    public long getSize() {
        return size;
    }
    
    /**
     * The int array the backs this {@link BitWriter}.
     * 
     * @return the ints written by this {@link BitWriter}, never null
     */
    
    public int[] getInts() {
        return ints;
    }
    
    // object methods
    
    @Override
    public String toString() {
    	int length = (int) Math.min(size, 100);
        StringBuilder sb = new StringBuilder(length);
        IntArrayBitReader reader = new IntArrayBitReader(ints, length);
        for (int i = 0; i < length; i++) {
            sb.append( reader.readBit() == 0 ? "0" : "1" );
        }
        if (length != size) sb.append("...");
        return sb.toString();
    }

    // private utlity methods
    
    private void flushBuffer() {
        if (bufferSize == 0) return;
        doWrite(bufferBits, bufferSize);
        position += bufferSize;
        //bufferBits = 0; we leave the buffer dirty
        bufferSize = 0;
    }
    
    //assumes count is non-zero
    private void doWrite(int bits, int count) {
        
        int frontBits = ((int) position) & 31;
        int firstInt = (int) (position >> 5);
        
        int sumBits = count + frontBits;
        if (sumBits <= 32) {
            int i = ints[firstInt];
            int mask = frontMask(frontBits) | backMask(sumBits);
            i &= mask;
            i |= (bits << (32 - sumBits)) & ~mask;
            ints[firstInt] = i;
        } else {
            int i = ints[firstInt];
            int mask = frontMask(frontBits);
            i &= mask;
            int lostBits = sumBits - 32;
            i |= (bits >> lostBits) & ~mask;
            ints[firstInt] = i;
            
            i = ints[firstInt + 1];
            mask = backMask(lostBits);
            i &= mask;
            i |= (bits << (32 - lostBits)) & ~mask;
            ints[firstInt + 1] = i;
        }
    }
    
}

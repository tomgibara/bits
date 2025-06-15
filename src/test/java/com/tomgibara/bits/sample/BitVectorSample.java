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
package com.tomgibara.bits.sample;

import com.tomgibara.bits.BitReader;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.BitStore.Test;
import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.tomgibara.bits.Operation;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BitVectorSample {

	@org.junit.jupiter.api.Test
	public void testSample() throws Exception {

		// INTRODUCTION

		/**
		 * A BitVector is a fixed-length sequence of bits. It's an extremely
		 * powerful class because of the huge number of ways that it allows the
		 * bits to be accessed and modified. Algorithms that rely heavily on bit
		 * manipulation can improve their performance by reducing the frequency
		 * with which bit data needs to be moved between different data
		 * structures; they can rely on BitVector for all the bit manipulations
		 * they require.
		 */

		{ // SIMPLE BITS

			/**
			 * There are many perspectives from which a BitVector can be viewed.
			 * The simplest is to see it as an indexed sequence of bits with a
			 * fixed size. This creates a BitVector of size 10:
			 */

			BitVector v = new BitVector(10);

			/**
			 * It's fine to have an empty BitVector:
			 */

			BitVector empty = new BitVector(0);

			/**
			 * The size of a bit vector is fixed and accessible.
			 */

			assertEquals(10, v.size());
			assertEquals(0, empty.size());

			/**
			 * Initially all of the bits are zero. The BitVector class
			 * universally represents bits as booleans (true/false) rather than
			 * (0/1) since it naturally provides a much better API. So to set
			 * the first bit of a BitVector we can call:
			 */

			v.setBit(0, true);

			/**
			 * As this makes clear (if it were necessary), the bits are zero
			 * indexed. In addition to being zero indexed, bits are arranged
			 * 'big-endian' for consistency with the Java language. Normally
			 * this is of no concern, since the internal representation of the
			 * bits is rarely a concern when using the class. One place where it
			 * may be seem confusing at first is the toString() method:
			 */

			assertEquals("0000000001", v.toString());

			/**
			 * If thinking of BitVector as a straightforward list one might
			 * expect the first bit to appear on the left. Instead, it's best to
			 * regard the zeroth bit of a BitVector as being the least
			 * significant bit - and in binary representation that appears on
			 * the right.
			 *
			 * It's also possible to construct a BitVector directly from a
			 * string. This is analogous to BigInteger's String constructor.
			 */

			assertEquals(new BitVector("0000000001"), v);

			/**
			 * Here's just one of the basic bit level operations that is
			 * available:
			 */

			v.or().withBit(1, true);
			assertEquals(new BitVector("0000000011"), v);

			/**
			 * In addition to OR, AND and XOR are also available. Any operation
			 * can be applied to a range rather than a single bit:
			 */

			v.range(1, 5).xor().with(true);
			assertEquals(new BitVector("0000011101"), v);

			/**
			 * Observe that, as is the norm in Java, the lower bound is
			 * inclusive and the upper bound is exclusive. Additionally, Any
			 * operation can be applied using the bits from a byte, int or long.
			 * Here we AND the binary representation of 9 ending at the 1st bit:
			 */

			v.and().withByte(1, (byte) 9);
			assertEquals(new BitVector("0000010001"), v);

			/**
			 * This is subtle, but nevertheless powerful: all of the eight bits
			 * spanned by the byte have been ANDed and none of the other bits
			 * are modified. Conversely, all the targeted bits must lay within
			 * the span of the vector. So what if you have a number but you only
			 * want to apply some of its bits? For this, the number of (least
			 * significant bits) that should be modified can specified
			 */

			v.set().withBits(6, 12, 4); // apply 4 bits starting at index 6
			assertEquals(new BitVector("1100010001"), v);

			/**
			 * The NOT operation is also supported, but is referred to as "flip"
			 * for consistency with other classes in the standard Java
			 * libraries.
			 */

			v.flipBit(2);
			assertEquals(new BitVector("1100010101"), v);
			v.flipBit(2);
			assertEquals(new BitVector("1100010001"), v);

			/**
			 * For convenience, it's easy to target every bit in a single
			 * operation. For example, this will clear all bits:
			 */

			v.clear();
			assertEquals(new BitVector("0000000000"), v);

			/**
			 * and this will flip them all:
			 */

			v.flip();
			assertEquals(new BitVector("1111111111"), v);

			/**
			 * For every method that applies a bit operation, there is a
			 * corresponding method which does the same thing, but takes the
			 * operator as an additional parameter. The following calls are
			 * equivalent:
			 */

			v.and().withBit(0, true);
			v.op(Operation.AND).withBit(0, true);

		}

		{ // NUMBERS

			/**
			 * Just as Java's integral primitives are fixed-width bit words, a
			 * BitVector can be regarded as an integral value, but in contrast
			 * to Java, BitVector values are always unsigned. It's easy to
			 * create a BitVector from a number using the string constructor
			 * which takes a radix.
			 */

			String monsterDigits = "808017424794512875886459904961710757005754368000000000";
			BitVector v = new BitVector(monsterDigits, 10);

			/**
			 * BitVectors can also be constructed from BigIntegers.
			 */

			BigInteger monsterInteger = new BigInteger(monsterDigits);
			BitVector w = BitVector.fromBigInteger(monsterInteger);

			assertEquals(v, w);

			/**
			 * BitVectors constructed in this way will always contain the least
			 * number of bits required to contain the number; in other words,
			 * the most significant bit is always 1.
			 */

			assertTrue(v.getBit(v.size() - 1));

			/**
			 * Another consequence is that a BitVector constructed from the
			 * number zero will have a size of zero.
			 */

			assertEquals(0, new BitVector("0", 10).size());
			assertEquals(0, BitVector.fromBigInteger(BigInteger.ZERO).size());

			/**
			 * If a greater number of bits is required, the size can be supplied
			 * as an extra parameter when constructing a BitVector from a
			 * BigInteger:
			 */

			assertEquals(new BitVector("00000001"),
					BitVector.fromBigInteger(BigInteger.ONE, 8));

			/**
			 * Note that, because BitVectors are unsigned, attempting to
			 * construct one from a negative number will populate the BitVector
			 * with its two's complement form.
			 */

			assertEquals(new BitVector("11010"),
					BitVector.fromBigInteger(BigInteger.valueOf(-6L), 5));

			/**
			 * So far we've dealt with converting numbers into BitVectors, of
			 * course, it's possible to do the reverse and convert a BitVector
			 * back into a number:
			 */

			assertEquals(monsterInteger, v.toBigInteger());

			/**
			 * Reflecting BitVector's role in storing numeric values, the class
			 * provides an asNumber() method and any BitVector can be converted
			 * into primitive values via the standard Java Number interface.
			 */

			assertEquals(monsterInteger.longValue(), v.asNumber().longValue());

			assertEquals(234, new BitVector("234", 10).asNumber().intValue());

			/**
			 * As with all other implementations of Number in the core Java
			 * packages, these methods take the least significant bits of the
			 * BitVector and silently truncate any other bits.
			 */

			assertEquals(85, new BitVector("0101010101010101").asNumber().byteValue());

			/**
			 * Note that truncation can result in negative values being returned
			 * from these methods even though the BitVector itself always
			 * represents a positive whole number.
			 */

			assertTrue(monsterInteger.longValue() < 0L);

			/**
			 * It is also possible to extract bits from any position in the
			 * BitVector as a primitive value.
			 */

			assertEquals((byte) (v.asNumber().longValue() >>> 56), v.getByte(56));

		}

		/*
		 * To make the following exposition clearer, we define a method
		 * bitVector() which simply constructs a BitVector from a string.
		 */

		{ // SHIFTS AND ROTATIONS

			/**
			 * Just as Java provides bit shift operators for ints and longs,
			 * BitVector provides the same functionality (and more) but for
			 * potentially much larger bit sequences; one method handles all of
			 * Java's bit shift operators (>>>, >> and <<). It takes two
			 * parameters, the first indicates how far the bits should be
			 * shifted with the sign of the number indicating the direction
			 * (negative is left, positive is right) and the second gives the
			 * value that should used to populate the vacated bits.
			 */

			BitVector v = bitVector("11001010");
			v.shift(1, true);
			assertEquals(bitVector("10010101"), v);
			v.shift(-2, false);
			assertEquals(bitVector("00100101"), v);

			/**
			 * There is no limit to the distance that a BitVector may be
			 * shifted. Obviously any shift distance that exceeds the size of
			 * the BitVector will eradicate all bits.
			 */

			v.shift(8, false);
			assertEquals(bitVector("00000000"), v);

			/**
			 * BitVector provides common bit transformations such as rotations
			 * and reversals via 'permutes' view.
			 */

			bitVector("").permute();

			/**
			 * Similar to bit shifts, BitVector can also rotate bits. Bits that
			 * are pushed off one end populate the vacated positions at the
			 * other. Operations, like rotate, that reorder bits without
			 * changing them are available via BitVector.permute().
			 */

			v = bitVector("11001010");
			v.permute().rotate(1);
			assertEquals(bitVector("10010101"), v);

			/**
			 * As with shifting, there is no limit to the distance over which
			 * bits can be rotated. But unlike shifting, bits are never lost.
			 */

			v = bitVector("11001010");
			v.permute().rotate(v.size());
			assertEquals(bitVector("11001010"), v);

			/**
			 * In addition to shifts and rotations, BitVector can reverse the
			 * order of the bits.
			 */

			v.permute().reverse();
			assertEquals(bitVector("01010011"), v);

			/**
			 * Reversing a BitVector twice will naturally restore the bits to
			 * their original order.
			 */

			v.permute().reverse();
			assertEquals(bitVector("11001010"), v);

			/**
			 * Finally, you should be aware that all of these operations
			 * (shifting, rotating and reversing) are possible over arbitrary
			 * ranges. Here are some examples:
			 */

			v = bitVector("11001010");
			v.range(0, 4).shift(1, false);
			assertEquals(bitVector("11000100"), v);
			v.range(2, 7).permute().rotate(-1);
			assertEquals(bitVector("11100000"), v);
			v.range(2, 8).permute().reverse();
			assertEquals(bitVector("00011100"), v);

		}

		{ // COPIES AND VIEWS

			/**
			 * It is frequently necessary to duplicate BitVectors. There are two
			 * ways of doing this which have very different semantics. One of
			 * them is copying; this forms a new copy of the bits inside the
			 * BitVector that won't be modified by changes to the original.
			 */

			BitVector original = new BitVector(10);
			BitVector copy = original.mutableCopy();
			assertNotSame(original, copy);
			assertEquals(original, copy);
			original.setBit(0, true);
			assertFalse(original.equals(copy));

			/**
			 * On the other hand, it's possible to create a new view over the
			 * bits of the original BitVector. This is commonly done by creating
			 * a 'ranged' view. This is similar to sublists in Java's
			 * collections API; mutations of the sublist modify the list from
			 * which they were taken, and vice versa. But there are many other
			 * methods for creating views and copies.
			 */

			original = new BitVector("0011100000");
			BitVector view = original.range(5, 8);
			assertEquals(3, view.size());
			assertTrue(view.ones().isAll());

			/**
			 * The range is specified by supplying the index of the first bit in
			 * the range, and the first beyond the range. Zero length ranges
			 * are permitted.
			 */

			original.range(3, 3);

			/**
			 * The range is specified by supplying the index of the first bit in
			 * the range, and the first beyond the range. Ranged views are very
			 * useful for limiting the scope of operations over specific bits.
			 */

			view.flip();
			assertTrue(original.zeros().isAll());

			/**
			 * Ranged views can also be used to adjust the indexing of bits,
			 * since the bits of a view are always indexed from zero. This can
			 * simplify the implementation of some algorithms.
			 */

			view.setBit(0, true);
			assertTrue(original.getBit(5));

			/**
			 * There are also convenient methods for obtaining ranges that
			 * start at the beginning or finish at the end of a BitVector.
			 * Using these methods it's easy to decompose a BitVector into
			 * two shorter BitVectors.
			 */

			BitVector left  = original.rangeFrom(5);
			BitVector right = original.rangeTo(5);
			assertEquals(original.size(), left.size() + right.size());

			/**
			 * Views and copies can also control mutability. When first
			 * constructed, all BitVectors are mutable, this means that the bits
			 * they contain can be changed.
			 */

			assertTrue(original.isMutable());

			/**
			 * But in many situations it's important to prevent code from
			 * accidentally modifying a BitVector. The simplest way to do this
			 * is to create an immutable copy.
			 */

			copy = original.immutableCopy();
			try {
				copy.permute().reverse();
				// the method won't complete normally ...
				fail();
			} catch (IllegalStateException e) {
				// ... the copy cannot be modified
			}

			/**
			 * But this may not be the most efficient way, depending on the
			 * application. The immutableCopy() will always create a copy
			 * even if the original is itself immutable.
			 */

			assertNotSame(copy, copy.immutableCopy());

			/**
			 * This is often unnecessary, so an immutable() method is also
			 * available which only creates a copy if the original is not
			 * already immutable.
			 */

			assertNotSame(original, original.immutable());
			assertSame(copy, copy.immutable());

			/**
			 * The corresponding methods mutableCopy() and mutable() go the
			 * other way. They can take an immutable BitVector and make one that
			 * is mutable. The difference between the two is again that the
			 * latter method will avoid creating a copy if the BitVector is
			 * already mutable.
			 */

			assertSame(original, original.mutable());
			assertNotSame(copy, copy.mutable());
			assertNotSame(original, original.mutableCopy());

			/**
			 * The problem with creating all of these copies is that all the bit
			 * data is repeatedly duplicated. This takes may require significant
			 * amounts of memory and processor time for large BitVectors.
			 *
			 * Often a copy is not required, and all that is needed instead is
			 * an immutable view. Immutable views can be safely shared between
			 * methods (the methods cannot modify the bits) but changes to the
			 * original BitVector (the one over which the view was taken) WILL
			 * be visible.
			 *
			 * Immutable views are much more efficient than immutable copies for
			 * large BitVectors. Whether it is necessary to create a copy, or a
			 * view suffices depends on the application.
			 */

			original.clear();
			view = original.immutableView();
			assertTrue(view.zeros().isAll());
			try {
				view.flip();
				// the method won't complete normally ...
				fail();
			} catch (IllegalStateException e) {
				// ... the view cannot be modified directly ...
			}
			// ... but it can be modified via the original
			original.flip();
			assertTrue(view.ones().isAll());

			/**
			 * Returning to the simple range() method described earlier. It's
			 * important to know that this method preserves the mutability of
			 * the original; a new BitVector created with this method will be
			 * mutable if and only if the original is mutable.
			 */

			BitVector mutable = original.mutableCopy();
			assertTrue(mutable.range(0,1).isMutable());

			BitVector immutable = original.immutableCopy();
			assertFalse(immutable.range(0,1).isMutable());

			/**
			 * Finally, it's worth noting that the BitVector class implements
			 * Cloneable. The clone() method is logically equivalent to calling
			 * view() but may be slightly more efficient, though perhaps less
			 * explicit.
			 */

			view = original.clone();
		}

		/*
		 * Again, for clarity we introduce a new method bitVector() that just
		 * constructs a BitVector from a BigInteger.
		 */

		{ // COMPARISONS AND TESTS

			/**
			 * The BitVector class implements the Comparable interface to allow
			 * different instances to be ordered in relation to each other.
			 * Comparison is consistent with the BigInteger value of the
			 * BitVector.
			 */

			BigInteger a = BigInteger.valueOf(86400000);
			BigInteger b = BigInteger.valueOf(16777216);
			BigInteger c = BigInteger.valueOf(10000000);

			assertTrue(bitVector(a).compareTo(bitVector(b)) > 0);
			assertTrue(bitVector(b).compareTo(bitVector(b)) == 0);
			assertTrue(bitVector(c).compareTo(bitVector(b)) < 0);

			/**
			 * Under this comparison equal BitVectors will always compare to
			 * zero, but the converse is not necessarily true since the
			 * shorter BitVector is implicitly padded with leading zeros.
			 */

			assertTrue(bitVector("00110").compareTo(bitVector("00110")) == 0);
			assertTrue(bitVector( "0110").compareTo(bitVector("00110")) == 0);
			assertTrue(bitVector(  "110").compareTo(bitVector("00110")) == 0);

			/**
			 * The same ordering is provided by a statically defined comparator
			 * which may be useful if the ordering needs to be adapted.
			 */

			Bits.numericalComparator().compare(bitVector("100"), bitVector("001"));

			/**
			 * For BitVectors that are the same size, this numerical ordering is
			 * equivalent to a lexical ordering of the bits.
			 */

			String s = "00110";
			String t = "01011";
			String u = "01101";

			assertEquals(
					Integer.signum(t.compareTo(s)),
					Integer.signum(bitVector(t).compareTo(bitVector(s)))
					);

			assertEquals(
					Integer.signum(t.compareTo(t)),
					Integer.signum(bitVector(t).compareTo(bitVector(t)))
					);

			assertEquals(
					Integer.signum(t.compareTo(u)),
					Integer.signum(bitVector(t).compareTo(bitVector(u)))
					);

			/**
			 * If a real lexical ordering is required, a second statically
			 * defined comparator is available.
			 */

			Bits.lexicalComparator().compare(bitVector("100"), bitVector("11"));

			/**
			 * In addition to the orderings provided by these comparators, the
			 * BitVector class supports a second notion of comparison that is
			 * related to operations on sets.
			 *
			 * The AND operation of one bit vector on another can be viewed as
			 * finding the intersection of the two vectors, ie. identifying all
			 * of the bits which are set in both vectors.
			 *
			 * Sometimes, it can be useful to know whether two bit vectors
			 * intersect without going so far as to compute the intersection.
			 * This can be as follows, by testing whether the two bit vectors
			 * are exclusive (the opposite of intersecting):
			 */

			BitVector v = new BitVector("01110");
			BitVector w = new BitVector("01010");
			BitVector x = new BitVector("10101");

			assertFalse(v.excludes().store(x));
			assertTrue(w.excludes().store(x));

			/**
			 * It is also possible to test whether one bit vector contains all
			 * the bits set in a second bit vector:
			 */

			assertTrue(v.contains().store(w));
			assertFalse(v.contains().store(x));

			/**
			 * Or to test whether one vector is complementary to another.
			 */

			assertTrue(w.complements().store(x));
			assertFalse(w.complements().store(v));

			/**
			 * Finally, it is possible to test that one bit vector contains
			 * exactly those bits set in a second bit vector, ie. that the
			 * two vectors have the same pattern of bits.
			 */

			assertFalse(v.equals().store(w));
			assertTrue(v.equals().store(v));

			/**
			 * Each test (EXCLUDES, CONTAINS, EQUALS and COMPLEMENT) can be
			 * performed using a generic method. The following pairs of tests
			 * are equivalent:
			 */

			assertEquals(
					v.test(Test.EXCLUDES).store(x),
					v.excludes().store(x)
					);

			assertEquals(
					v.test(Test.CONTAINS).store(w),
					v.contains().store(w)
					);

			assertEquals(
					w.test(Test.COMPLEMENTS).store(x),
					w.complements().store(x)
					);

			assertEquals(
					v.test(Test.EQUALS).store(w),
					v.equals().store(w)
					);

			/**
			 * Note that tests can only be performed between bit vectors of the
			 * same length.
			 *
			 * Using these test classes, short BitVectors (those no longer than
			 * 64 bits) can be conveniently compared against the least
			 * significant bits of a long value.
			 */

			assertTrue(v.equals().bits(0b11101110));
		}

		{ // I/O

			/**
			 * The BitVector class provides several methods for streaming data.
			 * One set of methods use the BitReader and BitWriter classes to
			 * stream bit-level data.
			 */

			BitVector v = new BitVector(10);

			/**
			 * Opening a writer is very simple...
			 */

			BitWriter w = v.openWriter();

			/**
			 * ... and it provides a variety of standardized methods for writing
			 * bits to the BitVector. For example, the code below will write a
			 * single 1 bit, skip 4 bits and write another five 1 bits:
			 */

			w.writeBoolean(true);     // writes a single 1 bit
			w.skipBits(4);            // skips 4 bits
			w.writeBooleans(true, 5); // writes 5 one bits
			w.flush();

			/**
			 * Importantly, bits are written to BitVector starting with at the
			 * most significant bit. So the state of the vector after these
			 * calls is:
			 */

			assertEquals(bitVector("1000011111"), v);

			/**
			 * Readers and writers obtained from a BitVector fully support
			 * positioning via the getPosition() and setPosition() methods.
			 */

			assertEquals(10, w.getPosition());
			w.setPosition(5);
			w.write(1, 5); // writes 5 LSBs of the integer 1
			w.flush();

			assertEquals(bitVector("1000000001"), v);

			/**
			 * A BitReader provides a symmetrical interface...
			 */

			BitReader r = v.openReader();

			/**
			 * As with the writer, the reader begins positioned at the most
			 * significant bit of the BitVector.
			 */

			r.readBit();       // reads the first bit
			r.readUntil(true); // reads any number of zeros from the stream
			                   // until a single one bit is read

			assertEquals(10, r.getPosition());

			/**
			 * There are also methods for opening readers over a sub-range.
			 * In these method signatures, the first parameter is the index of
			 * the last bit accessed and the second parameter is the position
			 * ahead of the first bit accessed. This parameter order is chosen
			 * for consistency with the range() method which provides an
			 * equivalent (though possibly less efficient) way of creating
			 * I/O over sub-ranges.
			 */

			v.openReader(2, 8); // equivalent to ...
			v.range(2, 8).openReader();
			v.openWriter(5, 10);
			v.rangeFrom(5).openReader();

			/**
			 * BitVector also provides methods for reading and writing
			 * byte-level data using primitives from the com.tomgibara.streams
			 * library.
			 */

			StreamBytes bytes = Streams.bytes();
			try (WriteStream out = bytes.writeStream()) {
				v.writeTo(out);
			}
			// v has size 10, so this the data consists of two bytes
			assertEquals(2, bytes.bytes().length);

			/**
			 * In addition to writing BitVectors to byte streams, it is also
			 * possible to write them to bit streams.
			 */

			BitVector u = new BitVector("10100");
			w.setPosition(0);
			u.writeTo(w);
			u.writeTo(w);
			w.flush();
			assertEquals(bitVector("1010010100"), v);

			/**
			 * Counterparts for reading from bit/byte streams are naturally also
			 * available.
			 */

			v.readFrom(bytes.readStream());
			// the value has been restored from the byte array
			assertEquals(bitVector("1000000001"), v);

			BitVector s = bitVector("1101010110");
			v.readFrom(s.openReader());
			assertEquals(s, v);
		}

		{ // MATCHING

			/**
			 * BitVector provides a number of methods for matching bit
			 * sequences. Here are some examples:
			 */

			BitVector v = new BitVector("1001101110");

			/**
			 * Count how many one bits are present.
			 */

			assertEquals(6, v.ones().count());


			/**
			 * The index of the first one bit.
			 */

			assertEquals(1, v.ones().first());

			/**
			 * The index of the last one bit.
			 */

			assertEquals(9, v.ones().last());

			/**
			 * The index of the next one bit at position 4...
			 */

			assertEquals(5, v.ones().next(4));

			/**
			 * ... and the next at position 5.
			 */

			assertEquals(5, v.ones().next(5));

			/**
			 * You can also move backwards, looking for the previous match.
			 */

			assertEquals(3, v.ones().previous(5));

			/**
			 * Check if all bits are ones.
			 */

			assertFalse(v.ones().isAll());

			/**
			 * Check if none of the bits are ones.
			 */

			assertFalse(v.ones().isNone());

			/**
			 * Restrict matching to a subrange.
			 */

			assertTrue(v.ones().range(1, 4).isAll());

			/**
			 * Arbitrary subsequences longer than a single bit can be also be
			 * matched.
			 */

			BitVector u = bitVector("11");
			assertEquals(1, v.match(u).first());

			/**
			 * The positions of any matches can be returned in the form of
			 * enriched list iterator. Matches may be 'overlapping'
			 * or 'disjoint' and this changes the positions that are returned.
			 */

			Positions ps = v.match(u).disjoint().positions();
			while (ps.hasNext()) {
				ps.next();
				ps.replace(false);
			}
			assertEquals(bitVector("1000001000"), v);

			/**
			 * Individual bit matches can also be returned as a sorted set,
			 * providing a mutable view over the BitVector.
			 */

			SortedSet<Integer> s = v.ones().asSet();
			assertEquals(2, s.size());
			v.flip();
			assertEquals(8, s.size());
			s.clear();
			assertTrue(v.zeros().isAll());
		}

		{ // JAVA OBJECT METHODS

			/**
			 * BitVector supports all the standard Java object methods.
			 * Equality is predicated on the BitVector's size and bit values
			 * and is consistent across all BitStore implementations, so:
			 */

			assertEquals(Bits.zeroBits(5), bitVector("00000"));

			/**
			 * Hashcodes are consistent with equals, and also standardized, as
			 * specified in the BitStore interface documentation.
			 */

			assertEquals(Bits.zeroBits(5).hashCode(), bitVector("00000").hashCode());

			/**
			 * The toString() method returns the BitVector bits.
			 */

			BitVector v = bitVector("11000");
			assertEquals("11000", v.toString());

			/**
			 * In a direct analogue to BigInteger and other java number types,
			 * an extended toString method is available which takes a radix in
			 * the range [2,36].
			 */

			assertEquals("220", v.toString(3));

			/**
			 * BitVector is cloneable, this is a shallow copy that is mutable
			 * if and only if the original is mutable.
			 */

			BitVector u = v.clone();
			u.clear();
			assertTrue(v.zeros().isAll());

			/**
			 * Finally, BitVector supports serialization. Serialization is
			 * guaranteed to be forward compatible (ie. serializations of
			 * BitVector will always be deserializable by newer versions of the
			 * library).
			 */

		}

		{ // BYTES AND MORE

			/**
			 * Although BitVector provides a large number of methods for
			 * importing and exporting its bits via streams, sometimes it is
			 * more practical to work with byte arrays directly.
			 */

			 /**
			 * A BitVector can be initialized from a byte array.
			 */

			byte[] bytes = {11, 87, 92};
			BitVector v = BitVector.fromByteArray(bytes, 20);
			assertEquals(bitVector("10110101011101011100"), v);

			/**
			 * A BitVector can also be converted to a byte array.
			 */

			assertTrue(Arrays.equals(bytes, v.toByteArray()));

			/**
			 * The byte array is consistent with that of the BitVector's
			 * BigInteger equivalent.
			 */

			assertTrue(Arrays.equals(bytes, v.toBigInteger().toByteArray()));

			/**
			 * In addition to exporting bit data as a byte array, it is possible
			 * to export the bit data of a BitVector as a BitSet.
			 */

			v.toBitSet();

			/**
			 * The BitVector can also be exposed as a fixed-length boolean list.
			 * The list is a live view of the BitVector that supports mutations
			 * that don't change the size of the list.
			 */

			v.asList();
		}

	}

	private static BitVector bitVector(String str) {
		return new BitVector(str);
	}

	private static BitVector bitVector(BigInteger bigInt) {
		return BitVector.fromBigInteger(bigInt);
	}

}
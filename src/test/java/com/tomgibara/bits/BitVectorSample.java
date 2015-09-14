package com.tomgibara.bits;

import java.math.BigInteger;

import junit.framework.TestCase;

import com.tomgibara.bits.BitStore.Operation;

public class BitVectorSample extends TestCase {

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

			v.clearWithZeros();
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
			 * Note that, because BitVectors are unsigned, any attempt to
			 * construct one from a negative number will fail. So far we've
			 * dealt with converting numbers into BitVectors, of course, it's
			 * possible to do the reverse and convert a BitVector back into a
			 * number:
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
			 * potentially much larger bit sequences. BitVector provides a
			 * single method that handles all of Java's bit shift operators
			 * (>>>, >> and <<). It takes two parameters, the first indicates
			 * how far the bits should be shifted with the sign of the number
			 * indicating the direction (negative is left, positive is right)
			 * and the second gives the value that should used to populate the
			 * vacated bits.
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
			 * Similar to bit shifts, BitVector can also rotate bits. Bits that
			 * are pushed off one end populate the vacated positions at the
			 * other.
			 */

			v = bitVector("11001010");
			v.rotate(1);
			assertEquals(bitVector("10010101"), v);

			/**
			 * As with shifting, there is no limit to the distance over which
			 * bits can be rotated. But unlike shifting, bits are never lost.
			 */

			v = bitVector("11001010");
			v.rotate(v.size());
			assertEquals(bitVector("11001010"), v);

			/**
			 * In addition to shifts and rotations, BitVector can reverse the
			 * order of the bits.
			 */

			v.reverse();
			assertEquals(bitVector("01010011"), v);

			/**
			 * Reversing a BitVector twice will naturally restore the bits to
			 * their original order.
			 */

			v.reverse();
			assertEquals(bitVector("11001010"), v);

			/**
			 * Finally, you should be aware that all of these operations
			 * (shifting, rotating and reversing) are possible over arbitrary
			 * ranges. Here are some examples:
			 */

			v = bitVector("11001010");
			v.range(0, 4).shift(1, false);
			assertEquals(bitVector("11000100"), v);
			v.range(2, 7).rotate(-1);
			assertEquals(bitVector("11100000"), v);
			v.range(2, 8).reverse();
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
			BitVector copy = original.copy();
			assertNotSame(original, copy);
			assertEquals(original, copy);
			original.setBit(0, true);
			assertFalse(original.equals(copy));

			/**
			 * On the other hand, it's possible to create a new view over the
			 * bits of the original BitVector. In this case changes to either
			 * one will be reflected in the other.
			 */

			BitVector view = original.view();
			assertNotSame(original, view);
			assertEquals(original, view);
			original.setBit(0, false);
			assertEquals(original, view);

			/**
			 * If the creation of views was limited to this one method, they
			 * wouldn't be very useful. But there are many other methods for
			 * creating views and copies, all of which can be applied to a
			 * subrange of the original BitVector.
			 */

			original = new BitVector("0011100000");
			copy = original.range(5, 8).copy();
			assertEquals(3, copy.size());
			assertTrue(copy.isAllOnes());

			/**
			 * Ranged views are very useful for limiting the scope of operations
			 * over specific bits.
			 */

			view = original.range(5, 8);
			view.flip();
			assertTrue(original.isAllZeros());

			/**
			 * Ranged views can also be used to adjust the indexing of bits,
			 * since the bits of a view are always indexed from zero. This can
			 * simplify the implementation of some algorithms at the expense of
			 * creating intermediate view objects.
			 */

			view.setBit(0, true);
			assertTrue(original.getBit(5));

			/**
			 * In addition to adjusting ranges, views and copies can also
			 * control mutability. When first constructed, all BitVectors are
			 * mutable, this means that the bits they contain can be changed.
			 */

			assertTrue(original.isMutable());

			/**
			 * But in many situations it's important to prevent code from
			 * accidentally modifying a BitVector. The simplest way to do this
			 * is to create an immutable copy.
			 */

			copy = original.immutableCopy();
			try {
				copy.reverse();
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

			original.clearWithZeros();
			view = original.immutableView();
			assertTrue(view.isAllZeros());
			try {
				view.flip();
				// the method won't complete normally ...
				fail();
			} catch (IllegalStateException e) {
				// ... the view cannot be modified directly ...
			}
			// ... but it can be modified via the original
			original.flip();
			assertTrue(view.isAllOnes());

			/**
			 * Returning to the simple view() and copy() methods described
			 * earlier. It's important to know that both of these methods
			 * preserve the mutability of the original. A new BitVector created
			 * with these methods will be mutable if and only if the original is
			 * mutable.
			 */

			BitVector mutable = original.mutableCopy();
			assertTrue(mutable.copy().isMutable());
			assertTrue(mutable.view().isMutable());

			BitVector immutable = original.immutableCopy();
			assertFalse(immutable.copy().isMutable());
			assertFalse(immutable.view().isMutable());

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

			BitVector.sNumericComparator.compare(bitVector("100"), bitVector("001"));

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

			BitVector.sLexicalComparator.compare(bitVector("100"), bitVector("11"));

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
			 * This can be done as follows:
			 */

			BitVector v = new BitVector("01110");
			BitVector w = new BitVector("01010");
			BitVector x = new BitVector("10101");

			assertTrue(v.testIntersects(x));
			assertFalse(w.testIntersects(x));

			/**
			 * It is also possible to test whether one bit vector contains all
			 * the bits set in a second bit vector:
			 */

			assertTrue(v.testContains(w));
			assertFalse(v.testContains(x));

			/**
			 * Or to test whether one vector is complementary to another.
			 */

			assertTrue(w.testComplements(x));
			assertFalse(w.testComplements(v));

			/**
			 * Finally, it is possible to test that one bit vector contains
			 * exactly those bits set in a second bit vector, ie. that the
			 * two vectors have the same pattern of bits.
			 */

			assertFalse(v.testEquals(w));
			assertTrue(v.testEquals(v));

			/**
			 * Each test (INTERSECTS, CONTAINS, EQUALS and COMPLEMENT) can be
			 * performed using a dedicated class. The following pairs of tests
			 * are equivalent:
			 */

			assertEquals(
					v.testIntersects(x),
					v.intersects().vector(x)
					);

			assertEquals(
					v.testIntersects(w),
					v.contains().vector(w)
					);

			assertEquals(
					w.testComplements(x),
					w.complements().vector(x)
					);

			assertEquals(
					v.testEquals(w),
					v.equals().vector(w)
					);

			/**
			 * Note that tests can only be performed between bit vectors of the
			 * same length.
			 *
			 * Using these test classes, short BitVectors (those no longer than 64 bits)
			 * can be conveniently compared against the least significant bits of a long value.
			 */

			assertTrue(v.equals().bits(0b11101110));
		}

		// TODO

		{ // ANALYZING
			// first, last, count, all, next
		}

		{ // BYTES
			// byte static factory method
			// operations on byte arrays
			// write/read methods
		}

		{ // COLLECTIONS
			// iterator, listIterator, positionIterator
			// asList
			// asSet
		}

		{ // JAVA OBJECT
			// cloneable
			// serializable
			// equals, hashcode
		}

		{ // EFFICIENCY
			// alignment
			// avoiding no-ops
			// getThen... methods
		}
	}

	private static BitVector bitVector(String str) {
		return new BitVector(str);
	}

	private static BitVector bitVector(BigInteger bigInt) {
		return BitVector.fromBigInteger(bigInt);
	}

}
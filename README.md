Bits
====

Bits is a Java library for storing, streaming and manipulating bit level data.

Overview
--------

Full documentation is available via the javadocs packaged with the release,
but this is an overview of some the of the key abstractions. All the classes
named below are found in the `com.tomgibara.bits` package:

* `BitStore` is provides a comprehensive basis for bit storage and bit
   manipulation. The general concept behind this interface is that it exposes a
   *large* number of methods for all sorts of bit related operations to avoid
   the inefficiency of moving bits from one specialized object to another (the
   overhead of which adds up quickly when dealing with bit-level operations). At
   the same time, default method implementations provide everything but the most
   basic functions, making it easy to map the interface onto any binary data
   store.
* `BitVector` is one of many specific `BitStore` implementations provided by
   the package. It is intended to serve as a canonical implementation for
   situations where bit operations must be performed with a trusted
   implementation.
* `BitWriter` is an interface that provides a clean API for writing streams of
   bits. It includes methods for reporting stream position and padding to
   boundaries. Default method implementations reduces the burden of implementing
   the interface to just one method.
* `BitReader` is the bit reading counterpart to `BitWriter`, allowing bits to
   be read from some source. It includes methods for skipping bits (an important
   performance consideration in many contexts) as well as reporting stream
   position. Again, default methods simplify implementation to just one method.
*  `GrowableBits` can accumulate bits in a `BitVector` via a `BitWriter`
   when the number of bits is not known ahead of time.
* `BitStreamException` is an unchecked exception which is thrown as standard by
   all IO methods on `BitReader` and `BitWriter`. Its subtype
   `EndOfBitStreamException` can be used to distinguish IO failures from
   end-of-stream conditions.

Significantly, all `BitStore` implementations provide `openReader()` and
`openWriter()` methods allowing them to serve as general-purpose bit streaming
buffers. Additionally, several general purpose reader/writer implementations are
available via the package entry point, the `com.tomgibara.bits.Bits` class
including implementations that wrap `byte` array, `int` array, `ByteBuffer`
`InputStream` and `ReadStream` and `CharSequence` among others.

Additionally, a large number of dedicated `BitStore` implementations are also
provided for convenient interoperability with established Java types including
`byte` array, `boolean` array, `BigInteger`, `BitSet`, `CharSequence` and
primitive `long` packed bits.

Usage
-----

The bits library is available from the Maven central repository:

> Group ID:    `com.tomgibara.bits`
> Artifact ID: `bits`
> Version:     `2.0.0`

The Maven dependency being:

    <dependency>
      <groupId>com.tomgibara.bits</groupId>
      <artifactId>bits</artifactId>
      <version>2.0.0</version>
    </dependency>

Release History
---------------

**2016.01.21** Version 2.0.0

 * Vast enlargement and reorganization of the API.
 * Use of new Java 8 language features.
 * Transferred BitVector functionality to a base interface `BitStore`.
 * Rationalized `BitStore` into a number of separate interfaces.
 * Extended range of functions provided by `BitStore`.
 * Provided many new `BitStore` implementations.
 * Introduced dependency on `streams` project for abstracting byte streams.
 * Added `GrowableBits` (modelled loosely on `StreamBytes`).
 * Completed documentation of every public API element. 

**2015.05.25** Version 1.0.1

 * Added new growable bit writer BitVectorWriter
 * Added toIntArray() and toLongArray() to BitVector

**2014.11.01** Version 1.0.0

Initial release

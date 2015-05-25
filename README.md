Bits
====

Bits is a Java library for storing, streaming and manipulating bit level data.

Overview
--------

Full documentation is available via the javadocs packaged with the release,
but this is an overview of some the of the key abstractions. All the classes
named below are found in the `com.tomgibara.bits` package:

* `BitVector` is a Swiss Army knife for bit storage and bit manipulation.
   I have a [some posts about the class and its design]
   (http://blog.tomgibara.com/search/BitVector).
   The general concept behind this class is that it exposes a *large* number of
   methods for all sorts of bit related operations to avoid the inefficiency of
   moving bits from one specialized object to another (the overhead of which
   adds up quickly when dealing with bit-level operations).
* `BitWriter` is an interface that provides a clean API for writing streams of
   bits. It includes methods for reporting stream position and padding to
   boundaries. An `AbstractBitWriter` class reduces the burden of implementing
   the interface to just one method.
* `BitReader` is the bit reading counterpart to `BitWriter`, allowing bits to be
   read from some source. It includes methods for skipping bits (an important
   performance consideration in many contexts) as well as reporting stream
   position. Again, an `AbstractBitReader` class simplifies implementation
   to just one method.
* `BitStreamException` is an unchecked exception which is thrown as standard by
   all IO methods on `BitReader` and `BitWriter`. Its subtype
   `EndOfBitStreamException` can be used to distinguish IO failures from
   end-of-stream conditions.

Significantly, the `BitVector` class provides `openReader()` and `openWriter()`
methods allowing it to serve as a general-purpose bit streaming buffer.
Additionally, several general purpose reader/writer implementations are
available:

* `ByteArrayBitReader`/ `ByteArrayBitWriter`
* `InputStreamBitReader` / `OutputStreamBitWriter`

Together with some more specialized implementations:

* `FileChannelBitReader` - provides good read & seek performance
* `IntArrayBitReader` - useful in the common case where bit data is already
   available as in an array of `int`.

Usage
-----

The bits library is available from the Maven central repository:

> Group ID:    `com.tomgibara.bits`
> Artifact ID: `bits`
> Version:     `1.0.1`

The Maven dependency being:

    <dependency>
      <groupId>com.tomgibara.bits</groupId>
      <artifactId>bits</artifactId>
      <version>1.0.1</version>
    </dependency>

Release History
---------------

**2015.05.25** Version 1.0.1

 * Added new growable bit writer BitVectorWriter
 * Added toIntArray() and toLongArray() to BitVector

**2014.11.01** Version 1.0.0

Initial release

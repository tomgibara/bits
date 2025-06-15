package com.tomgibara.bits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BitReaderTest {

  /*
   * Tests for 'readByte()'.
   */

  @Test
  public void testReadByteWithTwoBytes() {

    byte byte1 = (byte) 0b00000000;
    byte byte2 = (byte) 0b11111111;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2});

    assertEquals(byte1, bitReader.readByte());
    assertEquals(byte2, bitReader.readByte());

  }

  @Test
  public void testReadByteWithIntermixedBitReads() {

    byte byte1 = (byte) 0b00001111;
    byte byte2 = (byte) 0b11110000;
    byte byte3 = (byte) 0b00000011;
    byte byte4 = (byte) 0b00000111;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2, byte3, byte4});

    assertFalse(bitReader.readBoolean());
    assertFalse(bitReader.readBoolean());
    assertFalse(bitReader.readBoolean());
    assertFalse(bitReader.readBoolean());

    assertEquals((byte) 0b11111111, bitReader.readByte());
    assertEquals((byte) 0b00000000, bitReader.readByte());

    assertFalse(bitReader.readBoolean());
    assertFalse(bitReader.readBoolean());
    assertTrue(bitReader.readBoolean());
    assertTrue(bitReader.readBoolean());

    assertArrayEquals(new byte[]{byte4}, bitReader.readBytes(1));

  }

  @Test
  public void testReadByteWithoutEnoughBitsRemaining(){

    byte byte1 = (byte) 0b00000000;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1});

    bitReader.readBit();

    try{
      bitReader.readByte();
      fail();
    } catch (EndOfBitStreamException e){
      /* expected */
    }

  }


  /*
   * Tests for 'readBytes()'.
   */

  @Test
  public void testReadBytes() {

    byte byte1 = (byte) 0b00000000;
    byte byte2 = (byte) 0b00000001;
    byte byte3 = (byte) 0b00000011;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2, byte3});

    assertArrayEquals(new byte[]{byte1, byte2, byte3}, bitReader.readBytes(3));

  }

  @Test
  public void testReadByteAndReadyByte() {

    byte byte1 = (byte) 0b00000000;
    byte byte2 = (byte) 0b00000001;
    byte byte3 = (byte) 0b00000011;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2, byte3});

    assertArrayEquals(new byte[]{byte1, byte2}, bitReader.readBytes(2));
    assertEquals(byte3, bitReader.readByte());

  }

  @Test
  public void testReadBytesWithIntermixedBitReads() {

    byte byte1 = (byte) 0b10000000;
    byte byte2 = (byte) 0b00000001;
    byte byte3 = (byte) 0b00000011;
    byte byte4 = (byte) 0b00000111;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2, byte3, byte4});

    assertTrue(bitReader.readBoolean());
    assertFalse(bitReader.readBoolean());
    bitReader.skipBits(6);
    assertArrayEquals(new byte[]{byte2, byte3}, bitReader.readBytes(2));
    assertEquals(byte4, bitReader.readByte());

  }

  @Test
  public void testReadBytesWithoutEnoughBitsRemaining(){

    byte byte1 = (byte) 0b00000000;
    byte byte2 = (byte) 0b00000001;

    BitReader bitReader = Bits.readerFrom(new byte[]{byte1, byte2});

    bitReader.readBit();

    try{
      bitReader.readBytes(2);
      fail();
    } catch (EndOfBitStreamException e){
      /* expected */
    }

  }

}

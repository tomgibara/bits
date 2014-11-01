/*
 * Copyright 2012 Tom Gibara
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Provides a convenient way of opening and closing {@link BitReader}s over a
 * file. A {@link Mode} supplied to the constructor controls the characteristics
 * of returned readers.
 * 
 * @author Tom Gibara
 * 
 */

public class FileBitReaderFactory {

	/**
	 * The default size of byte buffer used to read files.
	 */
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	/**
	 * Specifies the method by which bits are read from the underlying file.
	 */
	
	public enum Mode {
		
		/**
		 * The entire file is read into memory and readers over the in-memory
		 * copy are created. This is the fastest mode of access if the file will
		 * be read multiple times, but may incur a significant memory overhead.
		 */
		
		MEMORY,
		
		/**
		 * A channel to the file is obtained for each reader and bits are read
		 * from the channel.
		 */
		
		CHANNEL,
		
		/**
		 * An input stream over the file is obtained for each reader and bits
		 * are read from the input stream. This mode does not support moving the
		 * read position backwards through the file.
		 */
		
		STREAM
	}

	private final Mode mode;
	private final File file;
	private final int bufferSize;
	private byte[] bytes = null;

	/**
	 * Constructs a new {@link FileBitReaderFactory} using the default buffer
	 * size specified by {@link #DEFAULT_BUFFER_SIZE}.
	 * 
	 * @param file
	 *            the file from which bits are to be read
	 * @param mode
	 *            the method by which bits are obtained from the file
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	
	public FileBitReaderFactory(File file, Mode mode) throws IllegalArgumentException {
		this(file, mode, DEFAULT_BUFFER_SIZE);
	}
	
	/**
	 * Constructs a new {@link FileBitReaderFactory} using the specified buffer
	 * size. When the mode is {@link Mode#MEMORY}, the bufferSize is ignored.
	 * 
	 * @param file
	 *            the file from which bits are to be read
	 * @param mode
	 *            the method by which bits are obtained from the file
	 * @param bufferSize
	 *            the size of the buffer
	 * @throws IllegalArgumentException
	 *             if file is null, the mode is null, or the bufferSize is not
	 *             positive
	 */

	public FileBitReaderFactory(File file, Mode mode, int bufferSize) throws IllegalArgumentException {
		if (file == null) throw new IllegalArgumentException("null file");
		if (mode == null) throw new IllegalArgumentException("null mode");
		if (bufferSize < 1) throw new IllegalArgumentException("non-positive bufferSize");
		this.file = file;
		this.mode = mode;
		this.bufferSize = bufferSize;
	}

	/**
	 * The file from which bits are to be read.
	 * 
	 * @return the file, never null
	 */
	
	public File getFile() {
		return file;
	}

	/**
	 * The method by which bits will be read from the file.
	 * 
	 * @return the mode, never null
	 */
	
	public Mode getMode() {
		return mode;
	}

	/**
	 * The size of the buffer used to read bytes from the file. The buffer size
	 * is irrelevant when the mode is {@link Mode#MEMORY}.
	 * 
	 * @return the buffer size, always positive
	 */
	
	public int getBufferSize() {
		return bufferSize;
	}
	
	/**
	 * Opens a reader over the bits of the file. The characteristics of the
	 * returned reader are determined by the {@link Mode} in which the factory
	 * was created.
	 * 
	 * Any reader returned by this method SHOULD eventually be closed by passing
	 * it to the {@link #closeReader(BitReader)} method. Not doing so may risk
	 * leaking system resources.
	 * 
	 * @return a new reader over the file
	 * @throws BitStreamException
	 *             if the reader could not be opened, typically because the file
	 *             could not be read
	 */
	
	public BitReader openReader() throws BitStreamException {
		try {
			switch(mode) {
			case MEMORY : return new ByteArrayBitReader(getBytes());
			case STREAM : return new InputStreamBitReader(new BufferedInputStream(new FileInputStream(file), bufferSize));
			case CHANNEL: return new FileChannelBitReader(new RandomAccessFile(file, "r").getChannel(), bufferSize, true);
			default: throw new IllegalStateException("Unexpected mode: " + mode);
			}
		} catch (IOException e) {
			throw new BitStreamException(e);
		}
	}
	
	/**
	 * Closes a reader that was previously opened with a call to
	 * {@link #openReader()}. Closing a reader multiple times has no effect.
	 * 
	 * @param reader
	 *            the reader to close
	 * @throws IllegalArgumentException
	 *             if the supplied reader was null
	 * @throws BitStreamException
	 *             if an IOException was raised when closing the file
	 */
	
	public void closeReader(BitReader reader) throws IllegalArgumentException, BitStreamException {
		if (reader == null) throw new IllegalArgumentException("null reader");
		if (reader instanceof InputStreamBitReader) {
			try {
				((InputStreamBitReader) reader).getInputStream().close();
			} catch (IOException e) {
				throw new BitStreamException(e);
			}
		} else if (reader instanceof FileChannelBitReader) {
			try {
				((FileChannelBitReader) reader).getChannel().close();
			} catch (IOException e) {
				throw new BitStreamException(e);
			}
		}
		
	}

	private byte[] getBytes() throws IOException {
		synchronized (this) {
			if (bytes == null) {
				int size = (int) file.length();
				bytes = new byte[size];
				FileInputStream in = null;
				try {
					in = new FileInputStream(file);
					new DataInputStream(in).readFully(bytes);
				} finally {
					try {
						in.close();
					} catch (IOException e) {
						System.err.println("Failed to close file! " + file);
					}
				}
			}
			return bytes;
		}
	}
}

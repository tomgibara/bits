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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import com.tomgibara.bits.FileBitReaderFactory.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileBitReaderFactoryTest {

	@Test
	public void testModes() throws IOException {

		long[] lengths = {0L, 100L, 10000L};
		int[] buflens = {0, 1000, 20000};
		Random random = new Random();
		// test for different lengths
		for (long length : lengths) {

			// generate bits
			byte[] bytes = new byte[(int) length];
			random.nextBytes(bytes);

			// generate file
			File file = File.createTempFile("crinch-bits-test", "-" + length + ".bits");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }

			// test for each mode
			for (Mode mode : Mode.values()) {
				// for different buffer lengths
				for (int buflen : buflens) {
					FileBitReaderFactory factory = buflen < 1 ? new FileBitReaderFactory(file, mode) : new FileBitReaderFactory(file, mode, buflen);
					// check accessors
					assertEquals(file, factory.getFile());
					assertEquals(mode, factory.getMode());
					// read multiple times
					for (int i = 0; i < 2; i++) {
						BitReader reader = factory.openReader();
						try {
							assertTrue( BitStreams.isSameBits(new ByteArrayBitReader(bytes), reader) );
						} finally {
							factory.closeReader(reader);
							//test closing twice
							factory.closeReader(reader);
						}
					}
				}
			}

			file.deleteOnExit();
		}

	}

}

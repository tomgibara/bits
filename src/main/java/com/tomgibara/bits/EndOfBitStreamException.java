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

/**
 * This exception is raised when methods on a {@link BitReader} or
 * {@link BitWriter} encounter the end of a bit stream during the operation.
 *
 * @author Tom Gibara
 *
 */

public class EndOfBitStreamException extends BitStreamException {

	private static final long serialVersionUID = -4892414594243780142L;

	public EndOfBitStreamException() {
	}

	public EndOfBitStreamException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndOfBitStreamException(String message) {
		super(message);
	}

	public EndOfBitStreamException(Throwable cause) {
		super(cause);
	}

}

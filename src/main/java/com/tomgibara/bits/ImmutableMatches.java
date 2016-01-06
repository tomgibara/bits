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

import java.util.Collections;
import java.util.SortedSet;

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;

class ImmutableMatches implements BitMatches {

	private final BitStore store;
	private final BitMatches matches;

	ImmutableMatches(BitStore store, BitMatches matches) {
		this.store = store;
		this.matches = matches;
	}
	
	@Override
	public BitStore store() {
		return store;
	}
	@Override
	public boolean bit() {
		return matches.bit();
	}
	@Override
	public BitStore sequence() {
		return matches.sequence();
	}
	@Override
	public BitMatches range(int from, int to) {
		return store.range(from, to).match(matches.bit());
	}
	@Override
	public boolean isAll() {
		return matches.isAll();
	}
	@Override
	public boolean isNone() {
		return matches.isNone();
	}
	@Override
	public int count() {
		return matches.count();
	}
	@Override
	public int first() {
		return matches.first();
	}
	@Override
	public int last() {
		return matches.last();
	}
	@Override
	public int next(int position) {
		return matches.next(position);
	}
	@Override
	public int previous(int position) {
		return matches.previous(position);
	}
	@Override
	public Positions positions() {
		return Bits.newPositions(this);
	}
	@Override
	public Positions positions(int position) {
		return Bits.newPositions(this, position);
	}

	@Override
	public void replaceAll(BitStore replacement) {
		Bits.checkMutable();
	}

	@Override
	public void replaceAll(boolean bits) {
		Bits.checkMutable();
	}

	@Override
	public SortedSet<Integer> asSet() {
		return Collections.unmodifiableSortedSet(matches.asSet());
	}
	
}

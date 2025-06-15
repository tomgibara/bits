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

import java.util.Random;

class ReversedBitStore extends AbstractBitStore {

	private final BitStore store;

	ReversedBitStore(BitStore store) {
		this.store = store;
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public boolean getBit(int index) {
		return store.getBit(adjIndex(index));
	}

	@Override
	public void setBit(int index, boolean value) {
		store.setBit(adjIndex(index), value);
	}

	@Override
	public long getBits(int position, int length) {
		long bits = store.getBits(adjPosition(position + length), length);
		return Long.reverse(bits) >>> (64 - length);
	}

	@Override
	public int getBitsAsInt(int position, int length) {
		int bits = store.getBitsAsInt(adjPosition(position + length), length);
		return Integer.reverse(bits) >>> (32 - length);
	}

	@Override
	public void flipBit(int index) {
		store.flipBit(adjIndex(index));
	}

	@Override
	public boolean getThenSetBit(int index, boolean value) {
		return store.getThenSetBit(adjIndex(index), value);
	}

	@Override
	public void setBits(int position, long value, int length) {
		value = Long.reverse(value) >>> (64 - length);
		store.setBits(adjPosition(position + length), value, length);
	}

	@Override
	public void setBitsAsInt(int position, int value, int length) {
		value = Integer.reverse(value) >>> (32 - length);
		store.setBitsAsInt(adjPosition(position + length), value, length);
	}

	@Override
	public void setStore(int position, BitStore store) {
		super.setStore(position, store);
	}

	@Override
	public void fill() {
		store.fill();
	}

	@Override
	public void clear() {
		store.clear();
	}

	@Override
	public void flip() {
		store.flip();
	}

	@Override
	public BitStore reversed() {
		return store;
	}

	@Override
	public Permutes permute() {
		return new ReversedPermutes();
	}

	@Override
	public OverlappingMatches match(BitStore sequence) {
		return new ReversedMatches(store.match(sequence.reversed()));
	}

	@Override
	public BitStore range(int from, int to) {
		return store.range(adjPosition(to), adjPosition(from)).reversed();
	}

	@Override
	public void shift(int distance, boolean fill) {
		store.shift(-distance, fill);
	}

	@Override
	public String toString() {
		return new StringBuilder( store.toString() ).reverse().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BitStore that)) return false;
        return store.equals(that.reversed());
	}

	// mutability

	@Override
	public boolean isMutable() {
		return store.isMutable();
	}

	@Override
	public BitStore mutableCopy() {
		return store.mutableCopy().reversed();
	}

	public BitStore immutableCopy() {
		return store.immutableCopy().reversed();
	}

	@Override
	public BitStore immutableView() {
		return store.immutableView().reversed();
	}

	// helper methods

	private int adjIndex(int index) {
		return store.size() - 1 - index;
	}

	private int adjPosition(int position) {
		return store.size() - position;
	}

	// inner classes

	private class ReversedPermutes implements Permutes {

		private final Permutes permutes;


		ReversedPermutes() {
			permutes = store.permute();
		}

		@Override
		public void transpose(int i, int j) {
			permutes.transpose(adjIndex(i), adjIndex(j));
		}

		@Override
		public void rotate(int distance) {
			permutes.rotate(-distance);
		}

		@Override
		public void reverse() {
			permutes.reverse();
		}

		@Override
		public void shuffle(Random random) {
			permutes.shuffle(random);
		}

	}

	private class ReversedMatches extends AbstractMatches implements OverlappingMatches {

		private final Matches matches;
		private final int seqSize;

		ReversedMatches(Matches matches) {
			super(matches);
			this.matches = matches;
			seqSize = matches.sequence().size();
		}

		@Override
		public BitStore store() {
			return ReversedBitStore.this;
		}

		@Override
		public BitStore sequence() {
			return matches.sequence().reversed();
		}

		@Override
		public DisjointMatches disjoint() {
			return new BitStoreDisjointMatches(this);
		}

		@Override
		public OverlappingMatches range(int from, int to) {
			return new ReversedMatches(matches.range(adjPosition(to), adjPosition(from)));
		}

		@Override
		public int count() {
			return matches.count();
		}

		@Override
		public int first() {
			return unadjSeqPos(matches.last());
		}

		@Override
		public int last() {
			return unadjSeqPos(matches.first());
		}

		@Override
		public int next(int position) {
			int size = store.size();
			int p = size - position + 1 - seqSize;
			int q = matches.previous(p);
			int r = q == -1 ? size : size - q - seqSize;
			return r;
		}

		@Override
		public int previous(int position) {
			int size = store.size();
			int p = size - position + 1 - seqSize;
			int q = matches.next(p);
			int r = q == size ? -1 : size - q - seqSize;
			return r;
		}

		@Override
		public Positions positions() {
			return new ReversedPositions(matches.positions(adjPosition(0)));
		}

		@Override
		public Positions positions(int position) {
			return new ReversedPositions(matches.positions(adjPosition(position)));
		}

		private class ReversedPositions implements Positions {

			private final Positions positions;

			ReversedPositions(Positions positions) {
				this.positions = positions;
			}

			@Override
			public boolean isDisjoint() {
				return positions.isDisjoint();
			}

			@Override
			public boolean hasNext() {
				return positions.hasPrevious();
			}

			@Override
			public Integer next() {
				return unadjInt(positions.previous());
			}

			@Override
			public boolean hasPrevious() {
				return positions.hasNext();
			}

			@Override
			public Integer previous() {
				return unadjInt(positions.next());
			}

			@Override
			public int nextIndex() {
				return unadjSeqPos(positions.previousIndex());
			}

			@Override
			public int previousIndex() {
				return unadjSeqPos(positions.nextIndex());
			}

			@Override
			public void remove() {
				positions.remove();
			}

			@Override
			public void set(Integer e) {
				positions.set(e);
			}

			@Override
			public void add(Integer e) {
				positions.add(e);
			}

			@Override
			public int nextPosition() {
				return unadjSeqPos(positions.previousPosition());
			}

			@Override
			public int previousPosition() {
				return unadjSeqPos(positions.nextPosition());
			}

			@Override
			public void replace(BitStore replacement) {
				if (replacement == null) throw new IllegalArgumentException("null replacement");
				positions.replace(replacement.reversed());
			}

			@Override
			public void replace(boolean bits) {
				positions.replace(bits);
			}
		}

		private int unadjSeqPos(int seqPos) {
			return adjPosition(seqPos + seqSize);
		}

		private Integer unadjInt(Integer i) {
			if (i == null) return null;
			return unadjSeqPos(i);
		}
	}

}

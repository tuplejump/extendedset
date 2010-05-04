/* 
 * (c) 2010 Alessandro Colantonio
 * <mailto:colanton@mat.uniroma3.it>
 * <http://ricerca.mat.uniroma3.it/users/colanton>
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


package it.uniroma3.mat.extendedset;


import java.util.Arrays;
import java.util.BitSet;
import java.util.Formatter;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * An {@link IntSet} implementation, representing a set of integers, based on an
 * uncompressed bitmap.
 * <p>
 * It actually is an extension of {@link BitSet}. More specifically, union and
 * intersection operations are mainly derived from the code of {@link BitSet} to
 * provide bitwise "or" and "and".
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @see ExtendedSet
 * @see AbstractExtendedSet
 * @see ConciseSet
 * @see IndexedSet
 */
public class FastSet extends IntSet {
	/** number of bits within each word */
	private final static int BITS_PER_WORD = 32;

	/** 32-bit string of all 1's */
	private static final int ALL_ONES_WORD = 0xFFFFFFFF;

	/** all bits, grouped in blocks of length 32 */
	private int[] words;

	/** the number of words in the logical size of this {@link FastSet} */
	private int wordsInUse;

	/** cached set size (only for fast size() call). When -1, the cache is invalid */
	private int size;

	/**
	 * Creates a new set. All bits are initially <code>false</code>.
	 */
	public FastSet() {
		clear();
	}

	/**
	 * Given a bit index, it returns the index of the word containing it
	 */
	private static int wordIndex(int bitIndex) {
		if (bitIndex < 0)
			throw new IndexOutOfBoundsException("index < 0: " + bitIndex);
		return bitIndex >> 5;
	}

	/**
	 * Sets the field {@link #wordsInUse} with the logical size in words of the
	 * bit set.
	 */
	private void fixWordsInUse() {
		int i = wordsInUse - 1;
		while (i >= 0 && words[i] == 0)
			i--;
		wordsInUse = i + 1;
	}

	/**
	 * Ensures that the {@link FastSet} can hold enough words.
	 * 
	 * @param wordsRequired
	 *            the minimum acceptable number of words.
	 */
	private void ensureCapacity(int wordsRequired) {
		if (words.length >= wordsRequired) 
			return;
		int newLength = Math.max(words.length << 1, wordsRequired);
		words = Arrays.copyOf(words, newLength);
	}

	/**
	 * Ensures that the {@link FastSet} can accommodate a given word index
	 * 
	 * @param wordIndex
	 *            the index to be accommodated.
	 */
	private void expandTo(int wordIndex) {
		int wordsRequired = wordIndex + 1;
		if (wordsInUse < wordsRequired) {
			ensureCapacity(wordsRequired);
			wordsInUse = wordsRequired;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		FastSet res = new FastSet();
		res.wordsInUse = wordsInUse;
		res.size = size;
		res.words = Arrays.copyOf(words, wordsInUse);
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int h = 1;
		for (int i = 0; i < wordsInUse; i++)
			h = (h << 5) - h + words[i];
		return h;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FastSet))
			return false;
		
		final FastSet other = (FastSet) obj;
		if (wordsInUse != other.wordsInUse)
			return false;
		for (int i = 0; i < wordsInUse; i++)
			if (words[i] != other.words[i])
				return false;
		return true;
	}
	
	/**
	 *  {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return wordsInUse == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		// check if the cached size is invalid
		if (size < 0) {
			size = 0;
			for (int i = 0; i < wordsInUse; i++)
				size += Integer.bitCount(words[i]);
		}
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(int i) {
		int wordIndex = wordIndex(i);
		expandTo(wordIndex);
		int before = words[wordIndex];
		words[wordIndex] |= (1 << i);
		if (before != words[wordIndex]) {
			if (size >= 0)
				size++;
			return true;
		} 
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(int i) {
		if (i < 0)
			return false;

		int wordIndex = wordIndex(i);
		if (wordIndex >= wordsInUse)
			return false;
		int before = words[wordIndex];
		words[wordIndex] &= ~(1 << i);
		if (before != words[wordIndex]) {
			if (size >= 0)
				size--;
			fixWordsInUse();
			return true;
		} 
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(IntSet c) {
		if (c == null || c.isEmpty() || this == c)
			return false;

		final FastSet other = convert(c);

		int wordsInCommon = Math.min(wordsInUse, other.wordsInUse);

		boolean modified = false;
		if (wordsInUse < other.wordsInUse) {
			modified = true;
			ensureCapacity(other.wordsInUse);
			wordsInUse = other.wordsInUse;
		}

		// Perform logical OR on words in common
		for (int i = 0; i < wordsInCommon; i++) {
			int before = words[i];
			words[i] |= other.words[i];
			modified = modified || before != words[i];
		}

		// Copy any remaining words
		if (wordsInCommon < other.wordsInUse) {
			modified = true;
			System.arraycopy(
					other.words, wordsInCommon, words, 
					wordsInCommon, wordsInUse - wordsInCommon);
		}
		if (modified)
			size = -1;
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(IntSet c) {
		if (c == null || c.isEmpty() || isEmpty())
			return false;
		if (c == this) {
			clear();
			return true;
		}
		
		final FastSet other = convert(c);

		// Perform logical (a & !b) on words in common
		boolean modified = false;
		for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--) {
			int before = words[i];
			words[i] &= ~other.words[i];
			modified = modified || before != words[i];
		}
		if (modified) {
			fixWordsInUse();
			size = -1;
		}
		return modified;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(IntSet c) {
		if (isEmpty() || c == this)
			return false;
		if (c == null || c.isEmpty()) {
			clear();
			return true;
		}
		
		final FastSet other = convert(c);

		boolean modified = false;
		if (wordsInUse > other.wordsInUse) {
			modified = true;
			while (wordsInUse > other.wordsInUse)
				words[--wordsInUse] = 0;
		}

		// Perform logical AND on words in common
		for (int i = 0; i < wordsInUse; i++) {
			int before = words[i];
			words[i] &= other.words[i];
			modified = modified || before != words[i];
		}
		if (modified) {
			fixWordsInUse();
			size = -1;
		}
		return modified;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		words = new int[1];
		wordsInUse = 0;
		size = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(int i) {
		if (isEmpty() || i < 0)
			return false;
		int wordIndex = wordIndex(i);
		return (wordIndex < wordsInUse)
				&& ((words[wordIndex] & (1 << i)) != 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(IntSet c) {
		if (c == null || c.isEmpty() || c == this)
			return true;
		if (isEmpty())
			return false;

		final FastSet other = convert(c);

		if (other.wordsInUse > wordsInUse)
			return false;

		for (int i = 0; i < other.wordsInUse; i++) {
			int o = other.words[i];
			if ((words[i] & o) != o)
				return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(IntSet c, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		if ((size >= 0 && size < minElements) || c == null || c.isEmpty() || isEmpty())
			return false;
		if (this == c)
			return size() >= minElements;

		final FastSet other = convert(c);

		int count = 0;
		for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--) {
			count += Integer.bitCount(words[i] & other.words[i]);
			if (count >= minElements)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(IntSet c) {
		if (c == null || c.isEmpty() || c == this)
			return true;
		if (isEmpty())
			return false;
		
		final FastSet other = convert(c);
			
		for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--)
			if ((words[i] & other.words[i]) != 0)
				return true;
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(IntSet c) {
		if (c == null || c.isEmpty())
			return 0;
		if (c == this)
			return size();
		if (isEmpty())
			return 0;

		final FastSet other = convert(c);

		int count = 0;
		for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--) 
			count += Integer.bitCount(words[i] & other.words[i]);
		return count;
	}

	/**
	 * Iterates over bits
	 */
	private class BitIterator implements ExtendedIntIterator {
		// current bit
		private int curr;
		
		// next bit to poll
		private int next;

		/**
		 * Returns the index of the first bit that is set to <code>true</code> that
		 * occurs on or after the specified starting index. If no such bit exists,
		 * then -1 is returned.
		 */
		private int nextSetBit(int fromIndex) {
			int u = wordIndex(fromIndex);
			if (u >= wordsInUse)
				return -1;
			int word = words[u] & (ALL_ONES_WORD << fromIndex);
			while (true) {
				if (word != 0)
					return (u * BITS_PER_WORD)
							+ Integer.numberOfTrailingZeros(word);
				if (++u == wordsInUse)
					return -1;
				word = words[u];
			}
		}

		/**
		 * Constructor
		 */
		public BitIterator() {
			curr = -1;
			next = nextSetBit(0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int next() {
			if (!hasNext())
				throw new NoSuchElementException();
			curr = next;
			next = nextSetBit(curr + 1);
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(int element) {
			if (element <= next)
				return;
			next = nextSetBit(element);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !contains(curr))
				throw new IllegalStateException();
			FastSet.this.remove(curr);
		}
	}

	/**
	 * Iterates over bits
	 */
	private class ReverseBitIterator implements ExtendedIntIterator {
		// current bit
		private int curr;
		
		// next bit to poll
		private int next;

		/**
		 * Returns the index of the first bit that is set to <code>true</code> that
		 * occurs on or before the specified starting index. If no such bit exists,
		 * then -1 is returned.
		 */
		private int nextSetBit(int fromIndex) {
			if (fromIndex < 0)
				return -1;
			int u = wordIndex(fromIndex);
			if (u >= wordsInUse)
				return last();
			int word = words[u] & (ALL_ONES_WORD >>> -(fromIndex + 1));
			while (true) {
				if (word != 0)
					return ((u + 1) * BITS_PER_WORD)
							- Integer.numberOfLeadingZeros(word) - 1;
				if (--u < 0)
					return -1;
				word = words[u];
			}
		}

		/**
		 * Constructor
		 */
		public ReverseBitIterator() {
			curr = -1;
			next = isEmpty() ? -1 : last();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int next() {
			if (!hasNext())
				throw new NoSuchElementException();
			curr = next;
			next = nextSetBit(curr - 1);
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(int element) {
			if (element >= next)
				return;
			next = nextSetBit(element);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !contains(curr))
				throw new IllegalStateException();
			FastSet.this.remove(curr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIntIterator intIterator() {
		return new BitIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIntIterator descendingIntIterator() {
		return new ReverseBitIterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int last() {
		if (isEmpty())
			throw new NoSuchElementException();
		return BITS_PER_WORD
			* (wordsInUse - 1)
			+ (BITS_PER_WORD - Integer.numberOfLeadingZeros(words[wordsInUse - 1])) - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		if (isEmpty())
			return;
		if (size > 0)
			size = last() - size + 1;
		int lastWordMask = ALL_ONES_WORD >>> Integer.numberOfLeadingZeros(words[wordsInUse - 1]);
		for (int i = 0; i < wordsInUse - 1; i++)
			words[i] ^= ALL_ONES_WORD;
		words[wordsInUse - 1] ^= lastWordMask;
		fixWordsInUse();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet complemented() {
		FastSet clone = clone();
		clone.complement();
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet empty() {
		return new FastSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet symmetricDifference(IntSet c) {
		if (c == null || c.isEmpty())
			return clone();
		if (c == this)
			return empty();
		
		final FastSet other = convert(c);

		if (c.isEmpty()) 
			return other.clone();

		FastSet cloned = clone();
		cloned.size = -1;

		int wordsInCommon = Math.min(cloned.wordsInUse, other.wordsInUse);

		if (cloned.wordsInUse < other.wordsInUse) {
			cloned.ensureCapacity(other.wordsInUse);
			cloned.wordsInUse = other.wordsInUse;
		}

		// Perform logical XOR on words in common
		for (int i = 0; i < wordsInCommon; i++)
			cloned.words[i] ^= other.words[i];

		// Copy any remaining words
		if (wordsInCommon < other.wordsInUse)
			System.arraycopy(other.words, wordsInCommon, cloned.words, wordsInCommon,
					other.wordsInUse - wordsInCommon);
		cloned.fixWordsInUse();
		return cloned;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return 1D;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) wordsInUse / size();
	}

	/**
	 * Convert a given collection to a {@link FastSet} instance
	 */
	private FastSet convert(IntSet c) {
		if (c instanceof FastSet)
			return (FastSet) c;
		if (c == null)
			return new FastSet();

		FastSet res = new FastSet();
		ExtendedIntIterator itr = c.intIterator();
		while (itr.hasNext()) 
			res.add(itr.next());
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet convert(int... a) {
		FastSet res = new FastSet();
		if (a != null)
			for (int i : a)
				res.add(i);
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(int fromIndex, int toIndex) {
		if (fromIndex > toIndex)
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
					+ " > toIndex: " + toIndex);
		if (fromIndex == toIndex) {
			add(fromIndex);
			return;
		}

		// Increase capacity if necessary
		int startWordIndex = wordIndex(fromIndex);
		int endWordIndex = wordIndex(toIndex);
		expandTo(endWordIndex);

		boolean modified = false;
		int firstWordMask = ALL_ONES_WORD << fromIndex;
		int lastWordMask = ALL_ONES_WORD >>> -(toIndex + 1);
		if (startWordIndex == endWordIndex) {
			// Case 1: One word
			int before = words[startWordIndex];
			words[startWordIndex] |= (firstWordMask & lastWordMask);
			modified = words[startWordIndex] != before;
		} else {
			// Case 2: Multiple words
			// Handle first word
			int before = words[startWordIndex];
			words[startWordIndex] |= firstWordMask;
			modified = words[startWordIndex] != before;

			// Handle intermediate words, if any
			for (int i = startWordIndex + 1; i < endWordIndex; i++) {
				modified = modified || words[i] != ALL_ONES_WORD;
				words[i] = ALL_ONES_WORD;
			}

			// Handle last word
			before = words[endWordIndex];
			words[endWordIndex] |= lastWordMask;
			modified = modified || words[endWordIndex] != before;
		}
		if (modified)
			size = -1;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(int fromIndex, int toIndex) {
		if (fromIndex > toIndex)
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
					+ " > toIndex: " + toIndex);
		if (fromIndex == toIndex) {
			remove(fromIndex);
			return;
		}

		int startWordIndex = wordIndex(fromIndex);
		if (startWordIndex >= wordsInUse)
			return;

		int endWordIndex = wordIndex(toIndex);
		if (endWordIndex >= wordsInUse) {
			toIndex = last();
			endWordIndex = wordsInUse - 1;
		}

		boolean modified = false;
		int firstWordMask = ALL_ONES_WORD << fromIndex;
		int lastWordMask = ALL_ONES_WORD >>> -(toIndex + 1);
		if (startWordIndex == endWordIndex) {
			// Case 1: One word
			int before = words[startWordIndex];
			words[startWordIndex] &= ~(firstWordMask & lastWordMask);
			modified = words[startWordIndex] != before;
		} else {
			// Case 2: Multiple words
			// Handle first word
			int before = words[startWordIndex];
			words[startWordIndex] &= ~firstWordMask;
			modified = words[startWordIndex] != before;

			// Handle intermediate words, if any
			for (int i = startWordIndex + 1; i < endWordIndex; i++) {
				modified = modified || words[i] != 0;
				words[i] = 0;
			}

			// Handle last word
			before = words[endWordIndex];
			words[endWordIndex] &= ~lastWordMask;
			modified = modified || words[endWordIndex] != before;
		}
		if (modified) {
			fixWordsInUse();
			size = -1;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(int e) {
		int wordIndex = wordIndex(e);
		expandTo(wordIndex);
		int mask = (1 << e);
		words[wordIndex] ^= mask;
		fixWordsInUse();
		if (size >= 0) {
			if ((words[wordIndex] & mask) == 0) 
				size--;
			else
				size++;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(IntSet o) {
		// empty set cases
		if (this.isEmpty() && o.isEmpty())
			return 0;
		if (this.isEmpty())
			return -1;
		if (o.isEmpty())
			return 1;
		
		final FastSet other = convert(o);

		if (wordsInUse > other.wordsInUse)
			return 1;
		if (wordsInUse < other.wordsInUse)
			return -1;
		for (int i = wordsInUse - 1; i >= 0; i--) {
			long w1 = words[i] & 0xFFFFFFFFL;
			long w2 = other.words[i] & 0xFFFFFFFFL;
			int res = w1 < w2 ? -1 : (w1 > w2 ? 1 : 0);
			if (res != 0)
				return res;
		}
		return 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int get(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		
		int count = 0;
		for (int j = 0; j < wordsInUse; j++) {
			int w = words[j];
			int current = Integer.bitCount(w);
			if (index < count + current) {
				int bit = -1;
				for (int skip = index - count; skip >= 0; skip--)
					bit = Integer.numberOfTrailingZeros(w & (ALL_ONES_WORD << (bit + 1)));
				return BITS_PER_WORD * j + bit;
			}
			count += current;
		}
		throw new NoSuchElementException();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(int e) {
		int index = wordIndex(e);
		int count = 0;
		for (int j = 0; j < index; j++)
			count += Integer.bitCount(words[j]);
		count += Integer.bitCount(words[index] & ~(ALL_ONES_WORD << e));
		return count;
	}
	
	/**
	 * Generates the 32-bit binary representation of a given word (debug only)
	 * 
	 * @param word
	 *            word to represent
	 * @return 32-character string that represents the given word
	 */
	private static String toBinaryString(int word) {
		String lsb = Integer.toBinaryString(word);
		StringBuilder pad = new StringBuilder();
		for (int i = lsb.length(); i < 32; i++) 
			pad.append('0');
		return pad.append(lsb).toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		final StringBuilder s = new StringBuilder("INTERNAL REPRESENTATION:\n");
		final Formatter f = new Formatter(s, Locale.ENGLISH);

		if (isEmpty())
			return s.append("null\n").toString();
		
		// elements
		f.format("Elements: %s\n", toString());
		
		// raw representation of words
		for (int i = 0; i < wordsInUse; i++)
			f.format("words[%d] = %s (from %d to %d)\n", 
					i, toBinaryString(words[i]), i * BITS_PER_WORD, (i + 1) * BITS_PER_WORD - 1);
		
		// object attributes
		f.format("wordsInUse: %d\n", wordsInUse);
		f.format("size: %s\n", (size == -1 ? "invalid" : Integer.toString(size)));
		f.format("words.length: %d\n", words.length);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet difference(IntSet other) {
		FastSet clone = clone();
		clone.removeAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet intersection(IntSet other) {
		FastSet clone = clone();
		clone.retainAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet union(IntSet other) {
		FastSet clone = clone();
		clone.addAll(other);
		return clone;
	}
}

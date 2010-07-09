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

package it.uniroma3.mat.extendedset.intset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * This is CONCISE+: COmpressed 'N' Composable Integer SEt, improved.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
//TODO: complete the class methods by adding TODO in method bodies
public class ConcisePlusSet extends AbstractIntSet {
	/** 
	 * Compressed representation of the integer set 
	 */
	private int[] words;
	
	/** 
	 * Maximum element 
	 */
	private int last;
	
	/**
	 * Cached cardinality of the bit-set. Defined for efficient {@link #size()}
	 * calls. When -1, the cache is invalid.
	 */ 
	private int size;

	/**
	 * Index of the last (literal or marker) word in {@link #words}
	 */ 
	private int lastWordIndex;

	/**
	 * Index of the last marker word in {@link #words}
	 */ 
	private int lastMarkerIndex;

	/**
	 * Creates an empty set
	 */
	public ConcisePlusSet() {
		clear();
	}

	/**
	 * Convert a given collection to a {@link FastSet} instance
	 */
	private ConcisePlusSet convert(IntSet c) {
		if (c instanceof ConcisePlusSet)
			return (ConcisePlusSet) c;
		if (c == null)
			return new ConcisePlusSet();

		ConcisePlusSet res = new ConcisePlusSet();
		IntIterator itr = c.iterator();
		while (itr.hasNext()) 
			res.add(itr.next());
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConcisePlusSet convert(int... a) {
		ConcisePlusSet res = new ConcisePlusSet();
		if (a != null) {
			a = Arrays.copyOf(a, a.length);
			Arrays.sort(a);
			for (int i : a)
				if (last != i)
					res.add(i);
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IntSet convert(Collection<Integer> c) {
		ConcisePlusSet res = empty();
		Collection<Integer> sorted;
		if (c != null) {
			if (c instanceof SortedSet<?> && ((SortedSet<?>) c).comparator() == null) {
				sorted = c;
			} else {
				sorted = new ArrayList<Integer>(c);
				Collections.sort((List<Integer>) sorted);
			}
			for (int i : sorted)
				res.add(i);
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConcisePlusSet clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		if (isEmpty())
			return new ConcisePlusSet();
		ConcisePlusSet res = new ConcisePlusSet();
		res.words = Arrays.copyOf(words, lastWordIndex + 1);
		res.lastMarkerIndex = lastMarkerIndex;
		res.lastWordIndex = lastWordIndex;
		res.last = last;
		res.size = size;
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return lastWordIndex < 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		words = null;
		last = -1;
		size = 0;
		lastWordIndex = -1;
		lastMarkerIndex = -1;
	}

	/** number of bits required to represent the bit position within a word */
	private final static int WORD_LOGB = 5;

	/** number of bits for each word */
	private final static int WORD_SIZE = 1 << WORD_LOGB;
	
	/** all bits set to 1 */
	private final static int FILLED_LITERAL = -1;

	/** MSB = 1, to indicate a marker word */
	private final static int MARKER_MASK = 1 << (WORD_SIZE - 1);
	
	/** bits of the marker word that indicates the number of literals */
	private final static int LITERAL_MASK = WORD_SIZE - 1;

	/** number of bits required for the literal count in a marker */
	private final static int LITERAL_MASK_SIZE = WORD_LOGB;
	
	/** bits of the marker word that indicates the prefix */
	private final static int PREFIX_MASK = ~MARKER_MASK & ~LITERAL_MASK;


	/**
	 * Append an integer to the set. The new integer <i>must</i> be greater than
	 * the current highest element of the set
	 * 
	 * @param i
	 *            the new integer to add
	 */
	private void append(final int i) {
		// check if it is a valid append request
		assert i > last : "i > last: " + i + " > " + last;
		
		// first append operation
		if (isEmpty()) {
			if (words == null)
				words = new int[1];
			words[0] = i;
			lastWordIndex = 0;
			lastMarkerIndex = 0;
			last = i;
			size = 1;
			return;
		} 
		
		final int newPrefix = i & PREFIX_MASK;
		final int markerValue = words[lastMarkerIndex];
		final int markerPrefix = markerValue & PREFIX_MASK;

		// check whether the last appended word is a single-value marker word
		if (lastMarkerIndex == lastWordIndex) {
			// allocate more space
			lastWordIndex++;
			ensureCapacity();
			
			// check if the new bit is within the last word
			if (newPrefix == markerPrefix) {
				// set the last and the new bits
				words[lastWordIndex] = (1 << markerValue) | (1 << i);
				
				// transform a mixed word into a sequence with one following literal
				words[lastMarkerIndex] &= PREFIX_MASK;
				words[lastMarkerIndex] |= MARKER_MASK;
			} else {
				// append a new mixed word
				words[lastMarkerIndex = lastWordIndex] = i;
			}
		} else {
			// check if the new bit is within the last word
			final int literals = LITERAL_MASK & markerValue;
			final int gap = newPrefix - markerPrefix - (literals << LITERAL_MASK_SIZE);
			if (gap == 0) {
				// add the new bit
				words[lastWordIndex] |= 1 << i;
			} else if (gap == WORD_SIZE && literals < (WORD_SIZE - 1)) {
				// allocate more space
				lastWordIndex++;
				ensureCapacity();
				
				// add the new bit
				words[lastWordIndex] |= 1 << i;

				// increase the literal count
				words[lastMarkerIndex]++;
			} else {
				// allocate more space
				lastWordIndex++;
				ensureCapacity();
				
				// append a new mixed word
				lastMarkerIndex = lastWordIndex;
				words[lastWordIndex] = i;
			}
		}
		
		// update other info
		last = i;
		if (size >= 0)
			size++;
	}

	/**
	 * Appends a literal word to the set, by specifying its prefix. The
	 * specified prefix <i>must</i> be greater than the prefix of the current
	 * highest element of the set.
	 * <p>
	 * NOTE: it does not update {@link #last} and {@link #size}
	 * 
	 * @param literal
	 *            the new literal word to add
	 * @param prefix
	 *            the prefix of the new literal word to add
	 */
	//TODO: it takes 30% of the time for dense datasets...
	private void appendLiteral(int literal, int prefix) {
		// useless to append
		if (literal == 0)
			return;

		// first append operation
		if (isEmpty()) {
			if (containsOnlyOneBit(literal)) {
				lastWordIndex = lastMarkerIndex = 0;
				ensureCapacity();
				words[0] = prefix + Integer.numberOfTrailingZeros(literal);
			} else {
				lastMarkerIndex = 0;
				lastWordIndex = 1;
				ensureCapacity();
				words[0] = MARKER_MASK | prefix;
				words[1] = literal;
			}
			return;
		} 

		// number of literals in the last marker
		final int literals = lastWordIndex - lastMarkerIndex;
		
		// if the last marker is a single-value word or it contains the maximum
		// number of literals, then create a new marker word
		if (literals == 0 || literals == WORD_SIZE) {
			if (containsOnlyOneBit(literal)) {
				// single-value marker
				lastMarkerIndex = ++lastWordIndex;
				ensureCapacity();
				words[lastWordIndex] = prefix + Integer.numberOfTrailingZeros(literal);
			} else {
				// simple marker
				lastMarkerIndex = ++lastWordIndex;
				++lastWordIndex;
				ensureCapacity();
				words[lastMarkerIndex] = MARKER_MASK | prefix;
				words[lastWordIndex] = literal;
			}
		} else {
			// check if there is a gap between the last literal and the new one
			final int currPrefix = (words[lastMarkerIndex] & PREFIX_MASK) + (literals << LITERAL_MASK_SIZE);
			assert prefix >= currPrefix : "prefix >= currPrefix: " + prefix + " >= " + currPrefix;
			if (prefix == currPrefix) {
				// append the new literal
				lastWordIndex++;
				ensureCapacity();
				words[lastWordIndex] = literal;
				words[lastMarkerIndex]++;
			} else {
				// there is a gap --> create a new marker
				if (containsOnlyOneBit(literal)) {
					// single-value marker
					lastMarkerIndex = ++lastWordIndex;
					ensureCapacity();
					words[lastWordIndex] = prefix + Integer.numberOfTrailingZeros(literal);
				} else {
					// simple marker
					lastMarkerIndex = ++lastWordIndex;
					++lastWordIndex;
					ensureCapacity();
					words[lastMarkerIndex] = MARKER_MASK | prefix;
					words[lastWordIndex] = literal;
				}
			}
		}
	}

	/**
	 * Appends a literal word to the set, by specifying its prefix, when we are
	 * <i>sure</i> that the literal contains one and only one bit. It is a
	 * simplified (and faster) version of {@link #appendLiteral(int, int)}. The
	 * specified prefix <i>must</i> be greater than the prefix of the current
	 * highest element of the set.
	 * <p>
	 * NOTE: it does not update {@link #last} and {@link #size}
	 * 
	 * @param literal
	 *            the new literal word to add, with only one se tbit
	 * @param prefix
	 *            the prefix of the new literal word to add
	 * @param value
	 *            the single value represented by the literal, namely
	 *            <code>prefix + Integer.numberOfTrailingZeros(literal)</code>
	 */
	private void appendLiteral(int literal, int prefix, int value) {
		assert (prefix + Integer.numberOfTrailingZeros(literal)) == value;
		
		// first append operation
		if (isEmpty()) {
			lastWordIndex = lastMarkerIndex = 0;
			ensureCapacity();
			words[0] = value;
			return;
		} 

		// number of literals in the last marker
		final int literals = lastWordIndex - lastMarkerIndex;
		
		// if the last marker is a single-value word or it contains the maximum
		// number of literals, then create a new marker word
		if (literals == 0 || literals == WORD_SIZE) {
			// single-value marker
			lastMarkerIndex = ++lastWordIndex;
			ensureCapacity();
			words[lastWordIndex] = value;
		} else {
			// check if there is a gap between the last literal and the new one
			final int currPrefix = (words[lastMarkerIndex] & PREFIX_MASK) + (literals << LITERAL_MASK_SIZE);
			assert prefix >= currPrefix : "prefix >= currPrefix: " + prefix + " >= " + currPrefix;
			if (prefix == currPrefix) {
				// append the new literal
				lastWordIndex++;
				ensureCapacity();
				words[lastWordIndex] = literal;
				words[lastMarkerIndex]++;
			} else {
				// there is a gap --> create a new marker
				// single-value marker
				lastMarkerIndex = ++lastWordIndex;
				ensureCapacity();
				words[lastWordIndex] = value;
			}
		}
	}
	
	/**
	 * Appends all the literal words specified by word iterator, till the end of
	 * the iterator or the prefix is greater than the specified one
	 * 
	 * @param itr
	 *            the word iterator
	 * @return <code>true</code> if the specified prefix has been reached,
	 *         <code>false</code> if the iterator has no more elements
	 */
	//TODO optimize by calling System.arraycopy
	private boolean appendAll(WordIterator itr, int prefix) {
		while (itr.currentPrefix < prefix) {
			appendLiteral(itr.currentLiteral(), itr.currentPrefix);
			if (!itr.hasNext())
				return false;
			itr.next();
		}
		return true;
	}
	
	/**
	 * Iterate over words.
	 * <p>
	 * After calling {@link #next()}, {@link #currentWordValue} contains the
	 * current word, that is:
	 * <ul>
	 * <li>A literal word (in this case, {@link #currentPrefix} indicates the
	 * value of the first bit of the literal);
	 * <li>A single-value marker word, namely the current integer (in this case,
	 * {@link #currentPrefix} indicates the value of the first bit of a literal
	 * that would contain the only set bit).
	 * </ul>
	 * Note that normal markers are never returned, but they are only used to
	 * compute the number of following literal words and the corresponding
	 * prefixes.
	 */
	private class WordIterator {
		/** index of currently pointed word */
		int currentWordIndex = -1;

		/** value of the currently pointed word */
		int currentWordValue = 0;

		/** prefix for the bits of the the current literal word */
		int currentPrefix = -WORD_SIZE;

		/** number of literals from {@link #currentWordIndex} to the next marker */
		int remainingLiterals = 0;

		/** <code>true</code> if the current word is a single-value marker */
		boolean isSingleValue = true;

		/**
		 * Prepare the next word
		 * 
		 * @throws NoSuchElementException
		 *             if there are no words to prepare
		 */
		//TODO: it takes about 20% of the time for dense datasets
		void next() {
			// check if there are words to read
			if (!hasNext())
				throw new NoSuchElementException();

			// read next word
			currentWordValue = words[++currentWordIndex];
			
			// check whether current word is a literal
			if (remainingLiterals > 0) {
				remainingLiterals--;
				currentPrefix += WORD_SIZE;
				return;
			}
			
			// extract the prefix
			currentPrefix = currentWordValue & PREFIX_MASK;

			// the word is a marker: check whether it is a single bit word
			isSingleValue = currentWordValue >= 0;
			if (isSingleValue)
				return;
			
			// extract the number of following literals
			remainingLiterals = currentWordValue & LITERAL_MASK;
			assert remainingLiterals >= 0 : "remainingLiterals >= 0: " + remainingLiterals;
			assert hasNext() : "hasNext()";
			
			// return the first literal
			currentWordValue = words[++currentWordIndex];
		}

		int currentLiteral() {
			if (isSingleValue)
				return 1 << currentWordValue;
			return currentWordValue;
		}
		/**
		 * Prepare the next literal word. <i>It must be called when we are sure
		 * that the next word is a literal!</i>
		 */
		void fastNext() {
			assert hasNext() && remainingLiterals > 0 : "hasNext() && remainingLiterals > 0: " + hasNext() + " && " + remainingLiterals  + " > 0";
			currentWordValue = words[++currentWordIndex];
			remainingLiterals--;
			currentPrefix += WORD_SIZE;
		}

		/**
		 * Checks if there are more words. (In other words, returns
		 * <code>true</code> if calling <code>next()</code> would prepare the
		 * new element rather than throwing an exception.)
		 * 
		 * @return <code>true</code> if there are more words
		 */
		boolean hasNext() {
			return currentWordIndex < lastWordIndex;
		}

		/**
		 * Skips all words that have a prefix strictly less than the specified
		 * one. If all the words are below the given prefix, {@link #hasNext()}
		 * returns <code>false</code> after calling this method. If the current
		 * prefix is above the specified one, the method returns without
		 * changing the iterator.
		 * 
		 * @param prefix
		 *            the prefix to jump to
		 * @return <code>true</code> if the specified prefix has been reached,
		 *         <code>false</code> if the iterator has no more elements
		 */
		//TODO it takes about 60% of the time for sparse datasets
		boolean skipAllBefore(int prefix) {
			while (currentPrefix < prefix) {
				if (!isSingleValue) {
					final int gap = (prefix - currentPrefix) >>> LITERAL_MASK_SIZE;
					if (gap <= remainingLiterals) {
						currentWordIndex += gap;
						remainingLiterals -= gap;
						currentPrefix = prefix;
						currentWordValue = words[currentWordIndex];
						return true;
					} 
					currentWordIndex += remainingLiterals;
//					currentPrefix += remainingLiterals << LITERAL_MASK_SIZE;
					remainingLiterals = 0;
				}

				if (currentWordIndex == lastWordIndex)
					return false;

				currentWordValue = words[++currentWordIndex];
				currentPrefix = currentWordValue & PREFIX_MASK;
				isSingleValue = currentWordValue >= 0;
				if (!isSingleValue) {
					remainingLiterals = currentWordValue & LITERAL_MASK;
					currentWordValue = words[++currentWordIndex];
				}
			}
			return true;
		}
	}
	
	@SuppressWarnings("unused")
	private class ReverseWordIterator {
		//TODO
	}
	
	/**
	 * To iterate over set bits
	 */
	public class IntegerIterator implements IntIterator {
		private final WordIterator wordIterator = new WordIterator();
		private int nextBitToCheck = WORD_SIZE;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int next() {
			if (nextBitToCheck > (WORD_SIZE - 1)) {
				wordIterator.next();
				if (wordIterator.isSingleValue)
					return wordIterator.currentWordValue;
				nextBitToCheck = 0;
			}
			
			while (nextBitToCheck < WORD_SIZE && (wordIterator.currentWordValue & (1 << nextBitToCheck)) == 0)
				nextBitToCheck++;
			if (nextBitToCheck < WORD_SIZE) 
				return wordIterator.currentPrefix + nextBitToCheck++;
			
			return next();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			if (wordIterator.hasNext())
				return true;
			if (nextBitToCheck > (WORD_SIZE - 1))
				return false;
			return (wordIterator.currentWordValue & (FILLED_LITERAL << nextBitToCheck)) != 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(int i) {
			//TODO
			throw new UnsupportedOperationException("TODO");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * To iterate over set bits in reverse order
	 */
	public class ReverseIntegerIterator implements IntIterator {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public int next() {
			//TODO
			throw new UnsupportedOperationException("TODO");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			//TODO
			throw new UnsupportedOperationException("TODO");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(int i) {
			//TODO
			throw new UnsupportedOperationException("TODO");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			//TODO
			throw new UnsupportedOperationException("TODO");
		}
	}

	/**
	 * Assures that the size of {@link #words} is sufficient to contain
	 * {@link #lastWordIndex} + 1 elements.
	 */
	private void ensureCapacity() {
		int capacity = words == null ? 0 : words.length;
		if (capacity > lastWordIndex)
			return;
		capacity = Math.max(capacity << 1, lastWordIndex + 1);

		if (words == null) {
			// nothing to copy
			words = new int[capacity];
			return;
		}
		words = Arrays.copyOf(words, capacity);
	}

	/**
	 * Returns <code>true</code> when the given literal string contains only one
	 * set bit
	 * 
	 * @param literal
	 *            a literal word
	 * @return <code>true</code> when the given literal contains only one set
	 *         bit
	 */
	private static boolean containsOnlyOneBit(int literal) {
		return (literal & (literal - 1)) == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public int intersectionSize(IntSet c) {
		if (c == null || c.isEmpty())
			return 0;
		if (c == this)
			return size();
		if (isEmpty())
			return 0;

		final ConcisePlusSet other = convert(c);

		// special cases
		if (isEmpty() || other == null || other.isEmpty() 
				|| last < (other.words[0] & PREFIX_MASK)
				|| other.last < (words[0] & PREFIX_MASK))
			return 0;
		if (this == other)
			return size();
		
		// iterate over the two sets
		int res = 0;
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!thisIterator.skipAllBefore(otherIterator.currentPrefix))
					break mainLoop;
				if (!otherIterator.skipAllBefore(thisIterator.currentPrefix))
					break mainLoop;
			}
				
			// perform the intersection
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				if (thisIterator.currentWordValue == otherIterator.currentWordValue)
					res++;
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				if (((1 << thisIterator.currentWordValue) & otherIterator.currentWordValue) != 0)
					res++;
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				if ((thisIterator.currentWordValue & (1 << otherIterator.currentWordValue)) != 0)
					res++;
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res += Integer.bitCount(thisIterator.currentWordValue & otherIterator.currentWordValue);
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// final result
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public ConcisePlusSet intersection(IntSet c) {
		// special cases
		if (isEmpty() || c == null || c.isEmpty())
			return new ConcisePlusSet();
		if (this == c)
			return clone();
		final ConcisePlusSet other = convert(c);
		if (last < (other.words[0] & PREFIX_MASK)
				|| other.last < (words[0] & PREFIX_MASK))
			return new ConcisePlusSet();
		
		// allocate a space that likely contains the result
		ConcisePlusSet res = new ConcisePlusSet();
		res.words = new int[Math.min(lastWordIndex, other.lastWordIndex) + 1];
		
		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!thisIterator.skipAllBefore(otherIterator.currentPrefix))
					break mainLoop;
				if (!otherIterator.skipAllBefore(thisIterator.currentPrefix))
					break mainLoop;
			}
			
			// perform the intersection
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				// NOTE: if both "this" and "other" are single-value marker, it is
				// impossible that their intersection can become a literal that
				// belongs to a sequence of subsequent literals...
				if (thisIterator.currentWordValue == otherIterator.currentWordValue)
					res.words[res.lastMarkerIndex = ++res.lastWordIndex] = thisIterator.currentWordValue;
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				int l = 1 << thisIterator.currentWordValue;
				if ((l & otherIterator.currentWordValue) != 0)
					res.appendLiteral(l, thisIterator.currentPrefix, thisIterator.currentWordValue);
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				int l = 1 << otherIterator.currentWordValue;
				if ((thisIterator.currentWordValue & l) != 0)
					res.appendLiteral(l, thisIterator.currentPrefix, otherIterator.currentWordValue);
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res.appendLiteral(thisIterator.currentWordValue & otherIterator.currentWordValue, thisIterator.currentPrefix);
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// final result
		res.updateLast();
		res.size = -1;
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public ConcisePlusSet difference(IntSet c) {
		// special cases
		if (isEmpty() || this == c)
			return new ConcisePlusSet();
		if (c == null || c.isEmpty())
			return clone();
		final ConcisePlusSet other = convert(c);
		if (last < (other.words[0] & PREFIX_MASK)
				|| other.last < (words[0] & PREFIX_MASK))
			return clone();

		// allocate a space that likely contains the result
		ConcisePlusSet res = new ConcisePlusSet();
		res.words = new int[lastWordIndex + 1];
		
		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!res.appendAll(thisIterator, otherIterator.currentPrefix))
					break mainLoop;
				if (!otherIterator.skipAllBefore(thisIterator.currentPrefix)) {
					res.appendAll(thisIterator, Integer.MAX_VALUE);
					break mainLoop;
				}
			}

			// perform the union
			// NOTE: if "this" and/or "other" are single-value marker, it is
			// impossible that their difference can become a literal that
			// belongs to a sequence of subsequent literals...
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				if (thisIterator.currentWordValue != otherIterator.currentWordValue)
					res.appendLiteral(1 << thisIterator.currentWordValue, thisIterator.currentPrefix, thisIterator.currentWordValue);
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				int l = 1 << thisIterator.currentWordValue;
				if ((l & ~otherIterator.currentWordValue) != 0)
					res.appendLiteral(l, thisIterator.currentPrefix, thisIterator.currentWordValue);
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				res.appendLiteral(thisIterator.currentWordValue & ~(1 << otherIterator.currentWordValue), thisIterator.currentPrefix);
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res.appendLiteral(thisIterator.currentWordValue & ~otherIterator.currentWordValue, thisIterator.currentPrefix);
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// append remaining words
		if (thisIterator.hasNext()) {
			thisIterator.next();
			res.appendAll(thisIterator, Integer.MAX_VALUE);
		}

		// final result
		res.updateLast();
		res.size = -1;
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public ConcisePlusSet union(IntSet c) {
		// special cases
		if (this == c || c == null || c.isEmpty())
			return clone();
		if (isEmpty()) {
			ConcisePlusSet res = convert(c);
			if (res == c)
				return res.clone();
			return res;
		}
		final ConcisePlusSet other = convert(c);
		
		// allocate a space that likely contains the result
		ConcisePlusSet res = new ConcisePlusSet();
		res.words = new int[Math.max(lastWordIndex, other.lastWordIndex) + 1];

		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!res.appendAll(thisIterator, otherIterator.currentPrefix)) {
					res.appendAll(otherIterator, Integer.MAX_VALUE);
					break mainLoop;
				}
				if (!res.appendAll(otherIterator, thisIterator.currentPrefix)) {
					res.appendAll(thisIterator, Integer.MAX_VALUE);
					break mainLoop;
				}
			}

			// perform the union
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// convert both "this" and "other" to a literal
				res.appendLiteral((1 << thisIterator.currentWordValue) | (1 << otherIterator.currentWordValue), thisIterator.currentPrefix);
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				res.appendLiteral((1 << thisIterator.currentWordValue) | otherIterator.currentWordValue, thisIterator.currentPrefix);
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				res.appendLiteral(thisIterator.currentWordValue | (1 << otherIterator.currentWordValue), thisIterator.currentPrefix);
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res.appendLiteral(thisIterator.currentWordValue | otherIterator.currentWordValue, thisIterator.currentPrefix);
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// append remaining words
		if (thisIterator.hasNext()) {
			thisIterator.next();
			res.appendAll(thisIterator, Integer.MAX_VALUE);
		}
		if (otherIterator.hasNext()) {
			otherIterator.next();
			res.appendAll(otherIterator, Integer.MAX_VALUE);
		}

		// final result
		res.updateLast();
		res.size = -1;
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConcisePlusSet empty() {
		return new ConcisePlusSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public ConcisePlusSet symmetricDifference(IntSet c) {
		if (c == null || c.isEmpty())
			return clone();
		if (c == this)
			return empty();
		
		final ConcisePlusSet other = convert(c);

		if (isEmpty())
			return other.clone();
		
		// allocate a space that likely contains the result
		ConcisePlusSet res = new ConcisePlusSet();
		res.words = new int[Math.max(lastWordIndex, other.lastWordIndex) + 1];

		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!res.appendAll(thisIterator, otherIterator.currentPrefix)) {
					res.appendAll(otherIterator, Integer.MAX_VALUE);
					break mainLoop;
				}
				if (!res.appendAll(otherIterator, thisIterator.currentPrefix)) {
					res.appendAll(thisIterator, Integer.MAX_VALUE);
					break mainLoop;
				}
			}

			// perform the symmetric difference 
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// convert both "this" and "other" to a literal
				res.appendLiteral((1 << thisIterator.currentWordValue) ^ (1 << otherIterator.currentWordValue), thisIterator.currentPrefix);
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				res.appendLiteral((1 << thisIterator.currentWordValue) ^ otherIterator.currentWordValue, thisIterator.currentPrefix);
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				res.appendLiteral(thisIterator.currentWordValue ^ (1 << otherIterator.currentWordValue), thisIterator.currentPrefix);
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res.appendLiteral(thisIterator.currentWordValue ^ otherIterator.currentWordValue, thisIterator.currentPrefix);
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// append remaining words
		if (thisIterator.hasNext()) {
			thisIterator.next();
			res.appendAll(thisIterator, Integer.MAX_VALUE);
		}
		if (otherIterator.hasNext()) {
			otherIterator.next();
			res.appendAll(otherIterator, Integer.MAX_VALUE);
		}

		// final result
		res.updateLast();
		res.size = -1;
		return res;
	}

	/**
	 * Refreshes the value of {@link #last}
	 */
	private void updateLast() {
		if (lastWordIndex < 0) {
			clear();
		} else if (lastMarkerIndex == lastWordIndex) {
			last = words[lastWordIndex];
		} else {
			final int marker = words[lastMarkerIndex];
			final int prefix = (marker & PREFIX_MASK) + ((marker & LITERAL_MASK) << LITERAL_MASK_SIZE);
			last = prefix + (WORD_SIZE - 1) - Integer.numberOfLeadingZeros(words[lastWordIndex]);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		if (size < 0) {
			size = 0;
			WordIterator itr = new WordIterator();
			while (itr.hasNext()) {
				itr.next();
				if (itr.isSingleValue)
					size++;
				else
					size += Integer.bitCount(itr.currentWordValue);
			}
		}
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return last;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int first() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return words[0] >= 0 ? words[0] : (words[0] & PREFIX_MASK) + Integer.numberOfTrailingZeros(words[1]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int get(int i) {
		//TODO
		throw new UnsupportedOperationException("TODO");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(int i) {
		//TODO
		throw new UnsupportedOperationException("TODO");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConcisePlusSet complemented() {
		if (isEmpty())
			return new ConcisePlusSet();
		
		// result
		ConcisePlusSet res = new ConcisePlusSet();
		
		// final size
		res.size = last - size() + 1;
		
		// iterate over this set
		WordIterator itr = new WordIterator();
		int prefix = 0;
		while (itr.hasNext()) {
			// current non-empty word
			itr.next();
			
			// fill the gap with "1"
			while (prefix < itr.currentPrefix) {
				res.appendLiteral(FILLED_LITERAL, prefix);
				prefix += WORD_SIZE;
			}
			
			// append the complemented literal
			if (itr.hasNext())
				res.appendLiteral(~itr.currentLiteral(), prefix);
			else
				res.appendLiteral((FILLED_LITERAL >>> -(last + 1)) & ~itr.currentLiteral(), prefix);
			prefix += WORD_SIZE;
		}
		
		res.updateLast();
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		replace(complemented());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(IntSet o) {
		//TODO
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(int from, int to) {
		if (from > to)
			throw new IndexOutOfBoundsException("from: " + from + " > to: " + to);
		
		// trivial case
		if (from == to) {
			add(from);
			return;
		}

		// prefix range
		int fromPrefix = from & PREFIX_MASK;
		int toPrefix = to & PREFIX_MASK;

		// if the prefix range is outside the maximum prefix, we can directly
		// append literal words
		ConcisePlusSet toAdd = fromPrefix > last ? this : new ConcisePlusSet();
		
		// prepare words to add
		int firstWordMask = FILLED_LITERAL << (from);
		int lastWordMask = FILLED_LITERAL >>> -(to + 1);
		if (fromPrefix == toPrefix) {
			// Case 1: One literal word
			toAdd.appendLiteral(firstWordMask & lastWordMask, fromPrefix);
		} else {
			// Case 2: Multiple literal words
			// handle first word
			toAdd.appendLiteral(firstWordMask, fromPrefix);

			// handle intermediate literal words, if any
			for (int p = fromPrefix + WORD_SIZE; p < toPrefix; p += WORD_SIZE)
				toAdd.appendLiteral(FILLED_LITERAL, p);

			// handle last literal word
			toAdd.appendLiteral(lastWordMask, toPrefix);
		}
		
		if (toAdd.size >= 0)
			toAdd.size += to - from + 1;
		toAdd.last = to;
		if (toAdd != this)
			replace(union(toAdd));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(int from, int to) {
		ConcisePlusSet toRemove = new ConcisePlusSet();
		toRemove.fill(from, to);
		replace(difference(toRemove));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        int h = 1;
        for (int i = 0; i <= lastWordIndex; i++) 
            h = (h << 5) - h + words[i]; // h = h * 31 + words[i]
        return h;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ConcisePlusSet))
			return false;
		
		ConcisePlusSet other = (ConcisePlusSet) obj;
		if (last != other.last)
			return false;
		if (!isEmpty())
	        for (int i = 0; i <= lastWordIndex; i++)
	            if (words[i] != other.words[i])
	                return false;
		return true;
	}

	/**
	 * Replace this set with the content of another set
	 * 
	 * @param other
	 *            the other set
	 */
	private void replace(ConcisePlusSet other) {
		words = other.words;
		last = other.last;
		lastMarkerIndex = other.lastMarkerIndex;
		lastWordIndex = other.lastWordIndex;
		size = other.size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO avoid the use of replace()
	public boolean add(int i) {
		// check if it can be an append operation
		if (i > last) {
			append(i);
			return true;
		}
		if (i == last) 
			return false;
		
		// find the word that contains the element
		WordIterator itr = new WordIterator();
		final int prefix = i & PREFIX_MASK;
		if (!itr.skipAllBefore(prefix))
			throw new RuntimeException("unexpected error");

		// check if the element already exists
		if (itr.currentPrefix == prefix) {
			if (itr.isSingleValue) {
				if (itr.currentWordValue == i)
					return false;
				replace(union(convert(i)));
				return true;
			}
				
			words[itr.currentWordIndex] |= 1 << i;
			if (itr.currentWordValue != words[itr.currentWordIndex]) {
				if (size >= 0)
					size++;
				return true;
			}
			return false;
		}
		
		// new prefix
		replace(union(convert(i)));
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO avoid the use of replace()
	public boolean remove(int i) {
		// check if the element does not exist
		if (i > last)
			return false;

		// special case of last element
		if (i == last) {
			// check if it is a singleton
			if (lastWordIndex == 0) 
				clear();
			else
				// we need to update the last marker...
				replace(difference(convert(i)));
			return true;
		}
		
		// find the word that would contain the element
		WordIterator itr = new WordIterator();
		final int prefix = i & PREFIX_MASK;
		if (!itr.skipAllBefore(prefix))
			throw new RuntimeException("unexpected error");
		
		// the element does not exist
		if (itr.currentPrefix > prefix)
			return false;
		
		// single-value marker
		if (itr.isSingleValue) {
			// check whether the element does exist
			if (itr.currentWordValue != i)
				return false;
			
			// remove the marker and shift remaining words
			System.arraycopy(
					words, itr.currentWordIndex + 1, 
					words, itr.currentWordIndex, 
					lastWordIndex - itr.currentWordIndex);
			words[lastWordIndex] = 0;
			lastWordIndex--;
			lastMarkerIndex--;
			if (size >= 0)
				size--;
			return true;
		}
		
		// clear the single bit
		words[itr.currentWordIndex] &= ~(1 << i);
		if (itr.currentWordValue == words[itr.currentWordIndex]) 
			return false;
		if (Integer.bitCount(words[itr.currentWordIndex]) > 1) {
			if (size >= 0)
				size--;
			return true;
		}
		
		// we need to update the marker and the following words...
		words[itr.currentWordIndex] |= 1 << i;
		replace(difference(convert(i)));
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(IntSet other) {
		ConcisePlusSet res = union(other);
		if (equals(res))
			return false;
		replace(res);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(IntSet other) {
		ConcisePlusSet res = difference(other);
		if (equals(res))
			return false;
		replace(res);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(IntSet other) {
		ConcisePlusSet res = intersection(other);
		if (equals(res))
			return false;
		replace(res);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(int i) {
		if (isEmpty() || i > last || i < 0)
			return false;
		
		int prefix = i & PREFIX_MASK;
		WordIterator itr = new WordIterator();
		if (!itr.skipAllBefore(prefix) || itr.currentPrefix > prefix)
			return false;
		if (itr.isSingleValue)
			return itr.currentWordValue == i;
		return (itr.currentWordValue & (1 << i)) != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO avoid the use of replace()
	public void flip(int i) {
		if (isEmpty() || i > last) {
			append(i);
			return;
		}
		if (i == last) {
			// check if it is a singleton
			if (lastWordIndex == 0) 
				clear();
			else
				// we need to update the last marker...
				replace(difference(convert(i)));
			return;
		}
		
		// check if it does exist
		int prefix = i & PREFIX_MASK;
		WordIterator itr = new WordIterator();
		if (!itr.skipAllBefore(prefix) || itr.currentPrefix > prefix) {
			replace(union(convert(i)));
			return;
		}
		
		// single-value marker
		if (itr.isSingleValue) {
			if (itr.currentWordValue == i) {
				// remove the marker and shift remaining words
				System.arraycopy(
						words, itr.currentWordIndex + 1, 
						words, itr.currentWordIndex, 
						lastWordIndex - itr.currentWordIndex);
				words[lastWordIndex] = 0;
				lastWordIndex--;
				lastMarkerIndex--;
				if (size >= 0)
					size--;
			} else {
				// add the element
				replace(union(convert(i)));
			}
			return;
		}
		
		// the new element is contained within a literal...
		if ((itr.currentWordValue & (1 << i)) != 0) {
			// remove
			int r = words[itr.currentWordIndex] & ~(1 << i);
			if (r == 0 || containsOnlyOneBit(r)) {
				replace(difference(convert(i)));
			} else {
				words[itr.currentWordIndex] = r;
				if (size >= 0)
					size--;
			}
		} else {
			// add
			words[itr.currentWordIndex] |= 1 << i;
			if (size >= 0)
				size++;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public boolean containsAll(IntSet c) {
		// useless to intersect empty sets
		if (c == null || c.isEmpty() || this == c)
			return true;
		if (isEmpty())
			return false;
		
		final ConcisePlusSet other = convert(c);
		if (last < other.last
				|| other.last < (words[0] & PREFIX_MASK))
			return false;
		
		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (thisIterator.currentPrefix > otherIterator.currentPrefix) 
					return false;
				if (!thisIterator.skipAllBefore(otherIterator.currentPrefix))
					break mainLoop;
			}
				
			// perform the intersection
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				if (thisIterator.currentWordValue != otherIterator.currentWordValue)
					return false;
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				int t = otherIterator.currentWordValue;
				if (t != ((1 << thisIterator.currentWordValue) & t))
					return false;
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				int t = 1 << otherIterator.currentWordValue;
				if (t != (thisIterator.currentWordValue & t))
					return false;
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					if (otherIterator.currentWordValue != (thisIterator.currentWordValue & otherIterator.currentWordValue))
						return false;
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// final result
		return !otherIterator.hasNext();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public boolean containsAny(IntSet c) {
		if (c == null || c.isEmpty() || c == this)
			return true;
		if (isEmpty())
			return false;
		
		final ConcisePlusSet other = convert(c);
		if (other.last < (words[0] & PREFIX_MASK))
			return false;
		
		// iterate over the two sets
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!thisIterator.skipAllBefore(otherIterator.currentPrefix))
					break mainLoop;
				if (!otherIterator.skipAllBefore(thisIterator.currentPrefix))
					break mainLoop;
			}
				
			// perform the intersection
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				if (thisIterator.currentWordValue == otherIterator.currentWordValue)
					return true;
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				if (((1 << thisIterator.currentWordValue) & otherIterator.currentWordValue) != 0)
					return true;
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				if ((thisIterator.currentWordValue & (1 << otherIterator.currentWordValue)) != 0)
					return true;
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					if ((thisIterator.currentWordValue & otherIterator.currentWordValue) != 0)
						return true;
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// final result
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	//TODO special case other.lastMarkerIndex == other.lastWordIndex or this.lastMarkerIndex == this.lastWordIndex
	public boolean containsAtLeast(IntSet c, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		if ((size >= 0 && size < minElements) || c == null || c.isEmpty() || isEmpty())
			return false;
		if (this == c)
			return size() >= minElements;

		final ConcisePlusSet other = convert(c);

		if (last < (other.words[0] & PREFIX_MASK)
				|| other.last < (words[0] & PREFIX_MASK))
			return false;
		
		// iterate over the two sets
		int res = 0;
		WordIterator thisIterator = new WordIterator();
		WordIterator otherIterator = other.new WordIterator();
		mainLoop:
		do {
			// identify two words with the same prefix
			thisIterator.next();
			otherIterator.next();
			while (thisIterator.currentPrefix != otherIterator.currentPrefix) {
				if (!thisIterator.skipAllBefore(otherIterator.currentPrefix))
					break mainLoop;
				if (!otherIterator.skipAllBefore(thisIterator.currentPrefix))
					break mainLoop;
			}
				
			// perform the intersection
			if (thisIterator.isSingleValue && otherIterator.isSingleValue) {
				// both "this" and "other" are single-value markers
				if (thisIterator.currentWordValue == otherIterator.currentWordValue)
					if (++res >= minElements)
						return true;
			} else if (thisIterator.isSingleValue) {
				// convert "this" to a literal
				if (((1 << thisIterator.currentWordValue) & otherIterator.currentWordValue) != 0)
					if (++res >= minElements)
						return true;
			} else if (otherIterator.isSingleValue) {
				// convert "other" to a literal
				if ((thisIterator.currentWordValue & (1 << otherIterator.currentWordValue)) != 0)
					if (++res >= minElements)
						return true;
			} else {
				// both literals
				int i = Math.min(thisIterator.remainingLiterals, otherIterator.remainingLiterals);
				while(true) {
					res += Integer.bitCount(thisIterator.currentWordValue & otherIterator.currentWordValue);
					if (res >= minElements)
						return true;
					if (i-- == 0)
						break;
					thisIterator.fastNext();
					otherIterator.fastNext();
				}
			}
		} while (thisIterator.hasNext() && otherIterator.hasNext());

		// final result
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) (lastWordIndex + 1) / ((last >>> WORD_LOGB) + 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) (lastWordIndex + 1) / size();
	}

	/**
	 * Generates the {@link #WORD_SIZE}-bit binary representation of a given
	 * word
	 * 
	 * @param word
	 *            word to represent
	 * @return {@link #WORD_SIZE}-character string that represents the given
	 *         word
	 */
	private static String toBinaryString(int word) {
		String lsb = Integer.toBinaryString(word);
		StringBuilder pad = new StringBuilder();
		for (int i = lsb.length(); i < WORD_SIZE; i++)
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

		// empty set
		if (isEmpty())
			return s.append("empty set\n").toString();

		// all integers
		f.format("Integers: %s\n", toString());

		// iterate over words
		int literals = -1;
		int prefix = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			// current word
			final int w = words[i];
			String ws = toBinaryString(w);
			f.format("words[%d] = [%s] = ", i, ws);
			
			// word type
			if (literals < 0) {
				// raw representation
				s.append('[');
				s.append(ws.substring(0, 1));
				s.append("][");
				s.append(ws.substring(1, 27));
				s.append("][");
				s.append(ws.substring(27));
				s.append("] = ");

				if (w >= 0) {
					s.append(w);
				} else {
					prefix = w & PREFIX_MASK;
					literals = w & LITERAL_MASK;
					s.append("prefix: ");
					s.append(prefix);
					s.append(", literals: ");
					s.append(literals);
					s.append(" (+1)");
				}
			} else {
				int bit = 0;
				while (bit < WORD_SIZE) {
					bit = Integer.numberOfTrailingZeros(w & (FILLED_LITERAL << bit));
					if (bit < WORD_SIZE) {
						s.append(prefix + bit);
						bit++;
						s.append(", ");
					} else {
						s.delete(s.length() - 2, s.length());
					}
				}
				literals--;
				prefix += WORD_SIZE;
			}
			s.append('\n');
		}
		
		// object attributes
		f.format("last: %d\n", last);
		f.format("size: %s\n", (size == -1 ? "*" : Integer.toString(size)));
		f.format("words.length: %d\n", words.length);
		f.format("lastWordIndex: %d\n", lastWordIndex);
		f.format("lastMarkerIndex: %d\n", lastMarkerIndex);

		// compression
		f.format("bitmap compression: %.4f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.4f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IntIterator descendingIterator() {
		return new ReverseIntegerIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IntIterator iterator() {
		return new IntegerIterator();
	}
}

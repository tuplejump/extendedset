/* (c) 2010 Alessandro Colantonio
 * <mailto:colanton@mat.uniroma3.it>
 * <http://ricerca.mat.uniroma3.it/users/colanton>
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */ 

package it.uniroma3.mat.extendedset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * This is CONCISE: COmpressed 'N' Composable Integer Set.
 * <p>
 * This class is a {@link SortedSet} of integers that are internally represented
 * by compressed bitmaps though a RLE (Run-Length Encoding) compression
 * algorithm. See <a
 * href="http://arxiv.org/abs/1004.0403">http://arxiv.org/abs/1004.0403</a> for
 * more details.
 * <p>
 * The RLE compression method is mainly inspired by WAH (<i>Word-Aligned
 * Hybrid</i> compression). However, CONCISE allows for a better compression for
 * sparse data. The memory footprint required by a representation of
 * <code>n</code> elements is at most the same as an array of <code>n + 1</code>
 * elements. In WAH, this requires an array of size <code>2 * n</code> elements.
 * <p>
 * Notice that the returned iterator is <i>fail-fast</i>, similar to most
 * {@link Collection}-derived classes. If the set is structurally modified at
 * any time after the iterator is created, the iterator will throw a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * The iterator throws a {@link ConcurrentModificationException} on a
 * best-effort basis. Therefore, it would be wrong to write a program that
 * depended on this exception for its correctness: <i>the fail-fast behavior of
 * iterators should be used only to detect bugs.</i>
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @see ExtendedSet
 * @see AbstractExtendedSet
 * @see FastSet
 * @see IndexedSet
 */
public class ConciseSet extends AbstractExtendedSet<Integer> implements
		SortedSet<Integer>, Cloneable {
	/**
	 * This is the compressed bitmap, that is a collection of words. For each
	 * word:
	 * <ul>
	 * <li> <tt>1* (0x80000000)</tt> means that it is a 31-bit <i>literal</i>.
	 * <li> <tt>00* (0x00000000)</tt> indicates a <i>sequence</i> made up of at
	 * most one set bit in the first 31 bits, and followed by blocks of 31 0's.
	 * The following 5 bits (<tt>00xxxxx*</tt>) indicates which is the set bit (
	 * <tt>00000</tt> = no set bit, <tt>00001</tt> = LSB, <tt>11111</tt> = MSB),
	 * while the remaining 25 bits indicate the number of following 0's blocks.
	 * <li> <tt>01* (0x40000000)</tt> indicates a <i>sequence</i> made up of at
	 * most one <i>un</i>set bit in the first 31 bits, and followed by blocks of
	 * 31 1's. (see the <tt>00*</tt> case above).
	 * </ul>
	 * <p>
	 * Note that literal words 0xFFFFFFFF and 0x80000000 are allowed, thus
	 * zero-length sequences (i.e., such that getSequenceCount() == 0) cannot
	 * exists.
	 */
	private int[] words;

	/**
	 * Most significant set bit within the uncompressed bit string.
	 */
	private int last;

	/**
	 * Cached cardinality of the bit-set. Defined for efficient {@link #size()}
	 * calls. When -1, the cache is invalid.
	 */ 
	private int size;

	/**
	 * Index of the last word in {@link #words}
	 */ 
	private int lastWordIndex;

	/**
	 * <code>true</code> if the class must simulate the behavior of WAH
	 */
	private final boolean simulateWAH;
	
	/**
	 * User for <i>fail-fast</i> iterator. It counts the number of operations
	 * that <i>do</i> modify {@link #words}
	 */
	protected int modCount = 0;

	/**
	 * The highest representable integer.
	 * <p>
	 * Its value is computed as follows. The number of bits required to
	 * represent the longest sequence of 0's or 1's is
	 * <tt>ceil(log<sub>2</sub>(({@link Integer#MAX_VALUE} - 31) / 31)) = 27</tt>.
	 * Indeed, at least one literal exists, and the other bits may all be 0's or
	 * 1's, that is <tt>{@link Integer#MAX_VALUE} - 31</tt>. If we use:
	 * <ul>
	 * <li> 2 bits for the sequence type; 
	 * <li> 5 bits to indicate which bit is set;
	 * </ul>
	 * then <tt>32 - 5 - 2 = 25</tt> is the number of available bits to
	 * represent the maximum sequence of 0's and 1's. Thus, the maximal bit that
	 * can be set is represented by a number of 0's equals to
	 * <tt>31 * (1 << 25)</tt>, followed by a literal with 30 0's and the
	 * MSB (31<sup>st</sup> bit) equal to 1
	 */
	public final static int MAX_ALLOWED_INTEGER = 31 * (1 << 25) + 30;

	/** 
	 * The lowest representable integer.
	 */
	public final static int MIN_ALLOWED_SET_BIT = 0;
	
	/** 
	 * Maximum number of representable bits within a literal
	 */
	private final static int MAX_LITERAL_LENGHT = 31;

	/**
	 * Literal that represents all bits set to 1 (and MSB = 1)
	 */
	private final static int ALL_ONES_LITERAL = 0xFFFFFFFF;
	
	/**
	 * Literal that represents all bits set to 0 (and MSB = 1)
	 */
	private final static int ALL_ZEROS_LITERAL = 0x80000000;
	
	/**
	 * All bits set to 1 and MSB = 0
	 */
	private final static int ALL_ONES_WITHOUT_MSB = 0x7FFFFFFF;
	
	/**
	 * All bits set to 0 and MSB = 0
	 */
	private final static int ALL_ZEROS_WITHOUT_MSB = 0x00000000;

	/**
	 * Resets to an empty set
	 * 
	 * @see #ConciseSet()
	 * {@link #clear()}
	 */
	private void reset() {
		modCount++;
		words = null;
		last = -1;
		size = 0;
		lastWordIndex = -1;
	}

	/**
	 * Creates an empty integer set
	 */
	public ConciseSet() {
		this(false);
	}

	/**
	 * Creates an empty integer set
	 * 
	 * @param simulateWAH
	 *            <code>true</code> if the class must simulate the behavior of
	 *            WAH
	 */
	public ConciseSet(boolean simulateWAH) {
		this.simulateWAH = simulateWAH;
		reset();
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it from
	 * an existing integer collection
	 * 
	 * @param c
	 */
	public ConciseSet(Collection<? extends Integer> c) {
		this(false, c);
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it from
	 * an existing integer collection
	 * 
	 * @param simulateWAH
	 *            <code>true</code> if the class must simulate the behavior of
	 *            WAH
	 * @param c
	 */
	public ConciseSet(boolean simulateWAH, Collection<? extends Integer> c) {
		this(simulateWAH);

		// try to convert the collection
		if (c != null && !c.isEmpty()) {
			// sorted element (in order to use the append() method)
			Collection<? extends Integer> elements;
			if ((c instanceof SortedSet<?>) && (((SortedSet<?>) c).comparator() == null)) {
				// if elements are already ordered according to the natural
				// order of Integer, simply use them
				elements = c;
			} else {
				// sort elements in ascending order
				elements = new ArrayList<Integer>(c);
				Collections.sort((List<?>) elements, null);
			}

			// append elements
			for (Integer i : elements)
				// check for duplicates
				if (last != i.intValue())
					append(i.intValue());
		}	
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it from
	 * an existing integer collection
	 * 
	 * @param a
	 */
	public ConciseSet(Object... a) {
		this(false, a);
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it from
	 * an existing integer collection
	 * 
	 * @param simulateWAH
	 *            <code>true</code> if the class must simulate the behavior of
	 *            WAH
	 * @param a
	 */
	@SuppressWarnings("unchecked")
	public ConciseSet(boolean simulateWAH, Object... a) {
		this(simulateWAH, a == null ? (Collection) null : Arrays.asList(a));
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it with
	 * the given integer
	 * 
	 * @param a
	 */
	public ConciseSet(int a) {
		this(false, a);
	}
	
	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it with
	 * the given integer
	 * 
	 * @param simulateWAH
	 *            <code>true</code> if the class must simulate the behavior of
	 *            WAH
	 * @param a
	 */
	public ConciseSet(boolean simulateWAH, int a) {
		this(simulateWAH);
		append(a);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet clone() {
		if (isEmpty())
			return empty();

		// NOTE: do not use super.clone() since it is 10 times slower!
		ConciseSet res = new ConciseSet(simulateWAH);
		res.last = last;
		res.lastWordIndex = lastWordIndex;
		res.modCount = 0;
		res.size = size;
		res.words = Arrays.copyOf(words, lastWordIndex + 1);
		return res;
	}

	/**
	 * Calculates the modulus division by 31 in a faster way than using <code>n % 31</code>
	 * <p>
	 * This method of finding modulus division by an integer that is one less
	 * than a power of 2 takes at most <tt>O(lg(32))</tt> time. The number of operations
	 * is at most <tt>12 + 9 * ceil(lg(32))</tt>.
	 * <p>
	 * See <a
	 * href="http://graphics.stanford.edu/~seander/bithacks.html">http://graphics.stanford.edu/~seander/bithacks.html</a>
	 * 
	 * @param n
	 *            number to divide
	 * @return <code>n % 31</code>
	 */
	private static int maxLiteralLengthModulus(int n) {
		int m = (n & 0xc1f07c1f) + ((n >>> 5) & 0xc1f07c1f);
		m = (m >>> 15) + (m & 0x00007fff);
		if (m <= 31)
			return m == 31 ? 0 : m;
		m = (m >>> 5) + (m & 0x0000001f);
		if (m <= 31)
			return m == 31 ? 0 : m;
		m = (m >>> 5) + (m & 0x0000001f);
		if (m <= 31)
			return m == 31 ? 0 : m;
		m = (m >>> 5) + (m & 0x0000001f);
		if (m <= 31)
			return m == 31 ? 0 : m;
		m = (m >>> 5) + (m & 0x0000001f);
		if (m <= 31)
			return m == 31 ? 0 : m;
		m = (m >>> 5) + (m & 0x0000001f);
		return m == 31 ? 0 : m;
	}

	/**
	 * Calculates the multiplication by 31 in a faster way than using <code>n * 31</code>
	 * 
	 * @param n
	 *            number to multiply
	 * @return <code>n * 31</code>
	 */
	private static int maxLiteralLengthMultiplication(int n) {
		return (n << 5) - n;
	}

	/**
	 * Calculates the division by 31
	 * 
	 * @param n
	 *            number to divide
	 * @return <code>n / 31</code>
	 */
	private static int maxLiteralLengthDivision(int n) {
		return n / 31;
	}

	/**
	 * Checks whether a word is a literal one
	 * 
	 * @param word
	 *            word to check
	 * @return <code>true</code> if the given word is a literal word
	 */
	private static boolean isLiteral(int word) {
		// "word" must be 1*
		// NOTE: this is faster than "return (word & 0x80000000) == 0x80000000"
		return (word & 0x80000000) != 0;
	}

	/**
	 * Checks whether a word contains a sequence of 1's
	 * 
	 * @param word
	 *            word to check
	 * @return <code>true</code> if the given word is a sequence of 1's
	 */
	private static boolean isOneSequence(int word) {
		// "word" must be 01*
		return (word & 0xC0000000) == 0x40000000;
	}

	/**
	 * Checks whether a word contains a sequence of 0's
	 * 
	 * @param word
	 *            word to check
	 * @return <code>true</code> if the given word is a sequence of 0's
	 */
	private static boolean isZeroSequence(int word) {
		// "word" must be 00*
		return (word & 0xC0000000) == 0x00000000;
	}

	/**
	 * Checks whether a word contains a sequence of 0's with no set bit, or 1's
	 * with no unset bit.
	 * <p>
	 * <b>NOTE:</b> when {@link #simulateWAH} is <code>true</code>, it is
	 * equivalent to (and as fast as) <code>!</code>{@link #isLiteral(int)}
	 * 
	 * @param word
	 *            word to check
	 * @return <code>true</code> if the given word is a sequence of 0's or 1's
	 *         but with no (un)set bit
	 */
	private static boolean isSequenceWithNoBits(int word) {
		// "word" must be 0?00000*
		return (word & 0xBE000000) == 0x00000000;
	}
	
	/**
	 * Gets the number of blocks of 1's or 0's stored in a sequence word
	 * 
	 * @param word
	 *            word to check
	 * @return the number of blocks that follow the first block of 31 bits
	 */
	private static int getSequenceCount(int word) {
		// get the 25 LSB bits
		return word & 0x01FFFFFF;
	}

	/**
	 * Clears the (un)set bit in a sequence
	 * 
	 * @param word
	 *            word to check
	 * @return the sequence corresponding to the given sequence and with no
	 *         (un)set bits
	 */
	private static int getSequenceWithNoBits(int word) {
		// clear 29 to 25 LSB bits
		return (word & 0xC1FFFFFF);
	}
	
	/**
	 * Gets the literal word that represents the first 31 bits of the given the
	 * word (i.e. the first block of a sequence word, or the bits of a literal word).
	 * <p>
	 * If the word is a literal, it returns the unmodified word. In case of a
	 * sequence, it returns a literal that represents the first 31 bits of the
	 * given sequence word.
	 * 
	 * @param word
	 *            word to check
	 * @return the literal contained within the given word, <i>with the most
	 *         significant bit set to 1</i>.
	 */
	private /*static*/ int getLiteral(int word) {
		if (isLiteral(word)) 
			return word;
		
		if (simulateWAH)
			return isZeroSequence(word) ? ALL_ZEROS_LITERAL  : ALL_ONES_LITERAL;

		// get bits from 30 to 26 and use them to set the corresponding bit
		// NOTE: "1 << (word >>> 25)" and "1 << ((word >>> 25) & 0x0000001F)" are equivalent
		// NOTE: ">>> 1" is required since 00000 represents no bits and 00001 the LSB bit set
		int literal = (1 << (word >>> 25)) >>> 1;  
		return isZeroSequence(word) 
				? (ALL_ZEROS_LITERAL | literal) 
				: (ALL_ONES_LITERAL & ~literal);
	}

	/**
	 * Gets the position of the flipped bit within a sequence word. If the
	 * sequence has no set/unset bit, returns -1.
	 * <p>
	 * Note that the parameter <i>must</i> a sequence word, otherwise the
	 * result is meaningless.
	 * 
	 * @param word
	 *            sequence word to check
	 * @return the position of the set bit, from 0 to 31. If the sequence has no
	 *         set/unset bit, returns -1.
	 */
	private static int getFlippedBit(int word) {
		// get bits from 30 to 26
		// NOTE: "-1" is required since 00000 represents no bits and 00001 the LSB bit set
		return ((word >>> 25) & 0x0000001F) - 1;  
	}

	/**
	 * Generates a sequence word.
	 * 
	 * @param bitPosition
	 *            position of the (un)set bit (0 = it does not exist, 1 = LSB,
	 *            31 = MSB)
	 * @param isOneSequence
	 *            <code>false</code> if it is a literal of all 0's, with at
	 *            most one set bit and followed by blocks of 0's,
	 *            <code>true</code> if it is a literal of all 1's, with at
	 *            most one unset bit and followed by blocks of 1's
	 * @param numberOfSubsequentBlocks
	 *            number of blocks made up of 31 0's or 1's after the first
	 *            literal of the sequence
	 * @return the generated sequence word
	 */
	private /*static*/ int makeSequenceWord(
			int bitPosition, 
			boolean isOneSequence, 
			int numberOfSubsequentBlocks) {
		if (simulateWAH)
			return (isOneSequence ? 0x40000000 : 0x00000000)
			    | numberOfSubsequentBlocks;
		return (isOneSequence ? 0x40000000 : 0x00000000)
		    | numberOfSubsequentBlocks 
			| (bitPosition << 25);
	}
	
	/**
	 * Gets the number of set bits within the literal word
	 * 
	 * @param word
	 *            literal word
	 * @return the number of set bits within the literal word
	 */
	private static int getLiteralBitCount(int word) {
		return Integer.bitCount(getLiteralBits(word));
	}

	/**
	 * Gets the bits contained within the literal word
	 * 
	 * @param word literal word
	 * @return the literal word with the most significant bit cleared
	 */
	private static int getLiteralBits(int word) {
		return ALL_ONES_WITHOUT_MSB & word;
	}

	/**
	 * Clears bits from MSB (excluded, since it indicates the word type) to the
	 * specified bit (excluded). Last word is supposed to be a literal one.
	 * 
	 * @param lastSetBit
	 *            leftmost bit to preserve
	 */
	private void clearBitsAfterInLastWord(int lastSetBit) {
		words[lastWordIndex] &= ALL_ZEROS_LITERAL | (0xFFFFFFFF >>> (31 - lastSetBit));
	}

	/**
	 * Returns <code>true</code> when the given 31-bit literal string (namely,
	 * with MSB set) contains only one set bit
	 * 
	 * @param literal
	 *            literal word (namely, with MSB unset)
	 * @return <code>true</code> when the given literal contains only one set
	 *         bit
	 */
	private static boolean containsOnlyOneBit(int literal) {
		return (literal & (literal - 1)) == 0;
	}
	
	/**
	 * Sets the bit at the given absolute position within the uncompressed bit
	 * string. The bit <i>must</i> be appendable, that is it must represent an
	 * integer that is strictly greater than the maximum integer in the set.
	 * Note that the parameter range check is performed by the public method
	 * {@link #add(Integer)} and <i>not</i> in this method.
	 * <p>
	 * <b>NOTE:</b> This method assumes that the last element of {@link #words}
	 * (i.e. <code>getLastWord()</code>) <i>must</i> be one of the
	 * following:
	 * <ul>
	 * <li> a literal word with <i>at least one</i> set bit;
	 * <li> a sequence of ones.
	 * </ul>
	 * Hence, the last word in {@link #words} <i>cannot</i> be:
	 * <ul>
	 * <li> a literal word containing only zeros;
	 * <li> a sequence of zeros.
	 * </ul>
	 * 
	 * @param i
	 *            the absolute position of the bit to set (i.e., the integer to add) 
	 */
	private void append(int i) {
		// special case of empty set
		if (isEmpty()) {
			int zeroBlocks = maxLiteralLengthDivision(i);
			if (zeroBlocks == 0) {
				words = new int[1];
				lastWordIndex = 0;
			} else if (!simulateWAH && zeroBlocks == 1) {
				words = new int[2];
				lastWordIndex = 1;
				words[0] = ALL_ZEROS_LITERAL;
			} else {
				words = new int[2];
				lastWordIndex = 1;
				words[0] = zeroBlocks - 1;
			}
			last = i;
			size = 1;
			words[lastWordIndex] = ALL_ZEROS_LITERAL | (1 << maxLiteralLengthModulus(i));
			return;
		}
		
		// position of the next bit to set within the current literal
		int bit = maxLiteralLengthModulus(last) + i - last;

		// if we are outside the current literal, add zeros in
		// between the current word and the new 1-bit literal word
		if (bit >= MAX_LITERAL_LENGHT) {
			int zeroBlocks = maxLiteralLengthDivision(bit) - 1;
			if (zeroBlocks == 0) {
				// just add a new literal word to set the new bit
				lastWordIndex++;
				ensureCapacity();
			} else {
				// add a 0's sequence before the new literal
				int w = words[lastWordIndex];
				if (!simulateWAH && containsOnlyOneBit(getLiteralBits(w))) {
					// the zero blocks can be merged with the previous literal that contains only one set bit
					words[lastWordIndex] = makeSequenceWord(1 + Integer.numberOfTrailingZeros(w), false, zeroBlocks);
					lastWordIndex++;
					ensureCapacity();
				} else {
					// the new zero block cannot be merged with the previous literal
					lastWordIndex += 2;
					ensureCapacity();
					if (zeroBlocks == 1) 
						words[lastWordIndex - 1] = ALL_ZEROS_LITERAL;
					else 
						words[lastWordIndex - 1] = zeroBlocks - 1;
				}
			}

			// prepare the new literal word
			bit = maxLiteralLengthModulus(bit);
			words[lastWordIndex] = ALL_ZEROS_LITERAL;
		}

		// set the new bit
		words[lastWordIndex] |= 1 << bit;
		last = i;
		if (size >= 0)
			size++;
		compress();
	}

	/**
	 * Assures that the length of {@link #words} is sufficient to contain
	 * {@link #lastWordIndex}.
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
	 * Removes unused allocated words at the end of {@link #words}
	 */
	private void compact() {
		if (words != null && lastWordIndex < words.length - 1)
			words = Arrays.copyOf(words, lastWordIndex + 1);
	}

	/**
	 * Iterates over words, from LSB to MSB.
	 * <p>
	 * It iterates over the <i>literals</i> represented by
	 * {@link ConciseSet#words}. In particular, when a word is a sequence, it
	 * "expands" the sequence to all the represented literals. It also maintains
	 * a modified copy of the sequence that stores the number of the remaining
	 * blocks to iterate.
	 */
	private class WordIterator {
		private int currentWordIndex;	// index of the current word
		private int currentWordCopy;	// copy of the current word
		private int currentLiteral;		// literal contained within the current word
		private int remainingWords;		// remaining words from "index" to the end
		
		/*
		 * Initialize data 
		 */
		{
			if (words != null) {
				currentWordIndex = 0;
				currentWordCopy = words[currentWordIndex];
				currentLiteral = getLiteral(currentWordCopy);
				remainingWords = lastWordIndex;
			} else {
				// empty set
				currentWordIndex = 0;
				currentWordCopy = 0;
				currentLiteral = 0;
				remainingWords = -1;
			}
		}

		/**
		 * Checks if the current word represents more than one literal
		 * 
		 * @return <code>true</code> if the current word represents more than
		 *         one literal
		 */
		private boolean hasCurrentWordManyLiterals() {
			/*
			 * The complete statement should be:
			 * 
			 *     return !isLiteral(currentWordCopy) && getSequenceCount(currentWordCopy) > 0;
			 *     
			 * that is equivalent to:
			 * 
			 *     return ((currentWordCopy & 0x80000000) == 0) && (currentWordCopy & 0x01FFFFFF) > 0;
			 *     
			 * and thus equivalent to...
			 */
			return (currentWordCopy & 0x81FFFFFF) > 0;
		}
		
		/**
		 * Checks whether other literals to analyze exist
		 * 
		 * @return <code>true</code> if {@link #currentWordIndex} is out of
		 *         the bounds of {@link #words}
		 */
		public final boolean endOfWords() {
			return remainingWords < 0;
		}

		/**
		 * Checks whether other literals to analyze exist
		 * 
		 * @return <code>true</code> if there are literals to iterate
		 */
		public final boolean hasMoreLiterals() {
			return hasMoreWords() || hasCurrentWordManyLiterals();
		}

		/**
		 * Checks whether other words to analyze exist
		 * 
		 * @return <code>true</code> if there are words to iterate
		 */
		public final boolean hasMoreWords() {
			return remainingWords > 0;
		}
		
		/**
		 * Prepares the next literal {@link #currentLiteral}, increases
		 * {@link #currentWordIndex}, decreases {@link #remainingWords} if
		 * necessary, and modifies the copy of the current word
		 * {@link #currentWordCopy}.
		 */ 
		public final void prepareNextLiteral() {
			if (!hasCurrentWordManyLiterals()) {
				if (remainingWords == -1)
					throw new NoSuchElementException();
				if (remainingWords > 0) 
					currentWordCopy = words[++currentWordIndex];
				remainingWords--;
			} else {
				// decrease the counter and avoid to generate again the 1-bit literal
				if (!simulateWAH)
					currentWordCopy = getSequenceWithNoBits(currentWordCopy);
				currentWordCopy--;
			}
			currentLiteral = getLiteral(currentWordCopy);
		}
	}
	
	/**
	 * Iterates over words, from MSB to LSB.
	 * <p>
	 * @see WordIterator
	 */
	private class ReverseWordIterator {
		private int currentWordIndex;	// index of the current word
		private int currentWordCopy;	// copy of the current word
		private int currentLiteral;		// literal contained within the current word

		/**
		 * Gets the literal word that represents the <i>last</i> 31 bits of the
		 * given the word (i.e. the last block of a sequence word, or the bits
		 * of a literal word).
		 * <p>
		 * If the word is a literal, it returns the unmodified word. In case of
		 * a sequence, it returns a literal that represents the last 31 bits of
		 * the given sequence word.
		 * <p>
		 * Different from {@link ConciseSet#getLiteral(int)}, when the word is
		 * a sequence that contains one (un)set bit, and the count is greater
		 * than zero, then it means that we are traversing the sequence from the
		 * end, and then the literal is represented by all ones or all zeros.
		 * 
		 * @param word
		 *            the word where to extract the literal
		 * @return the literal contained at the end of the given word, with the
		 *         most significant bit set to 1.
		 */
		private int getReverseLiteral(int word) {
			if (simulateWAH || isLiteral(word) || isSequenceWithNoBits(word) || getSequenceCount(word) == 0)
				return getLiteral(word);
			return isZeroSequence(word) ? ALL_ZEROS_LITERAL : ALL_ONES_LITERAL;
		}
		
		/*
		 * Initialize data 
		 */
		{
			if (words != null) {
				currentWordIndex = lastWordIndex;
				currentWordCopy = words[currentWordIndex];
				currentLiteral = getReverseLiteral(currentWordCopy);
			} else {
				// empty set
				currentWordIndex = -1;
				currentWordCopy = 0;
				currentLiteral = 0;
			}
		}
		
		/**
		 * Checks whether other literals to analyze exist
		 * 
		 * @return <code>true</code> if there are literals to iterate
		 */
		public final boolean hasMoreLiterals() {
			if (currentWordIndex > 1)
				return true;
			if (currentWordIndex < 0)
				return false;
			
			// now currentWordIndex == 0 or 1
			if (currentWordIndex == 1) {
				if (isLiteral(currentWordCopy) 
						|| getSequenceCount(currentWordCopy) == 0 
						|| (isZeroSequence(currentWordCopy) && isSequenceWithNoBits(currentWordCopy)))
					return !(words[0] == ALL_ZEROS_LITERAL 
							|| (isZeroSequence(words[0]) && isSequenceWithNoBits(words[0])));
				// I don't have to "jump" to words[0], namely I still have to finish words[1]
				return true;
			} 
			
			// now currentWordIndex == 0, namely the first element
			if (isLiteral(currentWordCopy))
				return false;
			
			// now currentWordCopy is a sequence
			if (getSequenceCount(currentWordCopy) == 0)
				return false;

			// now currentWordCopy is a non-empty sequence
			if (isOneSequence(currentWordCopy))
				return true;

			// now currentWordCopy is a zero sequence
			if (simulateWAH || isSequenceWithNoBits(currentWordCopy))
				return false;
			
			// zero sequence with a set bit at the beginning
			return true;
		}

		/**
		 * Checks whether other words to analyze exist
		 * 
		 * @return <code>true</code> if there are words to iterate
		 */
		public final boolean endOfWords() {
			return currentWordIndex < 0;
		}

		/**
		 * Prepares the next literal, similar to
		 * {@link WordIterator#prepareNextLiteral()}. <b>NOTE:</b> it supposes
		 * that {@link #hasMoreLiterals()} returns <code>true</code>.
		 */ 
		public final void prepareNextLiteral() {
			if (isLiteral(currentWordCopy) || getSequenceCount(currentWordCopy) == 0) {
				if (--currentWordIndex >= 0)
					currentWordCopy = words[currentWordIndex];
				if (currentWordIndex == -2)
					throw new NoSuchElementException();
			} else {
				currentWordCopy--;
			}
			currentLiteral = getReverseLiteral(currentWordCopy);
		}
	}
	
	/**
	 * When both word iterators currently point to sequence words, it decreases
	 * these sequences by the least sequence count between them and return such
	 * a count.
	 * <p>
	 * Conversely, when one of the word iterators does <i>not</i> point to a
	 * sequence word, it returns 0 and does not change the iterator.
	 * 
	 * @param itr1
	 *            first word iterator
	 * @param itr2
	 *            second word iterator
	 * @return the least sequence count between the sequence word pointed by the
	 *         given iterators
	 * @see #skipSequence(WordIterator)
	 */
	private static int skipSequence(WordIterator itr1, WordIterator itr2) {
		int count = 0;
		if (isSequenceWithNoBits(itr1.currentWordCopy) 
				&& isSequenceWithNoBits(itr2.currentWordCopy)) {
			count = Math.min(
					getSequenceCount(itr1.currentWordCopy),
					getSequenceCount(itr2.currentWordCopy));
			if (count > 0) {
				// increase sequence counter
				itr1.currentWordCopy -= count;
				itr2.currentWordCopy -= count;
			}
		} 
		return count;
	}
	
	/**
	 * When the given word iterator currently points to a sequence word,
	 * it decreases this sequence to 0 and return such a count.
	 * <p>
	 * Conversely, when the word iterators does <i>not</i> point to a
	 * sequence word, it returns 0 and does not change the iterator.
	 * 
	 * @param itr
	 *            word iterator
	 * @return the sequence count of the sequence word pointed by the given
	 *         iterator
	 * @see #skipSequence(WordIterator, WordIterator)
	 */
	private static int skipSequence(WordIterator itr) {
		int count = 0;
		if (isSequenceWithNoBits(itr.currentWordCopy)) {
			count = getSequenceCount(itr.currentWordCopy);
			if (count > 0) 
				itr.currentWordCopy -= count;
		} 
		return count;
	}
	
	/**
	 * The same as {@link #skipSequence(WordIterator, WordIterator)}, but for
	 * {@link ReverseWordIterator} instances
	 */
	private /*static*/ int skipSequence(ReverseWordIterator itr1, ReverseWordIterator itr2) {
		int count = 0;
		if (!isLiteral(itr1.currentWordCopy) && !isLiteral(itr2.currentWordCopy)) {
			if (simulateWAH)
				count = Math.min(
						getSequenceCount(itr1.currentWordCopy),
						getSequenceCount(itr2.currentWordCopy));
			else
				count = Math.min(
						getSequenceCount(itr1.currentWordCopy) - (isSequenceWithNoBits(itr1.currentWordCopy) ? 0 : 1),
						getSequenceCount(itr2.currentWordCopy) - (isSequenceWithNoBits(itr2.currentWordCopy) ? 0 : 1));
			if (count > 0) {
				// increase sequence counter
				itr1.currentWordCopy -= count;
				itr2.currentWordCopy -= count;
			}
		} 
		return count;
	}

	/**
	 * Appends to {@code #words} the content of another word array
	 * 
	 * @param itr
	 *            iterator that represents the words to copy. The copy will
	 *            start from the current index of the iterator.
	 */
	private void appendRemainingWords(WordIterator itr) {
		while (!itr.endOfWords()) {
			// copy the word
			lastWordIndex++;
			last += MAX_LITERAL_LENGHT;
			words[lastWordIndex] = itr.currentLiteral;
			compress(); 
			
			// avoid to loop if the current word is a sequence 
			if (!isLiteral(words[lastWordIndex])) {
				int s = skipSequence(itr);
				if (s > 0) {
					words[lastWordIndex] += s;
					last += maxLiteralLengthMultiplication(s);
				}
			}

			// next literal
			itr.prepareNextLiteral();
		}
	}
	
	/**
	 * Possible operations
	 */
	private enum Operator {
		/** 
		 * Bitwise <code>and</code> between literals 
		 */
		AND {
			@Override 
			public int combineLiterals(int literal1, int literal2) {
				return literal1 & literal2;
			}

			@Override 
			public ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2) {
				return new ConciseSet(op1.simulateWAH);
			}

			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.last) {
					// op2 is completely hidden by op1
					if (isZeroSequence(op1.words[0]))
						return new ConciseSet(op1.simulateWAH);
					// op2 is left unchanged, but the rest of op1 is hidden
					return op2.clone();
				}
				return null;
			}
			
			@Override
			public ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2) {
				ConciseSet res = oneWayCombineDisjointSets(op1, op2);
				if (res == null)
					res = oneWayCombineDisjointSets(op2, op1);
				return res;
			}
		},
		
		/** 
		 * Bitwise <code>or</code> between literals
		 */
		OR {
			@Override 
			public int combineLiterals(int literal1, int literal2) {
				return literal1 | literal2;
			}

			@Override 
			public ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2) {
				if (!op1.isEmpty())
					return op1.clone();
				if (!op2.isEmpty())
					return op2.clone();
				return new ConciseSet(op1.simulateWAH);
			}
			
			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.last) {
					// op2 is completely hidden by op1
					if (isOneSequence(op1.words[0]))
						return op1.clone();
					// op2 is left unchanged, but the rest of op1 must be appended...
					
					// ... first, allocate sufficient space for the result
					ConciseSet res = new ConciseSet(op1.simulateWAH);
					res.words = new int[op1.lastWordIndex + op2.lastWordIndex + 3];
					res.lastWordIndex = op2.lastWordIndex;
					
					// ... then, copy op2
					System.arraycopy(op2.words, 0, res.words, 0, op2.lastWordIndex + 1);
					
					// ... in turn, decrease the sequence of op1 by the blocks covered by op2
					WordIterator op1Itr = op1.new WordIterator();
					op1Itr.currentWordCopy -= maxLiteralLengthDivision(op2.last) + 1;
					if (op1Itr.currentWordCopy < 0)
						op1Itr.prepareNextLiteral();
					
					// ... finally, append op1
					res.appendRemainingWords(op1Itr);
					if (op1.size < 0 || op2.size < 0)
						res.size = -1;
					else
						res.size = op1.size + op2.size;
					res.last = op1.last;
					res.compact();
					return res;				
				}
				return null;
			}

			@Override
			public ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2) {
				ConciseSet res = oneWayCombineDisjointSets(op1, op2);
				if (res == null)
					res = oneWayCombineDisjointSets(op2, op1);
				return res;
			}
		},

		/** 
		 * Bitwise <code>xor</code> between literals 
		 */
		XOR {
			@Override 
			public int combineLiterals(int literal1, int literal2) {
				return ALL_ZEROS_LITERAL | (literal1 ^ literal2);
			}

			@Override 
			public ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2) {
				if (!op1.isEmpty())
					return op1.clone();
				if (!op2.isEmpty())
					return op2.clone();
				return new ConciseSet(op1.simulateWAH);
			}
			
			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.last) {
					// op2 is left unchanged by op1
					if (isZeroSequence(op1.words[0]))
						return OR.combineDisjointSets(op1, op2);
					// op2 must be complemented, then op1 must be appended 
					// it is better to perform it normally...
					return null;
				}
				return null;
			}

			@Override
			public ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2) {
				ConciseSet res = oneWayCombineDisjointSets(op1, op2);
				if (res == null)
					res = oneWayCombineDisjointSets(op2, op1);
				return res;
			}
		},

		/** 
		 * Bitwise <code>and-not</code> between literals (i.e. <code>X and (not Y)</code>) 
		 */
		ANDNOT {
			@Override 
			public int combineLiterals(int literal1, int literal2) {
				return ALL_ZEROS_LITERAL | (literal1 & (~literal2));
			}

			@Override 
			public ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2) {
				if (!op1.isEmpty())
					return op1.clone();
				return new ConciseSet(op1.simulateWAH);
			}
			
			@Override
			public ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.last) {
					// op1 is left unchanged by op2
					if (isZeroSequence(op1.words[0]))
						return op1.clone();
					// op2 must be complemented, then op1 must be appended 
					// it is better to perform it normally...
					return null;
				}
				// check whether the second operator starts with a sequence that
				// completely "covers" the first operator
				if (isSequenceWithNoBits(op2.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op2.words[0]) + 1) > op1.last) {
					// op1 is left unchanged by op2
					if (isZeroSequence(op2.words[0]))
						return op1.clone();
					// op1 is cleared by op2
					return new ConciseSet(op1.simulateWAH);
				}
				return null;
			}
		},
		;

		/**
		 * Performs the operation on the given literals
		 * 
		 * @param literal1
		 *            left operand
		 * @param literal2
		 *            right operand
		 * @return literal representing the result of the specified operation
		 */
		public abstract int combineLiterals(int literal1, int literal2); 
		
		/**
		 * Performs the operation when one or both operands are empty set
		 * <p>
		 * <b>NOTE: the caller <i>MUST</i> assure that one or both the operands
		 * are empty!!!</b>
		 * 
		 * @param op1
		 *            left operand
		 * @param op2
		 *            right operand
		 * @return <code>null</code> if both operands are non-empty
		 */
		public abstract ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2);
		
		/**
		 * Performs the operation in the special case of "disjoint" sets, namely
		 * when the first (or the second) operand starts with a sequence (it
		 * does not matter if 0's or 1's) that completely covers all the bits of
		 * the second (or the first) operand.
		 * 
		 * @param op1
		 *            left operand
		 * @param op2
		 *            right operand
		 * @return <code>null</code> if operands are non-disjoint
		 */
		public abstract ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2);
	}
	
	/**
	 * Performs the given operation over the bit-sets
	 * 
	 * @param other
	 *            {@link ConciseSet} instance that represents the right
	 *            operand
	 * @param operator
	 *            operator
	 * @return the result of the operation
	 */
	private ConciseSet performOperation(Collection<? extends Integer> other, Operator operator) {
		ConciseSet otherSet = convert(other);
		
		// non-empty arguments
		if (this.isEmpty() || otherSet.isEmpty()) 
			return operator.combineEmptySets(this, otherSet);
		
		// if the two operands are disjoint, the operation is faster
		ConciseSet res = operator.combineDisjointSets(this, otherSet);
		if (res != null)
			return res;
		
		// Allocate a sufficient number of words to contain all possible results.
		// NOTE: "+3" means:
		// - since lastWordIndex is the index of the last used word in "words",
		//   we require "+2" to have the actual maximum required space
		// - "+1" is required to allows for the addition of the last word before compacting
		// In any case, we do not allocate more than the maximum space required
		// for the uncompressed representation
		res = new ConciseSet(simulateWAH);
		res.words = new int[Math.min(
				this.lastWordIndex + otherSet.lastWordIndex + 3, 
				maxLiteralLengthDivision(Math.max(this.last, otherSet.last)) + 1)];

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		res.last = 0;
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			res.lastWordIndex++;
			res.words[res.lastWordIndex] = operator.combineLiterals(thisItr.currentLiteral, otherItr.currentLiteral);
			res.compress(); 
			
			// avoid loops when both are sequences and the result is a sequence
			res.last += MAX_LITERAL_LENGHT;
			if (!isLiteral(res.words[res.lastWordIndex])) {
				int s = skipSequence(thisItr, otherItr);
				if (s > 0) {
					res.words[res.lastWordIndex] += s;
					res.last += maxLiteralLengthMultiplication(s);
				}
			}
			
			// next literal
			thisItr.prepareNextLiteral();
			otherItr.prepareNextLiteral();
		}

		// if one bit string is greater than the other one, we add the remaining
		// bits depending on the given operation. 
		// NOTE: the iterators CANNOT be both non-empty
		switch (operator) {
		case AND:
			break;
		case OR:
		case XOR:
			// NOTE: one iterator does not have more elements!
			res.appendRemainingWords(otherItr);
			// no break;
		case ANDNOT:
			res.appendRemainingWords(thisItr);
			break;
		}

		// remove trailing zeros
		res.trimZeros();
		if (res.isEmpty())
			return res;

		// compact the memory
		res.compact();
		
		// update size and maximal element
		res.size = -1;
		int w = res.words[res.lastWordIndex];
		if (isLiteral(w)) 
			res.last -= Integer.numberOfLeadingZeros(getLiteralBits(w));
		else 
			res.last--;
		return res;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends Integer> other) {
		Statistics.sizeCheckCount++;
		
		// empty arguments
		if (other == null || other.isEmpty() || this.isEmpty())
			return 0;

		// single-element intersection
		if (size == 1)
			return other.contains(last()) ? 1 : 0;
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = convert(other);
		if (otherSet.size == 1) 
			return contains(otherSet.last()) ? 1 : 0;
		
		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.last) {
			if (isZeroSequence(this.words[0]))
				return 0;
			return otherSet.size();
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.last) {
			if (isZeroSequence(otherSet.words[0]))
				return 0;
			return this.size();
		}
		
		// resulting size
		int res = 0;

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			int curRes = getLiteralBitCount(thisItr.currentLiteral & otherItr.currentLiteral);
			res += curRes;

			// avoid loops when both are sequences and the result is a sequence
			if (curRes == ALL_ZEROS_WITHOUT_MSB || curRes == ALL_ONES_WITHOUT_MSB) 
				res += curRes * skipSequence(thisItr, otherItr);

			// next literals
			thisItr.prepareNextLiteral();
			otherItr.prepareNextLiteral();
		}

		// return the intersection size
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		if (isEmpty())
			return 0;
		return last - size() + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer get(int i) {
		if (i < 0)
			throw new IndexOutOfBoundsException();

		// initialize data
		int firstSetBitInWord = 0;
		int position = i;
		int setBitsInCurrentWord = 0;
		for (int j = 0; j <= lastWordIndex; j++) {
			int w = words[j];
			if (isLiteral(w)) {
				// number of bits in the current word
				setBitsInCurrentWord = getLiteralBitCount(w);
				
				// check if the desired bit is in the current word
				if (position < setBitsInCurrentWord) {
					int currSetBitInWord = -1;
					for (; position >= 0; position--)
						currSetBitInWord = Integer.numberOfTrailingZeros(w & (0xFFFFFFFF << (currSetBitInWord + 1)));
					return new Integer(firstSetBitInWord + currSetBitInWord);
				}
				
				// skip the 31-bit block
				firstSetBitInWord += MAX_LITERAL_LENGHT;
			} else {
				// number of involved bits (31 * blocks)
				int sequenceLength = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
				
				// check the sequence type
				if (isOneSequence(w)) {
					if (simulateWAH || isSequenceWithNoBits(w)) {
						setBitsInCurrentWord = sequenceLength;
						if (position < setBitsInCurrentWord)
							return new Integer(firstSetBitInWord + position);
					} else {
						setBitsInCurrentWord = sequenceLength - 1;
						if (position < setBitsInCurrentWord)
							// check whether the desired set bit is after the
							// flipped bit (or after the first block)
							return new Integer(firstSetBitInWord + position + (position < getFlippedBit(w) ? 0 : 1));
					}
				} else {
					if (simulateWAH ||isSequenceWithNoBits(w)) {
						setBitsInCurrentWord = 0;
					} else {
						setBitsInCurrentWord = 1;
						if (position == 0)
							return new Integer(firstSetBitInWord + getFlippedBit(w));
					}
				}

				// skip the 31-bit blocks
				firstSetBitInWord += sequenceLength;
			}
			
			// update the number of found set bits
			position -= setBitsInCurrentWord;
		}
		
		throw new IndexOutOfBoundsException(Integer.toString(i));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(Integer e) {
		// empty element
		if (isEmpty())
			return -1;

		// returned value
		int index = 0;

		int blockIndex = maxLiteralLengthDivision(e);
		int bitPosition = maxLiteralLengthModulus(e);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					if ((w & (1 << bitPosition)) == 0)
						return -1;
					return index + Integer.bitCount(w & ~(0xFFFFFFFF << bitPosition));
				}
				blockIndex--;
				index += getLiteralBitCount(w);
			} else {
				if (simulateWAH) {
					if (isOneSequence(w) && blockIndex <= getSequenceCount(w))
						return index + maxLiteralLengthMultiplication(blockIndex) + bitPosition;
				} else {
					// if we are at the beginning of a sequence, and it is
					// a set bit, the bit already exists
					if (blockIndex == 0) {
						int l = getLiteral(w);
						if ((l & (1 << bitPosition)) == 0)
							return -1;
						return index + Integer.bitCount(l & ~(0xFFFFFFFF << bitPosition));
					}
					
					// if we are in the middle of a sequence of 1's, the bit already exist
					if (blockIndex > 0 
							&& blockIndex <= getSequenceCount(w) 
							&& isOneSequence(w))
						return index + maxLiteralLengthMultiplication(blockIndex) + bitPosition - (isSequenceWithNoBits(w) ? 0 : 1);
				}
				
				// next word
				int blocks = getSequenceCount(w) + 1;
				blockIndex -= blocks;
				if (isZeroSequence(w)) {
					if (!simulateWAH && !isSequenceWithNoBits(w))
						index++;
				} else {
					index += maxLiteralLengthMultiplication(blocks);
					if (!simulateWAH && !isSequenceWithNoBits(w))
						index--;
				}
			}
		}
		
		// not found
		return -1;
	}

	/**
	 * Checks if the <i>literal</i> contained within
	 * {@code #getLastWord()} can be merged with the previous word
	 * sequences (i.e. {@code #words[lastWordIndex - 1]}), hence forming (or
	 * updating) a sequence of 0's or 1's
	 * 
	 * @return <code>true</code> if the word have been merged
	 * @see #compactLastWord()
	 */
	private boolean compress() {
		// nothing to merge
		if (lastWordIndex == 0)
			return false;
		
		// current word type
		final boolean isCurrentWordAllZeros = words[lastWordIndex] == ALL_ZEROS_LITERAL;
		final boolean isCurrentWordAllOnes = words[lastWordIndex] == ALL_ONES_LITERAL;

		// nothing to merge
		if (!isCurrentWordAllZeros && !isCurrentWordAllOnes)
			return false;
		
		// previous word to merge
		int previousWord = words[lastWordIndex - 1];
		
		// the previous word is a sequence of the same kind --> add one block
		final boolean isPreviousWordZeroSeq = isZeroSequence(previousWord);
		final boolean isPreviousWordOneSeq = isOneSequence(previousWord);
		if ((isCurrentWordAllOnes && isPreviousWordOneSeq) 
				|| (isCurrentWordAllZeros && isPreviousWordZeroSeq)) {
			lastWordIndex--;
			words[lastWordIndex]++;
			return true;
		}

		// the previous word is a sequence of a different kind --> cannot merge
		if ((isCurrentWordAllOnes && isPreviousWordZeroSeq)
				|| (isCurrentWordAllZeros && isPreviousWordOneSeq)) 
			return false;
		
		// try to convert the previous literal word in a sequence and to merge 
		// the current word with it
		if (isCurrentWordAllOnes) {
			// convert set bits to unset bits 
			previousWord = ALL_ZEROS_LITERAL | ~previousWord;
		}
		int b = getLiteralBits(previousWord);
		if (b == 0) {
			lastWordIndex--;
			words[lastWordIndex] = makeSequenceWord(0, isCurrentWordAllOnes, 1);
			return true;
		}
		if (!simulateWAH && containsOnlyOneBit(b)) {
			int setBit = 1 + Integer.numberOfTrailingZeros(previousWord);
			lastWordIndex--;
			words[lastWordIndex] = makeSequenceWord(setBit, isCurrentWordAllOnes, 1);
			return true;
		}

		// both the current and the previous words still remains literals
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet intersection(Collection<? extends Integer> other) {
		Statistics.intersectionCount++;
		return performOperation(other, Operator.AND);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet union(Collection<? extends Integer> other) {
		Statistics.unionCount++;
		return performOperation(other, Operator.OR);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet difference(Collection<? extends Integer> other) {
		Statistics.differenceCount++;
		return performOperation(other, Operator.ANDNOT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet symmetricDifference(Collection<? extends Integer> other) {
		Statistics.symmetricDifferenceCount++;
		return performOperation(other, Operator.XOR);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet complemented() {
		ConciseSet cloned = clone();
		cloned.complement();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		modCount++;
		
		if (isEmpty())
			return;
		
		if (last == MIN_ALLOWED_SET_BIT) {
			clear();
			return;
		}
		
		// update size
		if (size >= 0)
			size = last - size + 1;

		// complement each word
		for (int i = 0; i <= lastWordIndex; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				// negate the bits and set the most significant bit to 1
				words[i] = ALL_ZEROS_LITERAL | ~w;
			} else {
				// switch the sequence type
				words[i] ^= 0x40000000;
			}
		}

		// do not complement after the last element
		if (isLiteral(words[lastWordIndex]))
			clearBitsAfterInLastWord(maxLiteralLengthModulus(last));

		// remove trailing zeros
		trimZeros();
		if (isEmpty())
			return;

		// calculate the maximal element
		last = 0;
		int w = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			w = words[i];
			if (isLiteral(w)) 
				last += MAX_LITERAL_LENGHT;
			else 
				last += maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
		}

		// manage the last word (that must be a literal or a sequence of 1's)
		if (isLiteral(w)) 
			last -= Integer.numberOfLeadingZeros(getLiteralBits(w));
		else 
			last--;
	}

	/**
	 * Removes trailing zeros
	 */
	private void trimZeros() {
		// loop over ALL_ZEROS_LITERAL words
		int w;
		do {
			w = words[lastWordIndex];
			if (w == ALL_ZEROS_LITERAL) {
				lastWordIndex--;
				last -= MAX_LITERAL_LENGHT;
			} else if (isZeroSequence(w)) {
				if (isSequenceWithNoBits(w)) {
					last -= maxLiteralLengthMultiplication(getSequenceCount(w + 1));
					lastWordIndex--;
				} else {
					// convert the sequence in a 1-bit literal word
					last -= maxLiteralLengthMultiplication(getSequenceCount(w));
					words[lastWordIndex] = getLiteral(w);
				}
			} else {
				// one sequence or literal
				return;
			}
			if (lastWordIndex < 0) {
				reset();
				return;
			}
		} while (true);
	}
	
	/**
	 * Iterator for set bits of {@link ConciseSet}, from LSB to MSB
	 */
	private class BitIterator implements ExtendedIterator<Integer> {
		private WordIterator wordItr = new WordIterator();
		private int rightmostBitOfCurrentWord = 0;
		private int nextBitToCheck = 0;
		private int initialModCount = modCount;

		/**
		 * Gets the next bit in the current literal
		 * 
		 * @return 32 if there is no next bit, otherwise the next set bit within
		 *         the current literal
		 */
		private int getNextSetBit() {
			return Integer.numberOfTrailingZeros(wordItr.currentLiteral & (0xFFFFFFFF << nextBitToCheck));
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return wordItr.hasMoreLiterals() || getNextSetBit() < MAX_LITERAL_LENGHT;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer next() {
			// check for concurrent modification 
			if (initialModCount != modCount)
				throw new ConcurrentModificationException();
			
			// loop until we find some set bit
			int nextSetBit = MAX_LITERAL_LENGHT;
			while (nextSetBit >= MAX_LITERAL_LENGHT) {
				// next bit in the current literal
				nextSetBit = getNextSetBit();

				if (nextSetBit < MAX_LITERAL_LENGHT) {
					// return the current bit and then set the next search
					// within the literal
					nextBitToCheck = nextSetBit + 1;
				} else {
					// advance one word
					rightmostBitOfCurrentWord += MAX_LITERAL_LENGHT;
					if (isZeroSequence(wordItr.currentWordCopy)) {
						// skip zeros
						int blocks = getSequenceCount(wordItr.currentWordCopy);
						rightmostBitOfCurrentWord += MAX_LITERAL_LENGHT * blocks;
						wordItr.currentWordCopy -= blocks;
					}
					nextBitToCheck = 0;
					wordItr.prepareNextLiteral();
				}
			}

			return new Integer(rightmostBitOfCurrentWord + nextSetBit);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(Integer element) {
			if (element.intValue() > MAX_ALLOWED_INTEGER)
				throw new IndexOutOfBoundsException(element.toString());

			// the element is before the next one
			if (element.intValue() <= rightmostBitOfCurrentWord + nextBitToCheck)
				return;
			
			// the element is after the last one
			if (element.intValue() > last){
				// makes hasNext() return "false"
				wordItr.remainingWords = 0;
				wordItr.currentWordCopy = ALL_ZEROS_LITERAL;
				return;
			}
			
			// next element
			nextBitToCheck = element.intValue() - rightmostBitOfCurrentWord;
			
			// the element is in the current word
			if (nextBitToCheck < MAX_LITERAL_LENGHT)
				return;
			
			// the element should be after the current word, but there are no more words
			if (!wordItr.hasMoreLiterals()) {
				// makes hasNext() return "false"
				wordItr.remainingWords = 0;
				wordItr.currentLiteral = 0;
				return;
			}
			
			// the element is after the current word
			while (nextBitToCheck >= MAX_LITERAL_LENGHT) {
				if (isLiteral(wordItr.currentWordCopy) || !isSequenceWithNoBits(wordItr.currentWordCopy)) {
					// skip the current literal word or the first block of a
					// sequence with (un)set bit
					rightmostBitOfCurrentWord += MAX_LITERAL_LENGHT;
					nextBitToCheck -= MAX_LITERAL_LENGHT;
				} else {
					int blocks = getSequenceCount(wordItr.currentWordCopy);
					int bits = maxLiteralLengthMultiplication(1 + blocks);
					if (isZeroSequence(wordItr.currentWordCopy)) {
						if (bits > nextBitToCheck)
							nextBitToCheck = 0;
						else
							nextBitToCheck -= bits;
					} else {
						if (bits > nextBitToCheck) {
							blocks = maxLiteralLengthDivision(nextBitToCheck) - 1;
							bits = maxLiteralLengthMultiplication(blocks + 1);
						} 
						nextBitToCheck -= bits;
					}
					rightmostBitOfCurrentWord += bits;
					wordItr.currentWordCopy -= blocks;
				}
				wordItr.prepareNextLiteral();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			// it is difficult to remove the current bit in a sequence word and
			// to contextually keep the word iterator updated!
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator for set bits of {@link ConciseSet}, from MSB to LSB
	 */
	private class ReverseBitIterator implements ExtendedIterator<Integer> {
		private ReverseWordIterator wordItr = new ReverseWordIterator();
		private int rightmostBitOfCurrentWord = maxLiteralLengthMultiplication(maxLiteralLengthDivision(last));
		private int nextBitToCheck = maxLiteralLengthModulus(last);
		private int initialModCount = modCount;

		/**
		 * Gets the next bit in the current literal
		 * 
		 * @return -1 if there is no next bit, otherwise the next set bit within
		 *         the current literal
		 */
		private int getNextSetBit() {
			return MAX_LITERAL_LENGHT 
				- Integer.numberOfLeadingZeros(wordItr.currentLiteral & ~(0xFFFFFFFF << (nextBitToCheck + 1)));
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return wordItr.hasMoreLiterals() || getNextSetBit() >= 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer next() {
			// check for concurrent modification 
			if (initialModCount != modCount)
				throw new ConcurrentModificationException();
			
			// loop until we find some set bit
			int nextSetBit = -1;
			while (nextSetBit < 0) {
				// next bit in the current literal
				nextSetBit = getNextSetBit();

				if (nextSetBit >= 0) {
					// return the current bit and then set the next search
					// within the literal
					nextBitToCheck = nextSetBit - 1;
				} else {
					// advance one word
					rightmostBitOfCurrentWord -= MAX_LITERAL_LENGHT;
					if (isZeroSequence(wordItr.currentWordCopy)) {
						// skip zeros
						int blocks = getSequenceCount(wordItr.currentWordCopy);
						if (!isSequenceWithNoBits(wordItr.currentWordCopy))
							blocks--;
						if (blocks > 0) {
							rightmostBitOfCurrentWord -= maxLiteralLengthMultiplication(blocks);
							wordItr.currentWordCopy -= blocks;
						}
					}
					nextBitToCheck = MAX_LITERAL_LENGHT - 1;
					wordItr.prepareNextLiteral();
				}
			}

			return new Integer(rightmostBitOfCurrentWord + nextSetBit);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(Integer element) {
			if (element.intValue() < 0)
				throw new IndexOutOfBoundsException(element.toString());
			
			// the element is before the next one
			if (element.intValue() >= rightmostBitOfCurrentWord + nextBitToCheck)
				return;
			
			// next element
			nextBitToCheck = element.intValue() - rightmostBitOfCurrentWord;
			
			// the element is in the current word
			if (nextBitToCheck > 0)
				return;
			
			// the element should be after the current word, but there are no more words
			if (!wordItr.hasMoreLiterals()) {
				// makes hasNext() return "false"
				wordItr.currentWordIndex = -1;
				wordItr.currentWordCopy = ALL_ZEROS_LITERAL;
				return;
			}
			
			// the element is after the current word
			while (nextBitToCheck < 0) {
				if (isLiteral(wordItr.currentWordCopy) || getSequenceCount(wordItr.currentWordCopy) == 0) {
					// skip the current literal word or the first block of a
					// sequence with (un)set bit
					rightmostBitOfCurrentWord -= MAX_LITERAL_LENGHT;
					nextBitToCheck += MAX_LITERAL_LENGHT;
				} else {
					int blocks = getSequenceCount(wordItr.currentWordCopy);
					if (!isSequenceWithNoBits(wordItr.currentWordCopy))
						blocks--;
					int bits = maxLiteralLengthMultiplication(1 + blocks);
					if (bits > -nextBitToCheck) {
						blocks = maxLiteralLengthDivision(-nextBitToCheck - 1);
						bits = maxLiteralLengthMultiplication(blocks + 1);
					}
					rightmostBitOfCurrentWord -= bits;
					nextBitToCheck += bits;
					wordItr.currentWordCopy -= blocks;
				}
				wordItr.prepareNextLiteral();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			// it is difficult to remove the current bit in a sequence word and
			// to contextually keep the word iterator updated!
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> iterator() {
		if (isEmpty()) {
			return new ExtendedIterator<Integer>() {
				@Override public void skipAllBefore(Integer element) {/*empty*/}
				@Override public boolean hasNext() {return false;}
				@Override public Integer next() {throw new NoSuchElementException();}
				@Override public void remove() {throw new UnsupportedOperationException();}
			};
		}
		return new BitIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> descendingIterator() {
		if (isEmpty()) {
			return new ExtendedIterator<Integer>() {
				@Override public void skipAllBefore(Integer element) {/*empty*/}
				@Override public boolean hasNext() {return false;}
				@Override public Integer next() {throw new NoSuchElementException();}
				@Override public void remove() {throw new UnsupportedOperationException();}
			};
		}
		return new ReverseBitIterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super Integer> comparator() {
		// natural order of Integer
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return new Integer(last);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConciseSet convert(Collection<?> c) {
		if (c == null)
			return new ConciseSet(simulateWAH);
		if (c instanceof ConciseSet)
			return (ConciseSet) c;
		return new ConciseSet(simulateWAH, (Collection<? extends Integer>) c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet convert(Object... e) {
		return new ConciseSet(simulateWAH, e);
	}

	/**
	 * Converts a given integer into an instance of the current class
	 * 
	 * @param e integer to convert
	 * @return the converted collection
	 */
	public ConciseSet convert(int e) {
		return new ConciseSet(simulateWAH, e);
	}
	
	/**
	 * Replace the current instance with another {@link ConciseSet} instance
	 * 
	 * @param other {@link ConciseSet} instance to use to replace the current one
	 * @return <code>true</code> if the given set is different from the current set
	 */
	private boolean replaceWith(ConciseSet other) {
		if (this == other)
			return false;
		
		boolean isSimilar = (this.lastWordIndex == other.lastWordIndex)
			&& (this.last == other.last);
		for (int i = 0; isSimilar && (i <= lastWordIndex); i++)
			isSimilar &= this.words[i] == other.words[i]; 

		if (isSimilar) {
			if (other.size >= 0)
				this.size = other.size;
			return false;
		}
		
		this.words = other.words;
		this.size = other.size;
		this.last = other.last;
		this.lastWordIndex = other.lastWordIndex;
		this.modCount++;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(Integer e) {
		modCount++;

		final int b = e.intValue();

		// range check
		if (b < MIN_ALLOWED_SET_BIT || b > MAX_ALLOWED_INTEGER)
			throw new IndexOutOfBoundsException(Integer.toString(b));

		// the element can be simply appended
		if (b > last) {
			append(b);
			return true;
		}

		if (b == last)
			return false;

		// check if the element can be put in a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// bit already set
					if ((w & (1 << bitPosition)) != 0)
						return false;
					
					// By adding the bit we potentially create a sequence:
					// -- If the literal is made up of all zeros, it definitely
					//    cannot be part of a sequence (otherwise it would not have
					//    been created). Thus, we can create a 1-bit literal word
					// -- If there are MAX_LITERAL_LENGHT - 2 set bits, by adding 
					//    the new one we potentially allow for a 1's sequence 
					//    together with the successive word
					// -- If there are MAX_LITERAL_LENGHT - 1 set bits, by adding 
					//    the new one we potentially allow for a 1's sequence 
					//    together with the successive and/or the preceding words
					if (!simulateWAH) {
						int bitCount = getLiteralBitCount(w);
						if (bitCount >= MAX_LITERAL_LENGHT - 2)
							break;
					} else {
						if (containsOnlyOneBit(~w))
							break;
					}
						
					// set the bit
					words[i] |= 1 << bitPosition;
					if (size >= 0)
						size++;
					return true;
				} 
				
				blockIndex--;
			} else {
				if (simulateWAH) {
					if (isOneSequence(w) && blockIndex <= getSequenceCount(w))
						return false;
				} else {
					// if we are at the beginning of a sequence, and it is
					// a set bit, the bit already exists
					if (blockIndex == 0 
							&& (getLiteral(w) & (1 << bitPosition)) != 0)
						return false;
					
					// if we are in the middle of a sequence of 1's, the bit already exist
					if (blockIndex > 0 
							&& blockIndex <= getSequenceCount(w) 
							&& isOneSequence(w))
						return false;
				}

				// next word
				blockIndex -= getSequenceCount(w) + 1;
			}
		}
		
		// the bit is in the middle of a sequence or it may cause a literal to
		// become a sequence, thus the "easiest" way to add it is by ORing
		Statistics.unionCount++;
		return replaceWith(performOperation(convert(b), Operator.OR));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		modCount++;

		if (o == null || isEmpty() || !(o instanceof Integer))
			return false;

		final int b = ((Integer) o).intValue();

		// the element cannot exist
		if (b > last) 
			return false;

		// check if the element can be removed from a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// the bit is already unset
					if ((w & (1 << bitPosition)) == 0)
						return false;
					
					// By removing the bit we potentially create a sequence:
					// -- If the literal is made up of all ones, it definitely
					//    cannot be part of a sequence (otherwise it would not have
					//    been created). Thus, we can create a 30-bit literal word
					// -- If there are 2 set bits, by removing the specified
					//    one we potentially allow for a 1's sequence together with 
					//    the successive word
					// -- If there is 1 set bit, by removing the new one we 
					//    potentially allow for a 0's sequence 
					//    together with the successive and/or the preceding words
					if (!simulateWAH) {
						int bitCount = getLiteralBitCount(w);
						if (bitCount <= 2)
							break;
					} else {
						if (containsOnlyOneBit(getLiteralBits(w)))
							break;
					}
						
					// unset the bit
					words[i] &= ~(1 << bitPosition);
					if (size >= 0)
						size--;
					
					// if the bit is the maximal element, update it
					if (b == last) {
						last -= maxLiteralLengthModulus(last) - (MAX_LITERAL_LENGHT 
								- Integer.numberOfLeadingZeros(getLiteralBits(words[i])));
					}
					return true;
				} 

				blockIndex--;
			} else {
				if (simulateWAH) {
					if (isZeroSequence(w) && blockIndex <= getSequenceCount(w))
						return false;
				} else {
					// if we are at the beginning of a sequence, and it is
					// an unset bit, the bit does not exist
					if (blockIndex == 0 
							&& (getLiteral(w) & (1 << bitPosition)) == 0)
						return false;
					
					// if we are in the middle of a sequence of 0's, the bit does not exist
					if (blockIndex > 0 
							&& blockIndex <= getSequenceCount(w) 
							&& isZeroSequence(w))
						return false;
	
					// next word
					blockIndex -= getSequenceCount(w) + 1;
				}
			}
		}
		
		// the bit is in the middle of a sequence or it may cause a literal to
		// become a sequence, thus the "easiest" way to remove it by ANDNOTing
		Statistics.differenceCount++;
		return replaceWith(performOperation(convert(b), Operator.ANDNOT));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null || isEmpty() || !(o instanceof Integer))
			return false;

		final int b = ((Integer)o ).intValue();
		
		// the element is greater than the maximal value
		if (b > last) 
			return false;

		// check if the element is within a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// bit already set
					return (w & (1 << bitPosition)) != 0;
				} 
				blockIndex--;
			} else {
				if (simulateWAH) {
					if (isOneSequence(w) && blockIndex <= getSequenceCount(w))
						return true;
				} else {
					// if we are at the beginning of a sequence, and it is
					// a set bit, the bit already exists
					if (blockIndex == 0 
							&& (getLiteral(w) & (1 << bitPosition)) != 0)
						return true;
					
					// if we are in the middle of a sequence of 1's, the bit already exist
					if (blockIndex > 0 
							&& blockIndex <= getSequenceCount(w) 
							&& isOneSequence(w))
						return true;
				}
				
				// next word
				blockIndex -= getSequenceCount(w) + 1;
			}
		}
		
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		Statistics.sizeCheckCount++;

		if (c == null || c.isEmpty() || c == this)
			return true;
		if (isEmpty())
			return false;
		
		final ConciseSet otherSet = convert(c);
		if (otherSet.last > last)
			return false;
		if (size >= 0 && otherSet.size > size)
			return false;
		if (otherSet.size == 1) 
			return contains(otherSet.last());

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// check shared elements between the two sets
			int curRes = thisItr.currentLiteral & otherItr.currentLiteral;
			
			// check if this set does not completely contains the other set
			if (otherItr.currentLiteral != curRes)
				return false;

			// Avoid loops when both are sequence and the result is a sequence
			if (curRes == ALL_ZEROS_WITHOUT_MSB || curRes == ALL_ONES_WITHOUT_MSB) 
				skipSequence(thisItr, otherItr);

			// next literals
			thisItr.prepareNextLiteral();
			otherItr.prepareNextLiteral();
		}

		// the intersection is equal to the other set
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Integer> other) {
		Statistics.sizeCheckCount++;

		if (other == null || other.isEmpty() || this.isEmpty())
			return false;
		if (other == this)
			return true;
		
		final ConciseSet otherSet = convert(other);
		if (otherSet.size == 1)
			return contains(otherSet.last);

		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.last) {
			if (isZeroSequence(this.words[0]))
				return false;
			return true;
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.last) {
			if (isZeroSequence(otherSet.words[0]))
				return false;
			return true;
		}

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// check shared elements between the two sets
			int curRes = thisItr.currentLiteral & otherItr.currentLiteral;
			
			// check if this set contains some bit of the other set
			if (curRes != ALL_ZEROS_LITERAL)
				return true;

			// Avoid loops when both are sequence and the result is a sequence
			skipSequence(thisItr, otherItr);

			// next literals
			thisItr.prepareNextLiteral();
			otherItr.prepareNextLiteral();
		}

		// the intersection is equal to the empty set
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends Integer> other, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		
		Statistics.sizeCheckCount++;

		// empty arguments
		if ((size >= 0 && size < minElements) || other == null || other.isEmpty())
			return false;
		if (other == this)
			return true;

		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = convert(other);
		if (otherSet.size >= 0 && otherSet.size < minElements)
			return false;
		if (minElements == 1 && otherSet.size == 1)
			return contains(otherSet.last());
		if (minElements == 1 && size == 1)
			return otherSet.contains(last());
		
		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.last) {
			if (isZeroSequence(this.words[0]))
				return false;
			return true;
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.last) {
			if (isZeroSequence(otherSet.words[0]))
				return false;
			return true;
		}

		// resulting size
		int res = 0;

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			int curRes = getLiteralBitCount(thisItr.currentLiteral & otherItr.currentLiteral);
			res += curRes;
			if (res >= minElements)
				return true;

			// Avoid loops when both are sequence and the result is a sequence
			if (curRes == ALL_ZEROS_WITHOUT_MSB || curRes == ALL_ONES_WITHOUT_MSB) 
				res += curRes * skipSequence(thisItr, otherItr);

			// next literals
			thisItr.prepareNextLiteral();
			otherItr.prepareNextLiteral();
		}

		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return words == null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		modCount++;
		Statistics.intersectionCount++;

		if (isEmpty() || c == this)
			return false;
		if (c == null || c.isEmpty()) {
			clear();
			return true;
		}
		
		ConciseSet other = convert(c);
		if (other.size == 1) {
			if (contains(other.last())) {
				if (size == 1) 
					return false;
				return replaceWith(convert(other.last));
			} 
			clear();
			return true;
		}
		
		return replaceWith(performOperation(other, Operator.AND));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		modCount++;
		Statistics.unionCount++;
		if (c == null || c.isEmpty() || c == this)
			return false;

		ConciseSet other = convert(c);
		if (other.size == 1) 
			return add(other.last());
		
		return replaceWith(performOperation(convert(c), Operator.OR));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		modCount++;
		Statistics.differenceCount++;

		if (c == null || c.isEmpty() || isEmpty())
			return false;
		if (c == this)
			clear();

		ConciseSet other = convert(c);
		if (other.size == 1) 
			return remove(other.last());
		
		return replaceWith(performOperation(convert(c), Operator.ANDNOT));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		if (size < 0) {
			size = 0;
			for (int i = 0; i <= lastWordIndex; i++) {
				int w = words[i];
				if (isLiteral(w)) {
					size += getLiteralBitCount(w);
				} else {
					if (isZeroSequence(w)) {
						if (!isSequenceWithNoBits(w))
							size++;
					} else {
						size += maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
						if (!isSequenceWithNoBits(w))
							size--;
					}
				}
			}
		}
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet empty() {
		return new ConciseSet(simulateWAH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        int h = 1;
        for (int i = 0; i <= lastWordIndex; i++) 
            h = (h << 5) - h + words[i];
        return h;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		Statistics.equalsCount++;

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		ConciseSet other = convert((Collection<?>) obj);
		if (last != other.last)
			return false;
		if (!isEmpty())
	        for (int i = 0; i <= lastWordIndex; i++)
	            if (words[i] != other.words[i])
	                return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ExtendedSet<Integer> o) {
		// check if the given object has the same internal representation
		if (!(o instanceof ConciseSet))
			return super.compareTo(o);

		Statistics.equalsCount++;
		final ConciseSet other = (ConciseSet) o;
		
		// empty set cases
		if (this.isEmpty() && other.isEmpty())
			return 0;
		if (this.isEmpty())
			return -1;
		if (other.isEmpty())
			return 1;
		
		// the word at the end must be the same
		int res = this.last - other.last;
		if (res != 0)
			return res < 0 ? -1 : 1;
		
		// scan words from MSB to LSB
		ReverseWordIterator thisIterator = this.new ReverseWordIterator();
		ReverseWordIterator otherIterator = other.new ReverseWordIterator();
		while (!thisIterator.endOfWords() && !otherIterator.endOfWords()) {
			// compare current literals
			res = getLiteralBits(thisIterator.currentLiteral) - getLiteralBits(otherIterator.currentLiteral);
			if (res != 0)
				return res < 0 ? -1 : 1;
			
			// avoid loops when both are sequences and the result is a sequence
			skipSequence(thisIterator, otherIterator);

			// next literals
			thisIterator.prepareNextLiteral();
			otherIterator.prepareNextLiteral();
		}
		return thisIterator.hasMoreLiterals() ? 1 : (otherIterator.hasMoreLiterals() ? -1 : 0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends ConciseSet> powerSet() {
		return (List<? extends ConciseSet>) super.powerSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends ConciseSet> powerSet(int min, int max) {
		return (List<? extends ConciseSet>) super.powerSet(min, max);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (lastWordIndex + 1) / Math.ceil((1 + last) / 32D);
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
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<Integer> headSet(Integer toElement) {
		if (toElement.intValue() > MAX_ALLOWED_INTEGER)
			throw new IllegalArgumentException(toElement.toString());
		return super.headSet(toElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<Integer> subSet(Integer fromElement, Integer toElement) {
		if (toElement.intValue() > MAX_ALLOWED_INTEGER)
			throw new IllegalArgumentException(toElement.toString());
		if (fromElement.intValue() < 0)
			throw new IllegalArgumentException(fromElement.toString());
		return super.subSet(fromElement, toElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<Integer> tailSet(Integer fromElement) {
		if (fromElement.intValue() < 0)
			throw new IllegalArgumentException(fromElement.toString());
		return super.tailSet(fromElement);
	}
	
	/*
	 * DEBUG METHODS
	 */
	
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
		
		f.format("Elements: %s\n", toString());
		
		// elements
		int firstBitInWord = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			// raw representation of words[i]
			f.format("words[%d] = ", i);
			String ws = toBinaryString(words[i]);
			if (isLiteral(words[i])) {
				s.append(ws.substring(0, 1));
				s.append("--");
				s.append(ws.substring(1));
			} else {
				s.append(ws.substring(0, 2));
				s.append('-');
				if (simulateWAH)
					s.append("xxxxx");
				else
					s.append(ws.substring(2, 7));
				s.append('-');
				s.append(ws.substring(7));
			}
			s.append(" --> ");

			// decode words[i]
			if (isLiteral(words[i])) {
				// literal
				s.append("literal: ");
				s.append(toBinaryString(words[i]).substring(1));
				f.format(" ---> [from %d to %d] ", firstBitInWord, firstBitInWord + MAX_LITERAL_LENGHT - 1);
				firstBitInWord += MAX_LITERAL_LENGHT;
			} else {
				// sequence
				if (isOneSequence(words[i])) {
					s.append('1');
				} else {
					s.append('0');
				}
				s.append(" block: ");
				s.append(toBinaryString(getLiteralBits(getLiteral(words[i]))).substring(1));
				if (!simulateWAH) {
					s.append(" (bit=");
					int bit = (words[i] & 0x3E000000) >>> 25;
					if (bit == 0) 
						s.append("none");
					else 
						s.append(String.format("%4d", bit - 1));
					s.append(')');
				}
				int count = getSequenceCount(words[i]);
				f.format(" followed by %d blocks (%d bits)", 
						getSequenceCount(words[i]),
						maxLiteralLengthMultiplication(count));
				f.format(" ---> [from %d to %d] ", firstBitInWord, firstBitInWord + (count + 1) * MAX_LITERAL_LENGHT - 1);
				firstBitInWord += (count + 1) * MAX_LITERAL_LENGHT;
			}
			s.append('\n');
		}
		
		// object attributes
		f.format("simulateWAH: %b\n", simulateWAH);
		f.format("last: %d\n", last);
		f.format("size: %s\n", (size == -1 ? "invalid" : Integer.toString(size)));
		f.format("words.length: %d\n", words.length);
		f.format("lastWordIndex: %d\n", lastWordIndex);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}
	
	/**
	 * Test method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ConciseSet s = new ConciseSet(true, 3, 5);
		s.fill(31, 93);
		s.add(1024);
		s.add(1028);
		s.add(MAX_ALLOWED_INTEGER);
		System.out.println(s.debugInfo());
		
		System.out.println(new ConciseSet(false, s).debugInfo());
	}
}

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * This class is a {@link SortedSet} of integers that are internally represented
 * by compressed bitmaps though a RLE (Run-Length Encoding) compression
 * algorithm.
 * <p>
 * The RLE compression method is similar to WAH (<i>Word-Aligned Hybrid
 * compression</i>). However, when compared to WAH, this approach avoids that
 * sparse sets generates sequences of one literal word followed by one sequence
 * word. In this way, we have at most one word for each item to represent plus
 * one word for the first 0's sequence. Put another way, the memory footprint
 * required by a representation of <code>n</code> elements is at most the same
 * as an array of <code>n + 1</code> elements. In WAH, this requires an array
 * of size <code>2 * n</code> elements.
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
 * @see MatrixSet
 */
public class ConciseSet extends AbstractExtendedSet<Integer> implements
		SortedSet<Integer>, Cloneable {
	/**
	 * Compressed bitmap, that is a collection of words. For each word:
	 * <ul>
	 * <li> <tt>1* (0x80000000)</tt> means that it is a 31-bit <i>literal</i>.
	 * <li> <tt>00* (0x00000000)</tt> indicates a <i>sequence</i> made up of
	 * at most one set bit in the first 31 bits, and followed by blocks of 31
	 * 0's. The following 5 bits (<tt>00xxxxx*</tt>) indicates which is the
	 * set bit (<tt>00000</tt> = no set bit, <tt>00001</tt> = LSB,
	 * <tt>11111</tt> = MSB), while the remaining 25 bits indicates the number
	 * of following 0's blocks.
	 * <li> <tt>01* (0x40000000)</tt> indicates a <i>sequence</i> made up of
	 * at most one <i>un</i>set bit in the first 31 bits, and followed by
	 * blocks of 31 1's. (see the <tt>00*</tt> case above).
	 * </ul>
	 */
	// TODO: siccome posso avere i literal 0xFFFFFFFF e 0x80000000, allora le
	// sequence che hanno conta zero potrebbero indicare 2 blocchi, perché una
	// sequence da 1 blocco non può esistere. Similmente, una sequence con bit
	// settato e conta a zero indica due blocchi: se stesso e il successivo.
	//
	// TODO: devo dare la possibilità di inserire più sequence successive, in
	// modo tale da ingrandire l'insieme. A questo punto devo dare tre versioni
	// duplicate di add: una con int, l'altra con long, e l'altra ancora con
	// BigInteger
	private int[] words;

	/**
	 * It represents the position of last set bit in the last append operation
	 * performed via {@link #append(int)}. It is relative to the last literal
	 * word, namely it ranges between <tt>0</tt> and
	 * <tt>MAX_LITERAL_LENGHT</tt>.
	 * <p>
	 * Notice that when {@link #append(int)} generates a 1's sequence,
	 * {@link #lastSetBitOfLastWord} is equal to <tt>MAX_LITERAL_LENGHT - 1</tt>
	 */
	private int lastSetBitOfLastWord;

	/**
	 * When the last word is a literal, it represents the number of bits equal
	 * to 1. If the last word is a sequence of 1's, it contains
	 * <tt>MAX_LITERAL_LENGHT * blocks</tt>. If the last word is a sequence
	 * of 0's, it contains 0.
	 */
	private int numberOfSetBitOfLastWord;

	/**
	 * Most significant set bit within the uncompressed bit string.
	 */
	private int maxSetBit;

	/**
	 * Cardinality of the bit-set. Defined for efficient {@link #size()} calls.
	 */ 
	private int size;

	/**
	 * Index of the lasw word in {@link #words}
	 */ 
	private int lastWordIndex;

	/**
	 * User for <i>fail-fast</i> iterator. It counts the number of operations
	 * that <i>do</i> modify {@link #words}
	 */
	protected int modCount = 0;

	/**
	 * Highest representable integer.
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
	public final static int MAX_ALLOWED_SET_BIT = 31 * (1 << 25) + 30;

	/** 
	 * Lowest representable integer.
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
		maxSetBit = -1;
		size = 0;
		numberOfSetBitOfLastWord = 0;
		lastWordIndex = -1;
		
		// simulate a full literal word for the first append
		lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;
	}
	
	/**
	 * Creates an empty integer set
	 */
	public ConciseSet() {
		reset();
	}

	/**
	 * Creates an empty {@link ConciseSet} instance and then populates it from
	 * an existing integer collection
	 * 
	 * @param c
	 */
	public ConciseSet(Collection<? extends Integer> c) {
		this();
		addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet clone() {
		ConciseSet cloned = (ConciseSet) super.clone();
		if (!isEmpty()) {
			cloned.words = new int[lastWordIndex + 1];
			System.arraycopy(words, 0, cloned.words, 0, lastWordIndex + 1);
		}
		return cloned;
	}

	/**
	 * Calculates the modulus division by 31 in a faster way than using <code>n % 31</code>
	 * <p>
	 * This method of finding modulus division by an integer that is one less
	 * than a power of 2 takes at most O(lg(32)) time. The number of operations
	 * is at most 12 + 9 * ceil(lg(32)).
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
		return (word & 0x80000000) == 0x80000000;
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
	 * with no unset bit
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
	private static int getLiteral(int word) {
		if (isLiteral(word)) 
			return word;
		
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
	private static int makeSequenceWord(
			int bitPosition, 
			boolean isOneSequence, 
			int numberOfSubsequentBlocks) {
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
	 * @return the content of the last element of {@link #words}
	 */
	private int getLastWord() {
		return words[lastWordIndex];
	}
	
	/**
	 * Changes the content of the last element of {@link #words}
	 * 
	 * @param w
	 *            the new value
	 */
	private void setLastWord(int w) {
		words[lastWordIndex] = w;
	}

	/**
	 * Increases the count of the last element of {@link #words}. Last word is
	 * supposed to be a sequence.
	 * 
	 * @param count
	 *            the increment
	 */
	private void increaseLastWordSequenceCount(int count) {
		words[lastWordIndex] += count;
	}
	
	/**
	 * Clears bits from MSB (excluded, since it indicates the word type) to the
	 * specified bit (excluded). Last word is supposed to be a literal one.
	 * 
	 * @param lastSetBit leftmost bit to preserve
	 */
	private void clearBitsAfterInLastWord(int lastSetBit) {
		words[lastWordIndex] &= ALL_ZEROS_LITERAL | (0xFFFFFFFF >>> (31 - lastSetBit));
	}
	
	/**
	 * Sets the bit at the specified position of last element of {@link #words}.
	 * Last word is supposed to be a literal one.
	 * 
	 * @param bit
	 *            position of the bit to set
	 */
	private void setBitInLastWord(int bit) {
		words[lastWordIndex] |= 1 << bit;
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
	 * @param newSetBit
	 *            the absolute position of the bit to set (i.e., the integer to add) 
	 */
	private void append(int newSetBit) {
		// position of the next bit to set within the current literal
		lastSetBitOfLastWord += newSetBit - maxSetBit;

		// if we are outside the current literal, add zeros in
		// between the current word and the new 1-bit literal word
		if (lastSetBitOfLastWord >= MAX_LITERAL_LENGHT) {
			int zeroBlocks = maxLiteralLengthDivision(lastSetBitOfLastWord) - 1;
			if (zeroBlocks == 0) {
				// just add a new literal word to set the new bit
				lastWordIndex++;
				ensureCapacity();
			} else {
				// add a 0's sequence before the new literal
				if (isEmpty() || numberOfSetBitOfLastWord > 1) {
					// the new zero block cannot be merged with the previous literal
					lastWordIndex += 2;
					ensureCapacity();
					if (zeroBlocks == 1) 
						words[lastWordIndex - 1] = ALL_ZEROS_LITERAL;
					else 
						words[lastWordIndex - 1] = makeSequenceWord(0, false, zeroBlocks - 1);
				} else {
					// the zero blocks can be merged with the previous
					// literal that contains only one set bit
					setLastWord(makeSequenceWord(
							1 + Integer.numberOfTrailingZeros(getLastWord()), false, zeroBlocks));
					lastWordIndex++;
					ensureCapacity();
				}
			}

			// prepare the new literal word
			lastSetBitOfLastWord = maxLiteralLengthModulus(lastSetBitOfLastWord);
			setLastWord(ALL_ZEROS_LITERAL);
			numberOfSetBitOfLastWord = 0;
		}

		// set the new bit
		setBitInLastWord(lastSetBitOfLastWord);
		maxSetBit = newSetBit;
		size++;
		numberOfSetBitOfLastWord++;

		// if we have appended the 31st bit within the literal, it is possible that
		// we generated a sequence of 1's --> try to generate the sequence
		if (numberOfSetBitOfLastWord == MAX_LITERAL_LENGHT && compress())
			numberOfSetBitOfLastWord = maxLiteralLengthMultiplication(getSequenceCount(getLastWord()) + 1);
	}

	/**
	 * Assures that the length of {@link #words} is sufficient to contain
	 * {@link #lastWordIndex}.
	 */
	private void ensureCapacity() {
        // find the smallest power of 2 that is greater than "lastWordIndex"
        int capacity = words == null ? 8 : words.length;
        while (capacity <= lastWordIndex)
            capacity <<= 1;

		// nothing to copy
		if (words == null) {
			words = new int[capacity];
			return;
		}
		
		// nothing to change
		if (words.length >= capacity)
			return;
		
		// create the new words
		int[] newWords = new int[capacity];
		System.arraycopy(words, 0, newWords, 0, words.length);
		words = newWords;
	}

	/**
	 * Removes unused allocated words at the end of {@link #words}
	 */
	private void compact() {
		if (words != null && lastWordIndex < words.length - 1) {
			int[] newWords = new int[lastWordIndex + 1];
			System.arraycopy(words, 0, newWords, 0, lastWordIndex + 1);
			words = newWords;
		}
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
			return hasMoreWords() 
				|| (!isLiteral(currentWordCopy) && getSequenceCount(currentWordCopy) > 0);
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
		public final void computeNextLiteral() {
			if (isLiteral(currentWordCopy) || getSequenceCount(currentWordCopy) == 0) {
				// NOTE: when remainingWords < 0, then currentLiteral and
				// currentWordCopy are not valid!!!
				currentWordIndex++;
				if (hasMoreWords()) 
					currentWordCopy = words[currentWordIndex];
				remainingWords--;
			} else {
				// decrease the counter and avoid to generate again the 1-bit literal
				currentWordCopy = getSequenceWithNoBits(currentWordCopy) - 1;
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
			if (isLiteral(word) || isSequenceWithNoBits(word) || getSequenceCount(word) == 0)
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
			if (isSequenceWithNoBits(currentWordCopy))
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
		 * {@link WordIterator#computeNextLiteral()}. <b>NOTE:</b> it supposes
		 * that {@link #hasMoreLiterals()} returns <code>true</code>.
		 */ 
		public final void computeNextLiteral() {
			if (isLiteral(currentWordCopy) || getSequenceCount(currentWordCopy) == 0) { 
				if (--currentWordIndex >= 0)
					currentWordCopy = words[currentWordIndex];
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
	private static int skipSequence(ReverseWordIterator itr1, ReverseWordIterator itr2) {
		int count = 0;
		if (!isLiteral(itr1.currentWordCopy) && !isLiteral(itr2.currentWordCopy)) {
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
		if (itr.endOfWords())
			return;

		// TODO: fare una copia più smart, dove confronto prima
		// itr.currentWordCopy con words dell'oggetto che la contiene e se sono
		// diversi allora faccio "compact", altrimenti faccio una copia
		// brutale...
		
		// iterate over the remaining words
		while (!itr.endOfWords()) {
			// copy the word
			lastWordIndex++;
			setLastWord(itr.currentLiteral);
			compress(); 
			
			// avoid to loop if the current word is a sequence 
			if (!isLiteral(getLastWord()))
				increaseLastWordSequenceCount(skipSequence(itr));

			// next literal
			itr.computeNextLiteral();
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
				return new ConciseSet();
			}

			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.maxSetBit) {
					// op2 is completely hidden by op1
					if (isZeroSequence(op1.words[0]))
						return new ConciseSet();
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
				return new ConciseSet();
			}
			
			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.maxSetBit) {
					// op2 is completely hidden by op1
					if (isOneSequence(op1.words[0]))
						return op1.clone();
					// op2 is left unchanged, but the rest of op1 must be appended...
					
					// ... first, allocate sufficient space for the result
					ConciseSet res = new ConciseSet();
					res.words = new int[op1.lastWordIndex + op2.lastWordIndex + 3];
					res.lastWordIndex = op2.lastWordIndex;
					
					// ... then, copy op2
					System.arraycopy(op2.words, 0, res.words, 0, op2.lastWordIndex + 1);
					
					// ... in turn, decrease the sequence of op1 by the blocks covered by op2
					WordIterator op1Itr = op1.new WordIterator();
					op1Itr.currentWordCopy -= maxLiteralLengthDivision(op2.maxSetBit) + 1;
					if (op1Itr.currentWordCopy < 0)
						op1Itr.computeNextLiteral();
					
					// ... finally, append op1
					res.appendRemainingWords(op1Itr);
					res.size = op1.size + op2.size;
					res.maxSetBit = op1.maxSetBit;
					res.compact();

					// update last word info
					int w = res.getLastWord();
					if (isLiteral(w)) {
						res.numberOfSetBitOfLastWord = getLiteralBitCount(w);
						res.lastSetBitOfLastWord = MAX_LITERAL_LENGHT - Integer.numberOfLeadingZeros(getLiteralBits(w));
					} else {
						res.numberOfSetBitOfLastWord = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
						res.lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;
					}
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
				return new ConciseSet();
			}
			
			/** Used to implement {@link #combineDisjointSets(ConciseSet, ConciseSet)} */
			private ConciseSet oneWayCombineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.maxSetBit) {
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
				return new ConciseSet();
			}
			
			@Override
			public ConciseSet combineDisjointSets(ConciseSet op1, ConciseSet op2) {
				// check whether the first operator starts with a sequence that
				// completely "covers" the second operator
				if (isSequenceWithNoBits(op1.words[0]) 
						&& maxLiteralLengthMultiplication(getSequenceCount(op1.words[0]) + 1) > op2.maxSetBit) {
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
						&& maxLiteralLengthMultiplication(getSequenceCount(op2.words[0]) + 1) > op1.maxSetBit) {
					// op1 is left unchanged by op2
					if (isZeroSequence(op2.words[0]))
						return op1.clone();
					// op1 is cleared by op2
					return new ConciseSet();
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
		
		// allocate a sufficient number of words to contain all possible results
		// NOTE: "+1" is required to allows for the addition of the last word before compacting
		res = new ConciseSet();
		res.words = new int[this.lastWordIndex + otherSet.lastWordIndex + 3];

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			res.lastWordIndex++;
			res.setLastWord(operator.combineLiterals(thisItr.currentLiteral, otherItr.currentLiteral));
			res.compress(); 
			
			// avoid loops when both are sequences and the result is a sequence
			if (!isLiteral(res.getLastWord())) 
				res.increaseLastWordSequenceCount(skipSequence(thisItr, otherItr));

			// next literal
			thisItr.computeNextLiteral();
			otherItr.computeNextLiteral();
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
		res.updateSizeAndMaxSetBit();

		// return the set
		return res;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int intersectionSize(Collection<? extends Integer> other) {
		Statistics.increaseSizeCheckCount();
		
		// empty arguments
		if (other == null || other.isEmpty() || this.isEmpty())
			return 0;

		// single-element intersection
		if (other.size() == 1) 
			return contains(getSingleElement(other)) ? 1 : 0;
		if (size == 1)
			return other.contains(maxSetBit) ? 1 : 0;
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = convert(other);
		
		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.maxSetBit) {
			if (isZeroSequence(this.words[0]))
				return 0;
			return otherSet.size();
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.maxSetBit) {
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
			thisItr.computeNextLiteral();
			otherItr.computeNextLiteral();
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
		return this.maxSetBit - this.size + 1;
	}

	/**
	 * Updates {@link #maxSetBit} and {@link #size} according to the content of
	 * {@link #words}
	 */
	private void updateSizeAndMaxSetBit() {
		// empty element
		if (isEmpty())
			return;

		// initialize data
		maxSetBit = -1;
		size = 0;
		lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;

		// calculate the maximal element and the resulting size
		numberOfSetBitOfLastWord = 0;
		for (int i = 0; i <= lastWordIndex; i++) {
			int w = words[i];
			if (isLiteral(w)) {
				numberOfSetBitOfLastWord = getLiteralBitCount(w);
				maxSetBit += MAX_LITERAL_LENGHT;
			} else {
				numberOfSetBitOfLastWord = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
				maxSetBit += numberOfSetBitOfLastWord;
				if (isZeroSequence(w)) {
					if (isSequenceWithNoBits(w))
						numberOfSetBitOfLastWord = 0;
					else
						numberOfSetBitOfLastWord = 1;
				} else {
					if (!isSequenceWithNoBits(w))
						numberOfSetBitOfLastWord--;
				}
			}
			size += numberOfSetBitOfLastWord;
		}
		if (isLiteral(getLastWord())) {
			int gap = Integer.numberOfLeadingZeros(getLiteralBits(getLastWord())) - 1;
			maxSetBit -= gap;
			lastSetBitOfLastWord -= gap;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer position(int i) {
		if (i < 0 || i >= size)
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
					return firstSetBitInWord + currSetBitInWord;
				}
				
				// skip the 31-bit block
				firstSetBitInWord += MAX_LITERAL_LENGHT;
			} else {
				// number of involved bits (31 * blocks)
				int sequenceLength = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
				
				// check the sequence type
				if (isOneSequence(w)) {
					if (isSequenceWithNoBits(w)) {
						setBitsInCurrentWord = sequenceLength;
						if (position < setBitsInCurrentWord)
							return firstSetBitInWord + position;
					} else {
						setBitsInCurrentWord = sequenceLength - 1;
						if (position < setBitsInCurrentWord)
							// check whether the desired set bit is after the
							// flipped bit (or after the first block)
							return firstSetBitInWord + position + (position < getFlippedBit(w) ? 0 : 1);
					}
				} else {
					if (isSequenceWithNoBits(w)) {
						setBitsInCurrentWord = 0;
					} else {
						setBitsInCurrentWord = 1;
						if (position == 0)
							return firstSetBitInWord + getFlippedBit(w);
					}
				}

				// skip the 31-bit blocks
				firstSetBitInWord += sequenceLength;
			}
			
			// update the number of found set bits
			position -= setBitsInCurrentWord;
		}
		
		throw new RuntimeException("position not found");
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
		final boolean isCurrentWordAllZeros = getLastWord() == ALL_ZEROS_LITERAL;
		final boolean isCurrentWordAllOnes = getLastWord() == ALL_ONES_LITERAL;

		// nothing to merge
		if (!isCurrentWordAllZeros && !isCurrentWordAllOnes)
			return false;
		
		// previous word to merge
		int previousWord = words[lastWordIndex - 1];
		
		// the previous word is a sequence of the same kind --> add one block
		if ((isCurrentWordAllOnes && isOneSequence(previousWord)) 
				|| (isCurrentWordAllZeros && isZeroSequence(previousWord))) {
			lastWordIndex--;
			increaseLastWordSequenceCount(1);
			return true;
		}

		// the previous word is a sequence of a different kind --> cannot merge
		if ((isCurrentWordAllOnes && isZeroSequence(previousWord))
				|| (isCurrentWordAllZeros && isOneSequence(previousWord))) 
			return false;
		
		// try to convert the previous literal word in a sequence and to merge 
		// the current word with it
		if (isCurrentWordAllOnes) {
			// convert set bits to unset bits 
			previousWord = ALL_ZEROS_LITERAL | ~previousWord;
		}
		int previousWordBitCount = getLiteralBitCount(previousWord);
		if (previousWordBitCount == 0) {
			lastWordIndex--;
			setLastWord(makeSequenceWord(0, isCurrentWordAllOnes, 1));
			return true;
		}
		if (previousWordBitCount == 1) {
			int setBit = 1 + Integer.numberOfTrailingZeros(previousWord);
			lastWordIndex--;
			setLastWord(makeSequenceWord(setBit, isCurrentWordAllOnes, 1));
			return true;
		}

		// both the current and the previous words still remains literals
		return false;
	}

	/**
	 * Get the single element of a collection, supposing that it is a singleton!
	 * 
	 * @param other collection
	 * @return single element
	 */
	@SuppressWarnings("unchecked")
	private Integer getSingleElement(Collection<?> other) {
		return (other instanceof SortedSet) 
				? ((SortedSet<Integer>) other).last() 
				: (Integer) other.iterator().next();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet intersection(Collection<? extends Integer> other) {
		Statistics.increaseIntersectionCount();
		if (other == null)
			return new ConciseSet();
		if (other.size() != 1) 
			return performOperation(other, Operator.AND);
		
		// the result definitely contains at most one set bit, thus it is
		// faster to directly set such a bit
		Integer item = getSingleElement(other);
		if (contains(item)) {
			// if the cloned element already contains the item, return itself
			ConciseSet res = new ConciseSet();
			res.append(item);
			return res;
		} 
		return new ConciseSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet union(Collection<? extends Integer> other) {
		Statistics.increaseUnionCount();
		if (other == null)
			return clone();
		if (other.size() != 1) 
			return performOperation(other, Operator.OR);

		// it is faster to directly set the only set bit
		ConciseSet cloned = clone();
		cloned.add(getSingleElement(other));
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet difference(Collection<? extends Integer> other) {
		Statistics.increaseDifferenceCount();
		if (other == null)
			return clone();
		if (other.size() != 1) 
			return performOperation(other, Operator.ANDNOT);

		// it is faster to directly remove the only set bit
		ConciseSet cloned = clone();
		cloned.remove(getSingleElement(other));
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet symmetricDifference(Collection<? extends Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other == null)
			return clone();
		if (other.size() != 1) 
			return performOperation(other, Operator.XOR);
		
		// it is faster to directly flip the only set bit
		ConciseSet cloned = clone();
		cloned.flip(getSingleElement(other));
		return cloned;
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
		
		if (maxSetBit == MIN_ALLOWED_SET_BIT) {
			clear();
			return;
		}
		
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

		// do not complement after the last element (maxSetBit)
		if (isLiteral(getLastWord()))
			clearBitsAfterInLastWord(lastSetBitOfLastWord);

		// remove trailing zeros
		trimZeros();
		if (isEmpty())
			return;

		// update the size 
		size = maxSetBit - size + 1;
		
		// update the maximal element
		maxSetBit = -1;
		for (int i = 0; i <= lastWordIndex; i++) {
			int w = words[i];
			if (isLiteral(w)) 
				maxSetBit += MAX_LITERAL_LENGHT;
			else 
				maxSetBit += maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
		}
		
		// update last word info
		int w = getLastWord();
		lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;
		if (isLiteral(w)) {
			numberOfSetBitOfLastWord = getLiteralBitCount(w);
			int gap = Integer.numberOfLeadingZeros(getLiteralBits(w)) - 1;
			maxSetBit -= gap;
			lastSetBitOfLastWord -= gap;
		} else {
			numberOfSetBitOfLastWord = maxLiteralLengthMultiplication(getSequenceCount(w) + 1);
			if (!isSequenceWithNoBits(w))
				numberOfSetBitOfLastWord--;
		}
	}

	/**
	 * Removes trailing zeros
	 */
	private void trimZeros() {
		// loop over ALL_ZEROS_LITERAL words
		int w;
		do {
			w = getLastWord();
			if (w == ALL_ZEROS_LITERAL) {
				lastWordIndex--;
			} else if (isZeroSequence(w)) {
				if (isSequenceWithNoBits(w)) {
					lastWordIndex--;
				} else {
					// convert the sequence in a 1-bit literal word
					setLastWord(getLiteral(w));
				}
			} else {
				// one sequence or literal
				return;
			}
			if (lastWordIndex < 0) {
				clear();
				return;
			}
		} while (true);
	}
	
	/**
	 * Iterator for set bits of {@link ConciseSet}, from LSB to MSB
	 */
	private class BitIterator implements Iterator<Integer> {
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
			// check for concurrent modification 
			if (initialModCount != modCount)
				throw new ConcurrentModificationException();

			// there are no words
			if (isEmpty())
				return false;

			// there are other literals to read
			if (wordItr.hasMoreLiterals())
				return true;

			// check if we already reached the last bit of the last word
			return getNextSetBit() < MAX_LITERAL_LENGHT;
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
				if (!hasNext())
					throw new NoSuchElementException();

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
					wordItr.computeNextLiteral();
				}
			}

			return rightmostBitOfCurrentWord + nextSetBit;
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
	private class ReverseBitIterator implements Iterator<Integer> {
		private ReverseWordIterator wordItr = new ReverseWordIterator();
		private int rightmostBitOfCurrentWord = maxLiteralLengthMultiplication(maxLiteralLengthDivision(maxSetBit));
		private int nextBitToCheck = lastSetBitOfLastWord;
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
			// check for concurrent modification 
			if (initialModCount != modCount)
				throw new ConcurrentModificationException();

			// there are no words
			if (isEmpty())
				return false;

			// there are other literals to read
			if (wordItr.hasMoreLiterals())
				return true;

			// check if we already reached the last bit of the last word
			return getNextSetBit() >= 0;
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
				if (!hasNext())
					throw new NoSuchElementException();

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
					wordItr.computeNextLiteral();
				}
			}

			return rightmostBitOfCurrentWord + nextSetBit;
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
	public Iterator<Integer> iterator() {
		return new BitIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Integer> descendingIterator() {
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
	public Integer first() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return iterator().next();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return maxSetBit;
	}

	/**
	 * Converts a given {@link Collection} instance into an instance of the
	 * current class
	 * 
	 * @param c
	 *            collection to use to generate the new instance
	 * @return the generated instance. <b>NOTE:</b> if the parameter is already
	 *         an instance of the current class, the method returns the
	 *         parameter.
	 * @see #asConciseSet(Object...)
	 */
	@SuppressWarnings("unchecked")
	public static ConciseSet asConciseSet(Collection<?> c) {
		if (c == null)
			return new ConciseSet();
			
		// useless to convert...
		if (c instanceof ConciseSet)
			return (ConciseSet) c;
		if (c instanceof AbstractExtendedSet.UnmodifiableExtendedSet) {
			ExtendedSet<?> x = ((AbstractExtendedSet.UnmodifiableExtendedSet) c).container();
			if (x instanceof ConciseSet)
				return (ConciseSet) x;
		}
		if (c instanceof AbstractExtendedSet.ExtendedSubSet) {
			ExtendedSet<?> x = ((AbstractExtendedSet.ExtendedSubSet) c).container();
			if (x instanceof ConciseSet)
				return (ConciseSet) ((AbstractExtendedSet.ExtendedSubSet) c).convert(c);
		}

		// try to convert the collection
		ConciseSet res = new ConciseSet();
		if (!c.isEmpty()) {
			// sorted element (in order to use the append() method)
			Collection<Integer> elements;
			if ((c instanceof SortedSet) && (((SortedSet) c).comparator() == null)) {
				// if elements are already ordered according to the natural
				// order of Integer, simply use them
				elements = (SortedSet<Integer>) c;
			} else {
				// sort elements in ascending order
				elements = new ArrayList<Integer>((Collection<? extends Integer>) c);
				Collections.sort((ArrayList<Integer>) elements);
			}

			// append elements
			for (Integer i : elements)
				// check for duplicates
				if (res.maxSetBit != i)
					res.append(i);
		}
		return res;
	}
	
	/**
	 * Converts a given integer array into an instance of the current class
	 * 
	 * @param e
	 *            objects to use to generate the new instance
	 * @return the generated instance. 
	 * @see #asConciseSet(Collection)
	 */
	public static ConciseSet asConciseSet(Object... e) {
		if (e == null)
			return new ConciseSet();
		if (e.length == 1) {
			ConciseSet res = new ConciseSet();
			res.append((Integer) e[0]);
			return res;
		} 
		
		return asConciseSet(Arrays.asList(e));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ConciseSet convert(Collection<?> c) {
		return asConciseSet(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ConciseSet convert(Object... e) {
		return asConciseSet(e);
	}

	/**
	 * Replace the current instance with another {@link ConciseSet} instance
	 * 
	 * @param other {@link ConciseSet} instance to use to replace the current one
	 */
	private void becomeAliasOf(ConciseSet other) {
		if (this == other)
			return;
		this.words = other.words;
		this.size = other.size;
		this.lastSetBitOfLastWord = other.lastSetBitOfLastWord;
		this.maxSetBit = other.maxSetBit;
		this.modCount = other.modCount;
		this.numberOfSetBitOfLastWord = other.numberOfSetBitOfLastWord;
		this.lastWordIndex = other.lastWordIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(Integer e) {
		modCount++;

		final int b = e.intValue();

		// range check
		if (b < MIN_ALLOWED_SET_BIT || b > MAX_ALLOWED_SET_BIT)
			throw new IndexOutOfBoundsException(Integer.toString(b));

		// the element can be simply appended
		if (isEmpty() || b > maxSetBit) {
			append(b);
			return true;
		}

		// check if the element can be put in a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			if (isLiteral(words[i])) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// bit already set
					if ((words[i] & (1 << bitPosition)) != 0)
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
					int bitCount = getLiteralBitCount(words[i]);
					if (bitCount >= MAX_LITERAL_LENGHT - 2)
						break;
						
					// set the bit
					// NOTE: it is always lesser than the maximal element, thus
					// updating maxSetBit is not required
					words[i] |= 1 << bitPosition;
					size++;
					if (i == lastWordIndex)
						numberOfSetBitOfLastWord++;
					return true;
				} 
				
				blockIndex--;
			} else {
				// if we are at the beginning of a sequence, and it is
				// a set bit, the bit already exists
				if (blockIndex == 0 
						&& (getLiteral(words[i]) & (1 << bitPosition)) != 0)
					return false;
				
				// if we are in the middle of a sequence of 1's, the bit already exist
				if (blockIndex > 0 
						&& blockIndex <= getSequenceCount(words[i]) 
						&& isOneSequence(words[i]))
					return false;

				// next word
				blockIndex -= getSequenceCount(words[i]) + 1;
			}
		}
		
		// the bit is in the middle of a sequence or it may cause a literal to
		// become a sequence, thus the "easiest" way to add it is by ORing
		int sizeBefore = size;
		Statistics.increaseUnionCount();
		becomeAliasOf(performOperation(convert(b), Operator.OR));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		modCount++;

		if (o == null || isEmpty())
			return false;

		final int b = ((Integer) o).intValue();

		// the element cannot exist
		if (b > maxSetBit) 
			return false;

		// check if the element can be removed from a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			if (isLiteral(words[i])) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// the bit is already unset
					if ((words[i] & (1 << bitPosition)) == 0)
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
					int bitCount = getLiteralBitCount(words[i]);
					if (bitCount <= 2)
						break;
						
					// unset the bit
					words[i] &= ~(1 << bitPosition);
					size--;
					if (i == lastWordIndex)
						numberOfSetBitOfLastWord--;
					
					// if the bit is the maximal element, update it
					if (b == maxSetBit) {
						int oldLastSetBitOfLastWord = lastSetBitOfLastWord;
						lastSetBitOfLastWord = MAX_LITERAL_LENGHT 
							- Integer.numberOfLeadingZeros(getLiteralBits(getLiteral(words[i])));
						maxSetBit -= oldLastSetBitOfLastWord - lastSetBitOfLastWord;
					}
					return true;
				} 

				blockIndex--;
			} else {
				// if we are at the beginning of a sequence, and it is
				// an unset bit, the bit does not exist
				if (blockIndex == 0 
						&& (getLiteral(words[i]) & (1 << bitPosition)) == 0)
					return false;
				
				// if we are in the middle of a sequence of 0's, the bit does not exist
				if (blockIndex > 0 
						&& blockIndex <= getSequenceCount(words[i]) 
						&& isZeroSequence(words[i]))
					return false;

				// next word
				blockIndex -= getSequenceCount(words[i]) + 1;
			}
		}
		
		// the bit is in the middle of a sequence or it may cause a literal to
		// become a sequence, thus the "easiest" way to remove it by ANDNOTing
		int sizeBefore = size;
		Statistics.increaseDifferenceCount();
		becomeAliasOf(performOperation(convert(b), Operator.ANDNOT));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (isEmpty() || o == null)
			return false;

		final int b = ((Integer)o ).intValue();
		
		// the element is greater than the maximal value
		if (b > maxSetBit) 
			return false;

		// check if the element is within a literal word
		int blockIndex = maxLiteralLengthDivision(b);
		int bitPosition = maxLiteralLengthModulus(b);
		for (int i = 0; i <= lastWordIndex && blockIndex >= 0; i++) {
			if (isLiteral(words[i])) {
				// check if the current literal word is the "right" one
				if (blockIndex == 0) {
					// bit already set
					return (words[i] & (1 << bitPosition)) != 0;
				} 
				blockIndex--;
			} else {
				// if we are at the beginning of a sequence, and it is
				// a set bit, the bit already exists
				if (blockIndex == 0 
						&& (getLiteral(words[i]) & (1 << bitPosition)) != 0)
					return true;
				
				// if we are in the middle of a sequence of 1's, the bit already exist
				if (blockIndex > 0 
						&& blockIndex <= getSequenceCount(words[i]) 
						&& isOneSequence(words[i]))
					return true;

				// next word
				blockIndex -= getSequenceCount(words[i]) + 1;
			}
		}
		
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		Statistics.increaseSizeCheckCount();

		if (c == null || c.isEmpty())
			return true;
		if (isEmpty())
			return false;
		if (c.size() > size)
			return false;
		if (c.size() == 1) 
			return contains(getSingleElement(c));
		
		final ConciseSet otherSet = convert(c);
		
		if (otherSet.maxSetBit > maxSetBit)
			return false;

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
			thisItr.computeNextLiteral();
			otherItr.computeNextLiteral();
		}

		// the intersection is equal to the other set
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Integer> other) {
		Statistics.increaseSizeCheckCount();

		if (other == null || other.isEmpty() || this.isEmpty())
			return false;
		if (other.size() == 1)
			return contains(getSingleElement(other));
		
		final ConciseSet otherSet = convert(other);
		
		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.maxSetBit) {
			if (isZeroSequence(this.words[0]))
				return false;
			return true;
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.maxSetBit) {
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
			thisItr.computeNextLiteral();
			otherItr.computeNextLiteral();
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
		
		Statistics.increaseSizeCheckCount();

		// empty arguments
		if (this.size < minElements || other == null || other.size() < minElements)
			return false;

		// single-element intersection
		if (minElements == 1 && other.size() == 1)
			return contains(getSingleElement(other));
		if (minElements == 1 && this.size == 1)
			return other.contains(maxSetBit);
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = convert(other);
		
		// disjoint sets
		if (isSequenceWithNoBits(this.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(this.words[0]) + 1) > otherSet.maxSetBit) {
			if (isZeroSequence(this.words[0]))
				return false;
			return true;
		}
		if (isSequenceWithNoBits(otherSet.words[0]) 
				&& maxLiteralLengthMultiplication(getSequenceCount(otherSet.words[0]) + 1) > this.maxSetBit) {
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
			thisItr.computeNextLiteral();
			otherItr.computeNextLiteral();
		}

		// return the intersection size
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
		Statistics.increaseIntersectionCount();

		if (isEmpty())
			return false;
		if (c == null || c.isEmpty()) {
			clear();
			return true;
		}
		
		if (c.size() == 1) {
			Integer item = getSingleElement(c);
			if (contains(item)) {
				if (size == 1) 
					return false;
				clear();
				append(item);
			} else {
				clear();
			}
			return true;
		}
		
		int sizeBefore = size;
		becomeAliasOf(performOperation(convert(c), Operator.AND));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		modCount++;
		Statistics.increaseUnionCount();
		if (c == null || c.isEmpty())
			return false;

		if (c.size() == 1) 
			return add(getSingleElement(c));
		
		int sizeBefore = size;
		becomeAliasOf(performOperation(convert(c), Operator.OR));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		modCount++;
		Statistics.increaseDifferenceCount();

		if (c == null || c.isEmpty() || isEmpty())
			return false;
		
		if (c.size() == 1) 
			return remove(getSingleElement(c));
		
		int sizeBefore = size;
		becomeAliasOf(performOperation(convert(c), Operator.ANDNOT));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet empty() {
		return new ConciseSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        if (isEmpty())
            return 0;

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
		Statistics.increaseEqualsCount();

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		ConciseSet other = convert((Collection<?>) obj);
		if (size != other.size || maxSetBit != other.maxSetBit)
			return false;
		if (words != null)
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
		final ConciseSet other = (ConciseSet) o;
		
		// empty set cases
		if (this.isEmpty() && other.isEmpty())
			return 0;
		if (this.isEmpty())
			return -1;
		if (other.isEmpty())
			return 1;
		
		// the word at the end must be the same
		int res = this.maxSetBit - other.maxSetBit;
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
			thisIterator.computeNextLiteral();
			otherIterator.computeNextLiteral();
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
		return (lastWordIndex + 1) / Math.ceil((1 + maxSetBit) / 32D);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) (lastWordIndex + 1) / size;
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
			} else {
				// sequence
				if (isOneSequence(words[i])) {
					s.append('1');
				} else {
					s.append('0');
				}
				s.append(" block: ");
				s.append(toBinaryString(getLiteralBits(getLiteral(words[i]))).substring(1));
				s.append(" (bit=");
				int bit = (words[i] & 0x3E000000) >>> 25;
				if (bit == 0) {
					s.append("none");
				} else {
					s.append(String.format("%4d", bit - 1));
				}
				f.format(") followed by %d blocks (%d bits)", 
						getSequenceCount(words[i]),
						maxLiteralLengthMultiplication(getSequenceCount(words[i])));
			}
			s.append('\n');
		}
		
		// object attributes
		f.format("lastSetBitOfLastWord: %d\n", lastSetBitOfLastWord);
		f.format("numberOfSetBitOfLastWord: %d\n", numberOfSetBitOfLastWord);
		f.format("maxSetBit: %d\n", maxSetBit);
		f.format("size: %d\n", size);
		f.format("words.length: %d\n", words.length);
		f.format("lastWordIndex: %d\n", lastWordIndex);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}
}

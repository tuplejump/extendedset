/* $Id$
 * 
 * (c) 2010 Alessandro Colantonio
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

package it.uniroma3.mat.colantonio.extendedset;

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
 * CONCISE: <i>COmpressed 'N' Composable Integer SEt</i>
 * <p>
 * This class is a {@link SortedSet} of integers that are internally represented
 * by compressed bitmaps though a RLE (Run-Length Encoding) compression
 * algorithm.
 * <p>
 * The RLE compression method is similar to WAH. However, when compared to WAH,
 * this approach avoids that sparse sets generates sequences of one literal word
 * followed by one sequence word. In this way, we have at most one word for each
 * item to represent plus one word for the first 0's sequence. Put another way,
 * the memory footprint required by a representation of <code>n</code>
 * elements is at most the same as an array of <code>n + 1</code> elements. In
 * WAH, this requires an array of size <code>2 * n</code> elements.
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
 * @version 1.0
 * 
 * @see ExtendedSet
 * @see FastSet
 * @see IndexedSet
 */
public class ConciseSet extends ExtendedSet<Integer> implements
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
	 * Most significant set bit within the uncompressed bit string.
	 */
	private int maxSetBit;

	/**
	 * Cardinality of the bit-set. Defined for efficient {@link #size()} calls.
	 */ 
	private int size;

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
	 * Creates an empty integer set
	 */
	public ConciseSet() {
		clear();
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
		if (!isEmpty()) 
			cloned.words = this.words.clone();
		return cloned;
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
	private static int getSequenceWord(
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
	private static int literalBitCount(int word) {
		return Integer.bitCount(word) - 1;
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
	 * Gets the index of the last word in {@code #words}
	 * 
	 * @return the index to use with {@link #words} to get the last word
	 */
	private int lastWordIndex() {
		return words.length - 1;
	}
	
	/**
	 * Sets the bit at the given absolute position within the uncompressed bit
	 * string. The bit <i>must</i> be appendable, that is it must represent an
	 * integer that is strictly greater than the maximum integer in the set.
	 * Note that the parameter range check is performed by the public method
	 * {@link #add(Integer)} and <i>not</i> in this method.
	 * <p>
	 * <b>NOTE:</b> This method assumes that the last element of {@link #words}
	 * (i.e. <code>words[lastWordIndex()]</code>) <i>must</i> be one of the
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
			int zeroBlocks = (lastSetBitOfLastWord / MAX_LITERAL_LENGHT) - 1;
			if (zeroBlocks == 0) {
				// just add a new literal word to set the new bit
				resizeWords(1);
			} else {
				// add a 0's sequence before the new literal
				if (isEmpty()
						|| isOneSequence(words[lastWordIndex()])
						|| literalBitCount(words[lastWordIndex()]) > 1) {  
					// the new zero block cannot be merged with the previous literal
					resizeWords(2);
					if (zeroBlocks == 1) {
						words[lastWordIndex() - 1] = ALL_ZEROS_LITERAL;
					} else {
						words[lastWordIndex() - 1] = getSequenceWord(0, false, zeroBlocks - 1);
					}
				} else {
					// the new zero block can be merged with the previous
					// literal that contains only one set bit
					resizeWords(1);
					words[lastWordIndex()] = ALL_ZEROS_LITERAL;
					compact(lastWordIndex());
					words[lastWordIndex() - 1] += zeroBlocks - 1;
				}
			}

			// prepare the new literal word
			lastSetBitOfLastWord %= MAX_LITERAL_LENGHT;
			words[lastWordIndex()] = ALL_ZEROS_LITERAL;
		}

		// set the new bit
		words[lastWordIndex()] |= 1 << lastSetBitOfLastWord;
		maxSetBit = newSetBit;
		size++;

		// if we have appended the 31st bit within the literal, it is possible that
		// we generated a sequence of 1's --> try to generate the sequence
		if (lastSetBitOfLastWord == (MAX_LITERAL_LENGHT - 1) && compact(lastWordIndex()))
			resizeWords(-1);
	}

	/**
	 * Changes the length of {@link #words}, by appending "empty" words or by
	 * discarding words at the end.
	 * 
	 * @param wordsToAdd
	 *            number of words to add/remove. If it is greater than zero, we
	 *            add empty words. If it is less than zero, we remove words.
	 */
	private void resizeWords(int wordsToAdd) {
		// nothing to change
		if (wordsToAdd == 0)
			return;

		// calculate the new length
		int newLenght = (words == null ? 0 : words.length) + wordsToAdd;

		// create the new words
		int[] newWords = new int[newLenght];
		if (words != null) {
			int wordsToCopy = wordsToAdd < 0 ? newLenght : words.length;
			System.arraycopy(words, 0, newWords, 0, wordsToCopy);
		}

		// replace words
		words = newWords;
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
				remainingWords = words.length - 1;
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
	 * When both word iterators currently point to sequence words, it decreases
	 * these sequences by the least sequence count between them and return such
	 * a count.
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
	 * Appends to {@code #words} the content of another word array
	 * 
	 * @param itr
	 *            iterator that represents the words to copy. The copy will
	 *            start from the current index of the iterator.
	 * @param currentWordIndex
	 *            index of {@code #words} that represent the first word to
	 *            overwrite
	 * @return index of the word after the last copied one
	 */
	private int appendsRemainingWords(WordIterator itr, int currentWordIndex) {
		if (itr.endOfWords())
			return currentWordIndex;

		// iterate over the remaining words
		while (!itr.endOfWords()) {
			// copy the word
			words[currentWordIndex] = itr.currentLiteral;
			if (compact(currentWordIndex)) 
				currentWordIndex--;
			
			// avoid to loop if the current word is a sequence 
			if (!isLiteral(words[currentWordIndex]))
				words[currentWordIndex] += skipSequence(itr);

			// next literal
			currentWordIndex++;
			itr.computeNextLiteral();
		}
		
		// word after the last copied element
		return currentWordIndex;
	}
	
	/**
	 * Possible operations
	 */
	private enum Operator {
		/** bitwise <code>and</code> between literals **/
		AND {
			@Override 
			public int combineLiterals(int literal1, int literal2) {
				return literal1 & literal2;
			}

			@Override 
			public ConciseSet combineEmptySets(ConciseSet op1, ConciseSet op2) {
				return new ConciseSet();
			}
		},
		
		/** bitwise <code>or</code> between literals **/
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
		},

		/** bitwise <code>xor</code> between literals **/
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
		},

		/** bitwise <code>and-not</code> between literals (i.e. <code>X and (not Y)</code>) **/
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
	private ConciseSet performOperation(Collection<?> other, Operator operator) {
		ConciseSet otherSet = asConciseSet(other);
		
		// non-empty arguments
		if (this.isEmpty() || other.isEmpty()) 
			return operator.combineEmptySets(this, otherSet);
		
		// allocate a sufficient number of words to contain all possible results
		// NOTE: "+1" is required to allows for adding the last word and then compacting
		ConciseSet res = new ConciseSet();
		res.words = new int[this.words.length + otherSet.words.length + 1];

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		int resIndex = 0;
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			res.words[resIndex] = operator.combineLiterals(thisItr.currentLiteral, otherItr.currentLiteral);
			if (res.compact(resIndex)) 
				resIndex--;
			
			// avoid loops when both are sequences and the result is a sequence
			if (!isLiteral(res.words[resIndex])) 
				res.words[resIndex] += skipSequence(thisItr, otherItr);

			// next literal
			resIndex++;
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
			resIndex = res.appendsRemainingWords(otherItr, resIndex);
			resIndex = res.appendsRemainingWords(thisItr, resIndex);
			break;
		case ANDNOT:
			resIndex = res.appendsRemainingWords(thisItr, resIndex);
			break;
		default:
			throw new UnsupportedOperationException("unknown operation");
		}

		// remove trailing zeros
		resIndex -= res.trimZeros(resIndex - 1);

		// empty result
		if (resIndex <= 0)
			return new ConciseSet();

		// compact the memory
		res.resizeWords(resIndex - res.words.length);

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
		if (this.isEmpty() || other.isEmpty())
			return 0;

		// single-element intersection
		if (other.size() == 1) 
			return contains(getSingleElement(other)) ? 1 : 0;
		if (size == 1)
			return other.contains(maxSetBit) ? 1 : 0;
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = asConciseSet(other);
		
		// resulting size
		int res = 0;

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			int curRes = literalBitCount(thisItr.currentLiteral & otherItr.currentLiteral);
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
		if (isEmpty()) {
			clear();
			return;
		}

		// initialize data
		maxSetBit = -1;
		size = 0;
		lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;

		// calculate the maximal element and the resulting size
		for (int w : words) {
			if (isLiteral(w)) {
				maxSetBit += MAX_LITERAL_LENGHT;
				size += literalBitCount(w);
			} else {
				int bits = MAX_LITERAL_LENGHT * (getSequenceCount(w) + 1);
				maxSetBit += bits;
				if (isOneSequence(w))
					size += bits;
				if (!isSequenceWithNoBits(w)) {
					if (isOneSequence(w))
						size--;
					else
						size++;
				}
			}
		}
		if (isLiteral(words[lastWordIndex()])) {
			int gap = Integer.numberOfLeadingZeros(getLiteralBits(words[lastWordIndex()])) - 1;
			maxSetBit -= gap;
			lastSetBitOfLastWord -= gap;
		}
	}

	/**
	 * Checks if the <i>literal</i> contained within {@code #words[wordIndex]}
	 * can be merged with the previous word sequences (i.e.
	 * {@code #words[wordIndex - 1]}), hence forming (or updating) a sequence
	 * of 0's or 1's
	 * 
	 * @param wordIndex
	 *            index of the word to check
	 * @return <code>true</code> if the word have been merged
	 */
	private boolean compact(int wordIndex) {
		// nothing to merge
		if (wordIndex == 0)
			return false;
		
		// current word type
		final boolean isCurrentWordAllZeros = words[wordIndex] == ALL_ZEROS_LITERAL;
		final boolean isCurrentWordAllOnes = words[wordIndex] == ALL_ONES_LITERAL;

		// nothing to merge
		if (!isCurrentWordAllZeros && !isCurrentWordAllOnes)
			return false;
		
		// previous word to merge
		int previousWord = words[wordIndex - 1];
		
		// the previous word is a sequence of the same kind --> add one block
		if ((isCurrentWordAllOnes && isOneSequence(previousWord)) 
				|| (isCurrentWordAllZeros && isZeroSequence(previousWord))) {
			words[wordIndex - 1]++;
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
		int previousWordBitCount = literalBitCount(previousWord);
		if (previousWordBitCount == 0) {
			words[wordIndex - 1] = getSequenceWord(0, isCurrentWordAllOnes, 1);
			return true;
		}
		if (previousWordBitCount == 1) {
			int setBit = 1 + Integer.numberOfTrailingZeros(previousWord);
			words[wordIndex - 1] = getSequenceWord(setBit, isCurrentWordAllOnes, 1);
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
	public ConciseSet getIntersection(Collection<? extends Integer> other) {
		Statistics.increaseIntersectionCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.AND);
		
		// the result definitely contains at most one set bit, thus it is
		// faster to directly set such a bit
		final Integer item = getSingleElement(other);
		final ConciseSet cloned = clone();
		if (cloned.contains(item)) {
			// if the cloned element already contains the item, return itself
			if (cloned.size != 1) {
				cloned.clear();
				cloned.append(item);
			}
		} else {
			// return the empty set
			cloned.clear();
		}
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getUnion(Collection<? extends Integer> other) {
		Statistics.increaseUnionCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.OR);

		// it is faster to directly set the only set bit
		final Integer item = getSingleElement(other);
		final ConciseSet cloned = clone();
		cloned.add(item);
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getDifference(Collection<? extends Integer> other) {
		Statistics.increaseDifferenceCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.ANDNOT);

		// it is faster to directly remove the only set bit
		final Integer item = getSingleElement(other);
		final ConciseSet cloned = clone();
		cloned.remove(item);
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getSymmetricDifference(Collection<? extends Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.XOR);
		
		// it is faster to directly flip the only set bit
		final Integer item = getSingleElement(other);
		final ConciseSet cloned = clone();
		if (cloned.contains(item)) 
			cloned.remove(item);
		else
			cloned.add(item);
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getComplement() {
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
		
		if (isEmpty() || maxSetBit == MIN_ALLOWED_SET_BIT) {
			clear();
			return;
		}
		
		// complement each word
		for (int i = 0; i < words.length; i++) {
			if (isLiteral(words[i])) {
				// negate the bits and set the most significant bit to 1
				words[i] = ALL_ZEROS_LITERAL | ~words[i];
			} else {
				// flip the bit before the most significant one to switch the
				// sequence type
				words[i] ^= 0x40000000;
			}
		}

		// do not complement after the last element (maxSetBit)
		if (isLiteral(words[lastWordIndex()])) {
			words[lastWordIndex()] &= ALL_ZEROS_LITERAL
					| (0xFFFFFFFF >>> (MAX_LITERAL_LENGHT - lastSetBitOfLastWord));
			compact(lastWordIndex());
		}

		// remove trailing zeros
		int trimSize = trimZeros(lastWordIndex());
		if (words.length > trimSize) {
			resizeWords(-trimSize);
		} else {
			clear();
			return;
		}

		// update size and maximal element
		updateSizeAndMaxSetBit();
	}

	/**
	 * Removes trailing zeros
	 * 
	 * @param wordIndex
	 *            index of the word of {@link #words} to check
	 * @return number of trimmed words
	 */
	private int trimZeros(int wordIndex) {
		// number of words to trim
		int trimSize = 0;
		
		// loop over ALL_ZEROS_LITERAL words
		int currWord = words[wordIndex];
		while (wordIndex >= trimSize && 
				(currWord == ALL_ZEROS_LITERAL || isZeroSequence(currWord))) { 
			if (currWord == ALL_ZEROS_LITERAL) {
				trimSize++;
			} else if (isZeroSequence(currWord)) {
				// convert the sequence in a 1-bit literal word
				words[wordIndex - trimSize] = getLiteral(currWord);
			}
			if (wordIndex >= trimSize)
				currWord = words[wordIndex - trimSize];
		}

		return trimSize;
	}
	
	/**
	 * Iterator for set bits of {@link ConciseSet}, from LSB to MSB
	 */
	private class BitIterator implements Iterator<Integer> {
		private WordIterator wordItr = new WordIterator();
		private int firstBitOfCurrentWord = 0;
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
					firstBitOfCurrentWord += MAX_LITERAL_LENGHT;
					if (isZeroSequence(wordItr.currentWordCopy)) {
						// skip zeros
						int blocks = getSequenceCount(wordItr.currentWordCopy);
						firstBitOfCurrentWord += MAX_LITERAL_LENGHT * blocks;
						wordItr.currentWordCopy -= blocks;
					}
					nextBitToCheck = 0;
					wordItr.computeNextLiteral();
				}
			}

			return firstBitOfCurrentWord + nextSetBit;
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
	public void clear() {
		modCount++;
		words = null;
		maxSetBit = -1;
		size = 0;
		
		// simulate a full literal word for the first append
		lastSetBitOfLastWord = MAX_LITERAL_LENGHT - 1;
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
	 * Converts a given {@link Collection} instance into a {@link ConciseSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link ConciseSet}
	 *            instance
	 * @return the generated {@link ConciseSet} instance. <b>NOTE:</b> if
	 *         the parameter is an instance of {@link ConciseSet}, the
	 *         method returns this instance.
	 * @see #asConciseSet(Object[])
	 */
	@SuppressWarnings("unchecked")
	public static ConciseSet asConciseSet(Collection<?> c) {
		// useless to convert...
		if (c instanceof ConciseSet)
			return (ConciseSet) c;

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
				elements = new ArrayList<Integer>((Collection<Integer>) c);
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
	 * Converts a given integer array into a {@link ConciseSet}
	 * instance
	 * 
	 * @param e
	 *            integers to put within the new instance of
	 *            {@link ConciseSet}
	 * @return new instance of {@link ConciseSet}
	 * @see #asConciseSet(Collection)
	 */
	public static ConciseSet asConciseSet(Object... e) {
		if (e.length == 1) {
			ConciseSet res = new ConciseSet();
			res.append((Integer) e[0]);
			return res;
		} 
		
		return asConciseSet(Arrays.asList(e));
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
			throw new IndexOutOfBoundsException();

		// the element can be simply appended
		if (isEmpty() || b > maxSetBit) {
			append(b);
			return true;
		}

		// check if the element can be put in a literal word
		int blockIndex = b / MAX_LITERAL_LENGHT;
		int bitPosition = b % MAX_LITERAL_LENGHT;
		for (int i = 0; i < words.length && blockIndex >= 0; i++) {
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
					int bitCount = literalBitCount(words[i]);
					if (bitCount >= MAX_LITERAL_LENGHT - 2)
						break;
						
					// set the bit
					// NOTE: it is always lesser than the maximal element, thus
					// updating maxSetBit is not required
					words[i] |= 1 << bitPosition;
					size++;
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
		becomeAliasOf(performOperation(asConciseSet(b), Operator.OR));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		modCount++;

		final int b = ((Integer) o).intValue();

		// the element cannot exist
		if (isEmpty() || b > maxSetBit) 
			return false;

		// check if the element can be removed from a literal word
		int blockIndex = b / MAX_LITERAL_LENGHT;
		int bitPosition = b % MAX_LITERAL_LENGHT;
		for (int i = 0; i < words.length && blockIndex >= 0; i++) {
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
					int bitCount = literalBitCount(words[i]);
					if (bitCount <= 2)
						break;
						
					// unset the bit
					words[i] &= ~(1 << bitPosition);
					size--;
					
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
		becomeAliasOf(performOperation(asConciseSet(b), Operator.ANDNOT));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		final int b = ((Integer)o ).intValue();
		
		// the element is greater than the maximal value
		if (isEmpty() || b > maxSetBit) {
			return false;
		}

		// check if the element is within a literal word
		int blockIndex = b / MAX_LITERAL_LENGHT;
		int bitPosition = b % MAX_LITERAL_LENGHT;
		for (int i = 0; i < words.length && blockIndex >= 0; i++) {
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
		if (c.size() == 1) {
			if (c instanceof SortedSet)
				return contains(((SortedSet<?>) c).last());
			return contains(c.iterator().next());
		}
		
		final ConciseSet otherSet = asConciseSet(c);
		
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
		
		final ConciseSet otherSet = asConciseSet(other);
		
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
		ConciseSet otherSet = asConciseSet(other);
		
		// resulting size
		int res = 0;

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			int curRes = literalBitCount(thisItr.currentLiteral & otherItr.currentLiteral);
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
		
		if (c != null && c.size() == 1) {
			Integer item;
			if (c instanceof SortedSet)
				item = (Integer) ((SortedSet<?>) c).last();
			else
				item = (Integer) c.iterator().next();
			
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
		becomeAliasOf(performOperation(asConciseSet(c), Operator.AND));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		modCount++;

		Statistics.increaseUnionCount();
		if (c != null && c.size() == 1) {
			if (c instanceof SortedSet)
				// ConciseSet included...
				return add((Integer) ((SortedSet<?>) c).last());
			return add(c.iterator().next());
		}
		
		int sizeBefore = size;
		becomeAliasOf(performOperation(asConciseSet(c), Operator.OR));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		modCount++;

		Statistics.increaseDifferenceCount();
		if (c != null && c.size() == 1) {
			if (c instanceof SortedSet)
				return remove(((SortedSet<?>) c).last());
			return remove(c.iterator().next());
		}
		
		int sizeBefore = size;
		becomeAliasOf(performOperation(asConciseSet(c), Operator.ANDNOT));
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
	public ExtendedSet<Integer> emptySet() {
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
        for (int w : words)
            h = 31 * h + w;

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
		if (getClass() != obj.getClass())
			return false;
		final ConciseSet other = (ConciseSet) obj;
		if (size != other.size || maxSetBit != other.maxSetBit)
			return false;
		if (words != null)
	        for (int i = 0; i < words.length; i++)
	            if (words[i] != other.words[i])
	                return false;
		return true;
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
		return words.length / Math.ceil((1D + maxSetBit) / 32D);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) words.length / size;
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
		for (int i = 0; i < words.length; i++) {
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
						MAX_LITERAL_LENGHT * getSequenceCount(words[i]));
			}
			s.append('\n');
		}
		
		// object attributes
		f.format("lastSetBitOfLastWord: %d\n", lastSetBitOfLastWord);
		f.format("maxSetBit: %d\n", maxSetBit);
		f.format("size: %d\n", size);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}
}

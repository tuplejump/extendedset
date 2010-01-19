/* $Id: ConciseSet.java 6 2010-01-19 18:21:06Z cocciasik $
 * 
 * (c) 2010 Alessandro Colantonio
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
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * COncISE: <i>COmpressed Integer SEt</i>
 * <p>
 * {@link SortedSet} of integers, internally represented by compressed bitmaps
 * though a RLE (Run-Length Encoding) compression algorithm.
 * <p>
 * Benefits: When compared to WAH, this approach avoids that sparse sets
 * generates 1 literal followed by 1 sequence word. In this way, we have at most
 * 1 word for each item to represent + 1 word for the first 0 sequence---that
 * is, the memory footprint required by a representation of the elements through
 * an array such as int[#elements] + 1. In WAH, this requires an array of size 2 *
 * #elements.
 * <p>
 * The returned iterator is <i>fail-fast</i>: if the collection is structurally
 * modified at any time after the iterator is created, the iterator will throw a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * The iterator throws a {@link ConcurrentModificationException} on a
 * best-effort basis. Therefore, it would be wrong to write a program that
 * depended on this exception for its correctness: <i>the fail-fast behavior of
 * iterators should be used only to detect bugs.</i>
 * 
 * @author Alessandro Colantonio
 * @author <a href="mailto:colanton@mat.uniroma3.it">colanton@mat.uniroma3.it</a>
 * @author <a href="http://ricerca.mat.uniroma3.it/users/colanton">http://ricerca.mat.uniroma3.it/users/colanton</a>
 * 
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
	private int modCount = 0;

	/**
	 * Highest representable bit within a literal.
	 * <p>
	 * The number of bits required to represent the longest sequence of 0's or
	 * 1's is 27, namely
	 * <tt>ceil(log<sub>2</sub>((Integer.MAX_VALUE - 31) / 31)) = 27</tt>.
	 * Indeed, at least one literal exists, and the other bits may all be 0's or
	 * 1's, that is <tt>Integer.MAX_VALUE - 31</tt>
	 * 
	 * If we use:
	 * <ul>
	 * <li> 2 bits for the sequence type;
	 * <li> 5 bits to indicate which bit is set;
	 * </ul>
	 * then <tt>32 - 5 - 2 = 25</tt> is the number of available bits to
	 * represent the maximum sequence of 0's and 1's. Thus, the maximal bit that
	 * can be set is represented by a number of 0's equals to
	 * <tt>31 * (1 << 25)</tt>, followed by a literal with 30 0's and the
	 * MSB (31st bit) equal to 1
	 */
	public final static int MAX_ALLOWED_SET_BIT = 31 * (1 << 25) + 30;

	/** 
	 * Lowest representable bit within a literal.
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
	 * Create an empty bit-string
	 */
	public ConciseSet() {
		clear();
	}

	/**
	 * Create a new bit-string and populate it from an existing integer
	 * collection
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
	 * Check if a word is a literal one
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
	 * Check if a word contains a sequence of 1's
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
	 * Check if a word contains a sequence of 0's
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
	 * Check if a word contains a sequence of 0's with no set bit, or 1's with
	 * no unset bit
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
	 * Get the number of blocks of 1's or 0's stored in a sequence word
	 * 
	 * @param word
	 *            word to check
	 * @return number of blocks that follow the first block of 31 bits
	 */
	private static int getSequenceCount(int word) {
		// get the 25 LSB bits
		return word & 0x01FFFFFF;
	}

	/**
	 * Clear the (un)set bit in a sequence
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
	 * Get a literal word that represents the first 31 bits of the given the
	 * word.
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
	 * Generate a sequence word.
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
	 * Number of set bit within the literal word
	 * 
	 * @param word
	 *            literal word
	 * @return number of set bit within the literal word
	 */
	private static int literalBitCount(int word) {
		return Integer.bitCount(word) - 1;
	}

	/**
	 * Get the bits contained within the literal word
	 * 
	 * @param word literal word
	 * @return the literal word with the most significant bit cleared
	 */
	private static int getLiteralBits(int word) {
		return ALL_ONES_WITHOUT_MSB & word;
	}
	
	/**
	 * Index of the last word in {@code #words}
	 * 
	 * @return the index to use with {@link #words} to get the last word
	 */
	private int lastWordIndex() {
		return words.length - 1;
	}
	
	/**
	 * Set the bit at the given absolute position within the uncompressed bit string.
	 * <p>
	 * <b>NOTE:</b> The parameter range check is performed by {@link #add(Integer)}.
	 * <p>
	 * <b>NOTE:</b> This method assumes that words[lastWordIndex()] <i>must</i> be:
	 * <ul>
	 * <li> a literal word with some bit set;
	 * <li> a one sequence.
	 * </ul>
	 * Hence, the last word should <i>not</i> be:
	 * <ul>
	 * <li> a literal word containing only zeros;
	 * <li> a zero sequence.
	 * </ul>
	 * 
	 * @param newSetBit absolute position of the bit to set
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

		// if we have appended the 31th bit within the literal, it is possible that
		// we generated a sequence of 1's --> try to generate a one sequence
		if (lastSetBitOfLastWord == (MAX_LITERAL_LENGHT - 1) && compact(lastWordIndex())) {
			resizeWords(-1);
		}
	}

	/**
	 * Change the length of {@link #words}
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
	 * Iterate over words.
	 * <p>
	 * It iterates over the <i>literals</i> represented by
	 * {@link ConciseSet#words}. In particular, when a word is a
	 * sequence, it "expands" the sequence to all the represented literals. It
	 * also maintains a modified copy of the sequence that stores the number of
	 * the remaining blocks to iterate.
	 */
	private class WordIterator {
		private int currentWordIndex;	// index of the current word
		private int currentWordCopy;	// copy of the current word
		private int currentLiteral;		// literal contained within the current word
		private int remainingWords;		// remaining words from "index" to the end
		
		{
			if (words != null) {
				currentWordIndex = 0;
				currentWordCopy = words[currentWordIndex];
				currentLiteral = getLiteral(currentWordCopy);
				remainingWords = words.length - 1;
			} else {
				currentWordIndex = 0;
				currentWordCopy = 0;
				currentLiteral = 0;
				remainingWords = -1;
			}
		}
		
		/**
		 * Check whether other literals to analyze exist
		 * 
		 * @return <code>true</code> if {@link #currentWordIndex} is out of
		 *         the bounds of {@link #words}
		 */
		public final boolean endOfWords() {
			return remainingWords < 0;
		}

		/**
		 * Check whether other literals to analyze exist
		 * 
		 * @return <code>true</code> if there are literals to iterate
		 */
		public final boolean hasMoreLiterals() {
			return hasMoreWords() 
				|| (!isLiteral(currentWordCopy) && getSequenceCount(currentWordCopy) > 0);
		}

		/**
		 * Check whether other words to analyze exist
		 * 
		 * @return <code>true</code> if there are words to iterate
		 */
		public final boolean hasMoreWords() {
			return remainingWords > 0;
		}
		
		/**
		 * Prepare the next literal {@link #currentLiteral}, increase
		 * {@link #currentWordIndex} and decrease {@link #remainingWords} if
		 * necessary, and modify the copy of the current word
		 * {@link #currentWordCopy}
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
	 * When both word iterators currently point to sequence words, decrease
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
	private static int skipTheSmallerSequence(WordIterator itr1, WordIterator itr2) {
		int leastCount = 0;
		if (isSequenceWithNoBits(itr1.currentWordCopy) 
				&& isSequenceWithNoBits(itr2.currentWordCopy)) {
			leastCount = Math.min(
					getSequenceCount(itr1.currentWordCopy),
					getSequenceCount(itr2.currentWordCopy));
			if (leastCount > 0) {
				// increase sequence counter
				itr1.currentWordCopy -= leastCount;
				itr2.currentWordCopy -= leastCount;
			}
		} 
		return leastCount;
	}
	
	/**
	 * When the given word iterator currently points to a sequence word,
	 * decrease this sequence to 0 and return such a count.
	 * 
	 * @param itr
	 *            word iterator
	 * @return the sequence count of the sequence word pointed by the given
	 *         iterator
	 * @see #skipTheSmallerSequence(WordIterator, WordIterator)
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
	 * Append to {@code #words} the content of {@code #words} from another word
	 * array
	 * 
	 * @param itr
	 *            iterator that represents the words to copy. The copy will
	 *            start from the current index of the iterator
	 * @param currentWordIndex
	 *            index of the destination word array that represent the first
	 *            word to override
	 * @return index of the word after the last copied word
	 */
	private int copyRemainingWords(WordIterator itr, int currentWordIndex) {
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
		/** bitwise <code>and</code> **/
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
		
		/** bitwise <code>or</code> **/
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

		/** bitwise <code>xor</code> **/
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

		/** bitwise <code>and-not</code> **/
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
		 * Perform the operation represented by the enum on the given literals
		 * 
		 * @param literal1
		 *            left operand
		 * @param literal2
		 *            right operand
		 * @return literal representing the result of the specified operation
		 */
		public abstract int combineLiterals(int literal1, int literal2); 
		
		/**
		 * Perform the specified operation when one or both operands are empty set
		 * <p>
		 * <b>NOTE: one or both the operands <i>MUST</i> be empty!!!</b>
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
	 * Perform the given operation over the bit-sets
	 * 
	 * @param other
	 *            {@link ConciseSet} instance that represent the right
	 *            operand
	 * @param operator
	 *            operator
	 * @return result of the operation
	 */
	private ConciseSet performOperation(ExtendedSet<Integer> other, Operator operator) {
		ConciseSet otherSet = asCompressedIntegerSet(other);
		
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
			
			// Avoid loops when both are sequence and the result is a sequence
			if (!isLiteral(res.words[resIndex])) 
				res.words[resIndex] += skipTheSmallerSequence(thisItr, otherItr);

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
			resIndex = res.copyRemainingWords(otherItr, resIndex);
			resIndex = res.copyRemainingWords(thisItr, resIndex);
			break;
		case ANDNOT:
			resIndex = res.copyRemainingWords(thisItr, resIndex);
			break;
		default:
			throw new UnsupportedOperationException();
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
	@Override
	public int intersectionSize(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();
		
		// empty arguments
		if (this.isEmpty() || other.isEmpty())
			return 0;

		// single-element intersection
		if (other.size() == 1)
			return contains(other.last()) ? 1 : 0;
		if (size == 1)
			return other.contains(maxSetBit) ? 1 : 0;
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = asCompressedIntegerSet(other);
		
		// resulting size
		int res = 0;

		// scan "this" and "other"
		WordIterator thisItr = this.new WordIterator();
		WordIterator otherItr = otherSet.new WordIterator();
		while (!thisItr.endOfWords() && !otherItr.endOfWords()) {
			// perform the operation
			int curRes = literalBitCount(thisItr.currentLiteral & otherItr.currentLiteral);
			res += curRes;

			// Avoid loops when both are sequence and the result is a sequence
			if (curRes == ALL_ZEROS_WITHOUT_MSB || curRes == ALL_ONES_WITHOUT_MSB) 
				res += curRes * skipTheSmallerSequence(thisItr, otherItr);

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
	 * Update {@link #maxSetBit} and {@link #size} according to the content of
	 * {@link #words}
	 */
	private void updateSizeAndMaxSetBit() {
		// empty element
		if (isEmpty()) {
			clear();
			return;
		}

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
	 * Check if the <i>literal</i> contained within {@code #words[wordIndex]}
	 * can be transformed into a sequence and merged with the sequences of 0's
	 * and 1's of {@code #words[wordIndex - 1]}
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
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getIntersection(ExtendedSet<Integer> other) {
		Statistics.increaseIntersectionCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.AND);
		
		// the result definitely contains at most one set bit, thus it is
		// faster to directly set such a bit
		final Integer item = other.last();
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
	public ConciseSet getUnion(ExtendedSet<Integer> other) {
		Statistics.increaseUnionCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.OR);

		// it is faster to directly set the only set bit
		final Integer item = other.last();
		final ConciseSet cloned = clone();
		cloned.add(item);
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getDifference(ExtendedSet<Integer> other) {
		Statistics.increaseDifferenceCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.ANDNOT);

		// it is faster to directly remove the only set bit
		final Integer item = other.last();
		final ConciseSet cloned = clone();
		cloned.remove(item);
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConciseSet getSymmetricDifference(ExtendedSet<Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other.size() != 1) 
			return performOperation(other, Operator.XOR);
		
		// it is faster to directly flip the only set bit
		final Integer item = other.last();
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
			replaceThis(new ConciseSet());
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
	 * Remove trailing zeros
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
	 * Iterator for set bits of {@link ConciseSet}
	 */
	private class BitIterator implements Iterator<Integer> {
		private WordIterator wordItr = new WordIterator();
		private int firstBitOfCurrentWord = 0;
		private int nextBitToCheck = 0;
		private int initialModCount = modCount;

		/**
		 * Get the next bit in the current literal
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
			// contextually keep the word iterator updated!
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
		// natural order of integers
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
	 * Convert a given {@link Collection} instance to a {@link ConciseSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link ConciseSet}
	 *            instance
	 * @return the generated {@link ConciseSet} instance. <b>NOTE:</b> if
	 *         the parameter is an instance of {@link ConciseSet}, the
	 *         method returns this instance.
	 * @see #asCompressedIntegerSet(Object[])
	 */
	@SuppressWarnings("unchecked")
	public static ConciseSet asCompressedIntegerSet(Collection<?> c) {
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
	 * Convert a given integer element to a {@link ConciseSet}
	 * instance
	 * 
	 * @param e
	 *            integer to put within the new instance of
	 *            {@link ConciseSet}
	 * @return new instance of {@link ConciseSet}
	 * @see #asCompressedIntegerSet(Collection)
	 */
	public static ConciseSet asCompressedIntegerSet(Object... e) {
		if (e.length == 1) {
			ConciseSet res = new ConciseSet();
			res.append((Integer) e[0]);
			return res;
		} 
		
		return asCompressedIntegerSet(Arrays.asList(e));
	}

	/**
	 * Replace the current instance with another {@link ConciseSet} instance
	 * 
	 * @param other {@link ConciseSet} instance to use to replace the current one
	 */
	private void replaceThis(ConciseSet other) {
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
		replaceThis(performOperation(asCompressedIntegerSet(b), Operator.OR));
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
		replaceThis(performOperation(asCompressedIntegerSet(b), Operator.ANDNOT));
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
		
		final ConciseSet otherSet = asCompressedIntegerSet(c);
		
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
				skipTheSmallerSequence(thisItr, otherItr);

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
	public boolean containsAny(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();

		if (other == null || other.isEmpty() || this.isEmpty())
			return false;
		if (other.size() == 1)
			return contains(other.last());
		
		final ConciseSet otherSet = asCompressedIntegerSet(other);
		
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
			skipTheSmallerSequence(thisItr, otherItr);

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
	public boolean containsAtLeast(ExtendedSet<Integer> other, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		
		Statistics.increaseSizeCheckCount();

		// empty arguments
		if (this.size < minElements || other == null || other.size() < minElements)
			return false;

		// single-element intersection
		if (minElements == 1 && other.size() == 1)
			return contains(other.last());
		if (minElements == 1 && this.size == 1)
			return other.contains(maxSetBit);
		
		// convert the other set in order to perform a more complex intersection
		ConciseSet otherSet = asCompressedIntegerSet(other);
		
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
				res += curRes * skipTheSmallerSequence(thisItr, otherItr);

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
		replaceThis(performOperation(asCompressedIntegerSet(c), Operator.AND));
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
		replaceThis(performOperation(asCompressedIntegerSet(c), Operator.OR));
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
		replaceThis(performOperation(asCompressedIntegerSet(c), Operator.ANDNOT));
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
	@Override
	public int compareTo(ExtendedSet<Integer> o) {
		//TODO: controllare che funzioni...
		Iterator<Integer> thisIterator = this.descendingIterator();
		Iterator<Integer> otherIterator = o.descendingIterator();
		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			Integer thisItem = thisIterator.next();
			Integer otherItem = otherIterator.next();
			int res = thisItem.compareTo(otherItem);
			if (res != 0)
				return res;
		}
		return thisIterator.hasNext() ? 1 : (otherIterator.hasNext() ? -1 : 0);
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
	public List<? extends ConciseSet> powerSet(int min,
			int max) {
		return (List<? extends ConciseSet>) super.powerSet(min, max);
	}
	
	/**
	 * Generate the 32-bit binary representation of a given word (debug only)
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
	 * Print debug info (used by {@link #main(String[])})
	 * 
	 * @return a string that describes the internal representation of the instance
	 */
	@SuppressWarnings("unused")
	private String debugInfo() {
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

	/**
	 * Check if a {@link ConciseSet} and a {@link TreeSet} contains
	 * the same elements
	 * 
	 * @param bits
	 *            bit-set to check
	 * @param items
	 *            {@link TreeSet} instance that must contain the same elements
	 *            of the bit-set
	 * @return <code>true</code> if the given {@link ConciseSet} and
	 *         {@link TreeSet} are equals in terms of contained elements
	 */
	@SuppressWarnings("unused")
	private static boolean checkContent(ConciseSet bits, TreeSet<Integer> items) {
		if (bits.size() != items.size())
			return false;
		if (bits.isEmpty())
			return true;
		for (Integer i : bits) 
			if (!items.contains(i)) 
				return false;
		return bits.last().equals(items.last());
	}
	
	/**
	 * Simple test for the method {@link #append(int)}
	 * <p>
	 * It appends sequential numbers, thus generating 2 blocks of 1's
	 */
	@SuppressWarnings("unused")
	private static void testForAppendSimple() {
		ConciseSet bits = new ConciseSet();
		for (int i = 0; i < 62; i++) {
			System.out.format("Appending %d...\n", i);
			bits.append(i);
			System.out.println(bits.debugInfo());
		}
	}
	
	/**
	 * Another test for the method {@link #append(int)}
	 * <p>
	 * It tests some particular cases
	 */
	@SuppressWarnings("unused")
	private static void testForAppendComplex() {
		ConciseSet bits = new ConciseSet();
		TreeSet<Integer> items = new TreeSet<Integer>();

		// elements to append
		items.add(1000);
		items.add(1001);
		items.add(1023);
		items.add(2000);
		items.add(2046);
		for (int i = 0; i < 62; i++) 
			items.add(2048 + i);
		items.add(2158);
		items.add(MAX_ALLOWED_SET_BIT);

		// append elements
		for (Integer i : items) {
			System.out.format("Appending %d...\n", i);
			bits.append(i);
			System.out.println(bits.debugInfo());
		}
		
		// check the result
		if (checkContent(bits, items)) {
			System.out.println("OK!");
		} else {
			System.out.println("ERRORS!");
		}
	}
	
	/**
	 * Random test for the method {@link #append(int)}
	 * <p>
	 * It adds randomly generated numbers
	 */
	@SuppressWarnings("unused")
	private static void testForAppendRandom() {
		ConciseSet bits = new ConciseSet();
		TreeSet<Integer> items = new TreeSet<Integer>();

		// random number generator
		Random rnd = new Random();

		bits.clear();
		items.clear();
		System.out.println("SPARSE ITEMS");
		for (int i = 0; i < 10000; i++)
			items.add(rnd.nextInt(1000000000 + 1));
		for (Integer i : items)
			bits.append(i);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());

		bits.clear();
		items.clear();
		System.out.println("DENSE ITEMS");
		for (int i = 0; i < 10000; i++)
			items.add(rnd.nextInt(10000 + 1));
		for (Integer i : items)
			bits.append(i);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());

		bits.clear();
		items.clear();
		System.out.println("MORE DENSE ITEMS");
		for (int i = 0; i < 2000; i++)
			items.add(rnd.nextInt(310 + 1));
		for (int i = 0; i < 2000; i++)
			items.add(714 + rnd.nextInt(805 - 714 + 1));
		for (int i = 0; i < 2000; i++)
			items.add(850 + rnd.nextInt(900 - 850 + 1));
		for (int i = 0; i < 4000; i++)
			items.add(700 + rnd.nextInt(100000 - 700 + 1));
		for (Integer e : items)
			bits.append(e);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());
	}
	
	/**
	 * Simple test for the method {@link #getIntersection(ExtendedSet)}
	 */
	@SuppressWarnings("unused")
	private static void testForIntersectionSimple() {
		System.out.println("FIRST SET");
		ConciseSet bitsLeft = new ConciseSet();
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		itemsLeft.add(1);
		itemsLeft.add(2);
		itemsLeft.add(3);
		itemsLeft.add(100);
		itemsLeft.add(1000);
		for (Integer i : itemsLeft)
			bitsLeft.append(i);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		ConciseSet bitsRight = new ConciseSet();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();
		itemsRight.add(100);
		itemsRight.add(101);
		for (Integer i : itemsRight)
			bitsRight.append(i);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("INTERSECTION SET");
		ConciseSet bitsIntersection = bitsLeft.getIntersection(bitsRight);
		TreeSet<Integer> itemsIntersection = new TreeSet<Integer>(itemsLeft);
		itemsIntersection.retainAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsIntersection, itemsIntersection));
		System.out.println("Original items: " + itemsIntersection);
		System.out.println(bitsIntersection.debugInfo());
	}
	
	/**
	 * More complex test for the methods {@link #getIntersection(ExtendedSet)}
	 * and {@link #intersectionSize(ExtendedSet)}
	 */
	@SuppressWarnings("unused")
	private static void testForIntersectionComplex() {
		// generate items to intersect completely at random
		Random rnd = new Random();

		System.out.println("FIRST SET");
		ConciseSet bitsLeft = new ConciseSet();
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		for (int i = 0; i < 30; i++)
			itemsLeft.add(rnd.nextInt(1000 + 1));
		for (int i = 0; i < 1000; i++)
			itemsLeft.add(rnd.nextInt(200 + 1));
		bitsLeft.addAll(itemsLeft);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		ConciseSet bitsRight = new ConciseSet();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();
		for (int i = 0; i < 30; i++)
			itemsRight.add(rnd.nextInt(1000 + 1));
		for (int i = 0; i < 1000; i++)
			itemsRight.add(150 + rnd.nextInt(300 - 150 + 1));
		bitsRight.addAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("INTERSECTION SET");
		ConciseSet bitsIntersection = bitsLeft.getIntersection(bitsRight);
		TreeSet<Integer> itemsIntersection = new TreeSet<Integer>(itemsLeft);
		itemsIntersection.retainAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsIntersection, itemsIntersection));
		System.out.println("Original items: " + itemsIntersection);
		System.out.println(bitsIntersection.debugInfo());
		
		System.out.println("INTERSECTION SIZE");
		System.out.println("Correct: " + (itemsIntersection.size() == bitsLeft.intersectionSize(bitsRight)));	
	}
	
	/**
	 * Simple test for the method {@link #getUnion(ExtendedSet)}
	 */
	@SuppressWarnings("unused")
	private static void testForUnionSimple() {
		System.out.println("FIRST SET");
		ConciseSet bitsLeft = new ConciseSet();
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		itemsLeft.add(1);
		itemsLeft.add(2);
		itemsLeft.add(30000);
		for (Integer i : itemsLeft)
			bitsLeft.append(i);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		ConciseSet bitsRight = new ConciseSet();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();
		itemsRight.add(100);
		itemsRight.add(101);
		itemsRight.add(MAX_ALLOWED_SET_BIT);
		for (int i = 0; i < 62; i++)
			itemsRight.add(341 + i);
		for (Integer i : itemsRight) 
			bitsRight.append(i);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("UNION SET");
		ConciseSet bitsUnion = bitsLeft.getUnion(bitsRight);
		TreeSet<Integer> itemsUnion = new TreeSet<Integer>(itemsLeft);
		itemsUnion.addAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsUnion, itemsUnion));
		System.out.println("Original items: " + itemsUnion);
		System.out.println(bitsUnion.debugInfo());
		
		System.out.println("UNION SIZE");
		System.out.println("Correct: " + (itemsUnion.size() == bitsLeft.unionSize(bitsRight)));
	}
	
	/**
	 * Simple test for the method {@link #getComplement()}
	 */
	@SuppressWarnings("unused")
	private static void testForComplement() {
		System.out.println("Original");
		ConciseSet bits = new ConciseSet();
		bits.append(1);
		bits.append(2);
		bits.append(30000);
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());
	}
	
	/**
	 * Simple test for the methods:
	 * <ul>
	 * <li> {@link #add(Integer)}
	 * <li> {@link #remove(Object)}
	 * <li> {@link #addAll(Collection)}
	 * <li> {@link #removeAll(Collection)}
	 * <li> {@link #retainAll(Collection)}
	 * <li> {@link #getSymmetricDifference(ExtendedSet)}
	 * <li> {@link #getComplement()}
	 * </ul>
	 */
	@SuppressWarnings("unused")
	private static void testForMixedStuff() {
		ConciseSet bitsLeft = new ConciseSet();
		bitsLeft.add(1);
		bitsLeft.add(100);
		bitsLeft.add(2);
		bitsLeft.add(3);
		bitsLeft.add(2);
		bitsLeft.add(100);
		System.out.println("A: " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		ConciseSet bitsRight = new ConciseSet();
		bitsRight.add(1);
		bitsRight.add(1000000);
		bitsRight.add(2);
		bitsRight.add(30000);
		bitsRight.add(1000000);
		System.out.println("B: " + bitsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("A.getSymmetricDifference(B): " + bitsLeft.getSymmetricDifference(bitsRight));
		System.out.println(bitsLeft.getSymmetricDifference(bitsRight).debugInfo());

		System.out.println("A.getComplement(): " + bitsLeft.getComplement());
		System.out.println(bitsLeft.getComplement().debugInfo());

		bitsLeft.removeAll(bitsRight);
		System.out.println("A.removeAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.addAll(bitsRight);
		System.out.println("A.addAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.retainAll(bitsRight);
		System.out.println("A.retainAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.remove(1);
		System.out.println("A.remove(1): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());
	}
	
	/**
	 * Stress test for {@link #add(Integer)}
	 * <p> 
	 * It starts from a very sparse set (most of the words will be 0's 
	 * sequences) and progressively become very dense (words first
	 * become 0's sequences with 1 set bit and there will be almost one 
	 * word per item, then words become literals, and finally they 
	 * become 1's sequences and drastically reduce in number)
	 */
	@SuppressWarnings("unused")
	private static void testForAdditionStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// add 100000 random numbers
		for (int i = 0; i < 100000; i++) {
			// random number to add
			int item = rnd.nextInt(10000 + 1);

			// keep the previous results
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// add the element
			System.out.format("Adding %d...\n", item);
			boolean itemExistsBefore = currentItems.contains(item);
			boolean itemAdded = currentItems.add(item);
			boolean itemExistsAfter = currentItems.contains(item);
			boolean bitExistsBefore = currentBits.contains(item);
			boolean bitAdded = currentBits.add(item);
			boolean bitExistsAfter = currentBits.contains(item);
			if (itemAdded ^ bitAdded) {
				System.out.println("wrong add() result");
				return;
			}
			if (itemExistsBefore ^ bitExistsBefore) {
				System.out.println("wrong contains() before");
				return;
			}
			if (itemExistsAfter ^ bitExistsAfter) {
				System.out.println("wrong contains() after");
				return;
			}

			// check the list of elements
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("add() error");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = asCompressedIntegerSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}

			// check the union size
			ConciseSet singleBitSet = new ConciseSet();
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.unionSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());
		
		System.out.println();
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Stress test for {@link #remove(Object)}
	 * <p> 
	 * It starts from a very dense set (most of the words will be 1's 
	 * sequences) and progressively become very sparse (words first
	 * become 1's sequences with 1 unset bit and there will be few 
	 * words per item, then words become literals, and finally they 
	 * become 0's sequences and drastically reduce in number)
	 */
	@SuppressWarnings("unused")
	private static void testForRemovalStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// create a 1-filled bitset
		currentBits.append(10001);
		currentBits.complement();
		currentItems.addAll(currentBits);
		if (currentItems.size() != 10001) {
			System.out.println("Unexpected error!");
			return;
		}
		
		// remove 100000 random numbers
		for (int i = 0; i < 100000 & !currentBits.isEmpty(); i++) {
			// random number to remove
			int item = rnd.nextInt(10000 + 1);

			// keep the previous results
			previousBits = currentBits;
			currentBits = currentBits.clone();
			
			// remove the element
			System.out.format("Removing %d...\n", item);
			boolean itemExistsBefore = currentItems.contains(item);
			boolean itemRemoved = currentItems.remove(item);
			boolean itemExistsAfter = currentItems.contains(item);
			boolean bitExistsBefore = currentBits.contains(item);
			boolean bitRemoved = currentBits.remove(item);
			boolean bitExistsAfter = currentBits.contains(item);
			if (itemRemoved ^ bitRemoved) {
				System.out.println("wrong remove() result");
				return;
			}
			if (itemExistsBefore ^ bitExistsBefore) {
				System.out.println("wrong contains() before");
				return;
			}
			if (itemExistsAfter ^ bitExistsAfter) {
				System.out.println("wrong contains() after");
				return;
			}

			// check the list of elements
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("remove() error");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				
				return;
			}
			
			// check the representation
			ConciseSet otherBits = asCompressedIntegerSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}

			// check the union size
			ConciseSet singleBitSet = new ConciseSet();
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.differenceSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());

		System.out.println();
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Random operations on random sets.
	 * <p>
	 * It randomly chooses among {@link #addAll(Collection)},
	 * {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}, and
	 * perform the operation over random sets
	 */
	@SuppressWarnings("unused")
	private static void testForRandomOperationsStress() {
		ConciseSet bitsLeft = new ConciseSet();
		ConciseSet bitsRight = new ConciseSet();
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// random operation loop
		for (int i = 0; i < 100000; i++) {
			System.out.print("Test " + i + ": ");
			
			// new set
			itemsRight.clear();
			bitsRight.clear();
			final int size = 1 + rnd.nextInt(10000);
			final int min = 1 + rnd.nextInt(10000 - 1);
			final int max = min + rnd.nextInt(10000 - min + 1);
			for (int j = 0; j < size; j++) {
				int item = min + rnd.nextInt(max - min + 1);
				itemsRight.add(item);
				bitsRight.add(item);
			}
			if (!checkContent(bitsRight, itemsRight)) {
				System.out.println("ERROR!");
				System.out.println("Same elements: " + (itemsRight.toString().equals(bitsRight.toString())));
				System.out.println("Original items: " + itemsRight);
				System.out.println(bitsRight.debugInfo());
				return;
			}
			
			// perform the random operation with the previous set
			int operationSize = 0;
			switch (1 + rnd.nextInt(3)) {
			case 1:
				System.out.format(" union of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.unionSize(bitsRight);
				itemsLeft.addAll(itemsRight);
				bitsLeft.addAll(bitsRight);
				break;

			case 2:
				System.out.format(" difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.differenceSize(bitsRight);
				itemsLeft.removeAll(itemsRight);
				bitsLeft.removeAll(bitsRight);
				break;

			case 3:
				System.out.format(" intersection of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.intersectionSize(bitsRight);
				itemsLeft.retainAll(itemsRight);
				bitsLeft.retainAll(bitsRight);
				break;
			}
			
			// check the list of elements
			if (!checkContent(bitsLeft, itemsLeft)) {
				System.out.println("OPERATION ERROR!");
				System.out.println("Same elements: " + 
						(itemsLeft.toString().equals(bitsLeft.toString())));
				System.out.println("Original items: " + itemsLeft);
				System.out.println(bitsLeft.debugInfo());
				return;
			}
			
			// check the representation
			if (asCompressedIntegerSet(itemsLeft).hashCode() != bitsLeft.hashCode()) {
				System.out.println("REPRESENTATION ERROR!");
				System.out.println(bitsLeft.debugInfo());
				System.out.println(asCompressedIntegerSet(itemsLeft).debugInfo());
				return;
			}

			// check the union size
			if (itemsLeft.size() != operationSize) {
				System.out.println("SIZE ERROR");
				System.out.println("Wrong size: " + operationSize);
				System.out.println("Correct size: " + itemsRight.size());
				System.out.println(bitsLeft.debugInfo());
				return;
			}
			
			System.out.println("done.");
		}
	}
	
	/**
	 * Stress test (addition) for {@link #subSet(Integer, Integer)}
	 */
	@SuppressWarnings("unused")
	private static void testForSubSetAdditionStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		for (int j = 0; j < 100000; j++) {
			// keep the previous result
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// generate a new subview
			int min = rnd.nextInt(10000);
			int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
			int item = min + rnd.nextInt((max - 1) - min + 1);
			System.out.println("Adding " + item + " to the subview from " + min + " to " + max + " - 1");
			SortedSet<Integer> subBits = currentBits.subSet(min, max);
			SortedSet<Integer> subItems = currentItems.subSet(min, max);
			boolean subBitsResult = subBits.add(item);
			boolean subItemsResult = subItems.add(item);

			if (subBitsResult != subItemsResult 
					|| subBits.size() != subItems.size() 
					|| !subBits.toString().equals(subItems.toString())) {
				System.out.println("Subset error!");
				return;
			}			
			
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("Subview not correct!");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = asCompressedIntegerSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Stress test (addition) for {@link #subSet(Integer, Integer)}
	 */
	@SuppressWarnings("unused")
	private static void testForSubSetRemovalStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		// create a 1-filled bitset
		currentBits.append(10001);
		currentBits.complement();
		currentItems.addAll(currentBits);
		if (currentItems.size() != 10001) {
			System.out.println("Unexpected error!");
			return;
		}

		Random rnd = new Random(System.currentTimeMillis());

		for (int j = 0; j < 100000; j++) {
			// keep the previous result
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// generate a new subview
			int min = rnd.nextInt(10000);
			int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
			int item = rnd.nextInt(10000 + 1);
			System.out.println("Removing " + item + " from the subview from " + min + " to " + max + " - 1");
			SortedSet<Integer> subBits = currentBits.subSet(min, max);
			SortedSet<Integer> subItems = currentItems.subSet(min, max);
			boolean subBitsResult = subBits.remove(item);
			boolean subItemsResult = subItems.remove(item);

			if (subBitsResult != subItemsResult 
					|| subBits.size() != subItems.size() 
					|| !subBits.toString().equals(subItems.toString())) {
				System.out.println("Subset error!");
				return;
			}
			
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("Subview not correct!");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = asCompressedIntegerSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.getSummary());
	}

	/**
	 * Random operations on random sub sets.
	 * <p>
	 * It randomly chooses among all operations and performs the operation over
	 * random sets
	 */
	@SuppressWarnings("unused")
	private static void testForSubSetRandomOperationsStress() {
		ConciseSet bits = new ConciseSet();
		ConciseSet bitsPrevious = new ConciseSet();
		TreeSet<Integer> items = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// random operation loop
		for (int i = 0; i < 100000; i++) {
			System.out.print("Test " + i + ": ");
			
			// new set
			bitsPrevious = bits.clone();
			if (!bitsPrevious.toString().equals(bits.toString()))
				throw new RuntimeException("clone() error!");
			bits.clear();
			items.clear();
			final int size = 1 + rnd.nextInt(10000);
			final int min = 1 + rnd.nextInt(10000 - 1);
			final int max = min + rnd.nextInt(10000 - min + 1);
			final int minSub = 1 + rnd.nextInt(10000 - 1);
			final int maxSub = minSub + rnd.nextInt(10000 - minSub + 1);
			for (int j = 0; j < size; j++) {
				int item = min + rnd.nextInt(max - min + 1);
				bits.add(item);
				items.add(item);
			}
			
			// perform base checks 
			SortedSet<Integer> bitsSubSet = bits.subSet(minSub, maxSub);
			SortedSet<Integer> itemsSubSet = items.subSet(minSub, maxSub);
			if (!bitsSubSet.toString().equals(itemsSubSet.toString())) {
				System.out.println("toString() difference!");
				System.out.println("value: " + bitsSubSet.toString());
				System.out.println("actual: " + itemsSubSet.toString());
				return;
			}
			if (bitsSubSet.size() != itemsSubSet.size()) {
				System.out.println("size() difference!");
				System.out.println("value: " + bitsSubSet.size());
				System.out.println("actual: " + itemsSubSet.size());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}
			if (!itemsSubSet.isEmpty() && (!bitsSubSet.first().equals(itemsSubSet.first()))) {
				System.out.println("first() difference!");
				System.out.println("value: " + bitsSubSet.first());
				System.out.println("actual: " + itemsSubSet.first());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}
			if (!itemsSubSet.isEmpty() && (!bitsSubSet.last().equals(itemsSubSet.last()))) {
				System.out.println("last() difference!");
				System.out.println("value: " + bitsSubSet.last());
				System.out.println("actual: " + itemsSubSet.last());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}

			// perform the random operation 
			boolean resBits = false;
			boolean resItems = false;
			boolean exceptionBits = false;
			boolean exceptionItems = false;
			switch (1 + rnd.nextInt(4)) {
			case 1:
				System.out.format(" addAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.addAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.addAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 2:
				System.out.format(" removeAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.removeAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.removeAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 3:
				System.out.format(" retainAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.retainAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.retainAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 4:
				System.out.format(" clear() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					bitsSubSet.clear();
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					itemsSubSet.clear();
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;
			}			
			
			if (exceptionBits != exceptionItems) {
				System.out.println("Incorrect exception!");
				return;
			}

			if (resBits != resItems) {
				System.out.println("Incorrect results!");
				System.out.println("resBits: " + resBits);
				System.out.println("resItems: " + resItems);
				return;
			}
			
			if (!checkContent(bits, items)) {
				System.out.println("Subview not correct!");
				System.out.format("min: %d, max: %d, minSub: %d, maxSub: %d\n", min, max, minSub, maxSub);
				System.out.println("Same elements: " + (items.toString().equals(bits.toString())));
				System.out.println("Original items: " + items);
				System.out.println(bits.debugInfo());
				System.out.println(bitsPrevious.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = asCompressedIntegerSet(items);
			if (otherBits.hashCode() != bits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.format("min: %d, max: %d, minSub: %d, maxSub: %d\n", min, max, minSub, maxSub);
				System.out.println(bits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(bitsPrevious.debugInfo());
				return;
			}
			
			System.out.println("done.");
		}
	}
	
	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int testCase = 14;
		
		if (args != null && args.length == 1) {
			try {
				testCase = new Integer(args[0]);
			} catch (NumberFormatException ignore) {
				// nothing to do
			}
		}
		
		switch (testCase) {
		case 1:
			testForAppendSimple();
			break;
		case 2:
			testForAppendComplex();
			break;
		case 3:
			testForAppendRandom();
			break;
		case 4:
			testForIntersectionSimple();
			break;
		case 5:
			testForIntersectionComplex();
			break;
		case 6:
			testForUnionSimple();
			break;
		case 7:
			testForComplement();
			break;
		case 8:
			testForMixedStuff();
			break;
		case 9:
			testForAdditionStress();
			break;
		case 10:
			testForRemovalStress();
			break;
		case 11:
			testForRandomOperationsStress();
			break;
		case 12:
			testForSubSetAdditionStress();
			break;
		case 13:
			testForSubSetRemovalStress();
			break;
		case 14:
			testForSubSetRandomOperationsStress();
			break;
		default:
			System.out.println("Unknown test case!");
		}
	}
}

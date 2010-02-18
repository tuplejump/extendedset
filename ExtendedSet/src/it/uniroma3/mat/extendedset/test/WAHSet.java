package it.uniroma3.mat.extendedset.test;

import java.text.DecimalFormat;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Integer set internally represented by a bit-set, using a RLE compression
 * algorithm
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class WAHSet extends AbstractSet<Integer> implements
		SortedSet<Integer>, Cloneable {
	/*
	 * Compressed bit-string. 
	 * For each word: 
	 * - 1* (0x80000000) means that it is a 31-bit literal 
	 * - 01* (0x40000000) means that indicates a sequence of blocks of 1's 
	 *   (each block represents 31 bits) 
	 * - 00* (0x00000000) means that indicates a sequence of blocks of 0's 
	 *   (each block represents 31 bits)
	 */
	private int[] words;

	// If the last word is a literal, it represents the last set bit.
	// It is relative position, namely in [0, MAXIMUM_LITERAL_LENGHT]
	private int maxSetBitInLastWord;

	// maximum set bit
	private int maxSetBit;

	// cardinality of the bit-set (defined only to have an efficient size()
	// method)
	private int size;

	// maximum number of representable bits within a literal
	private final static int MAXIMUM_LITERAL_LENGHT = 31;

	// literal representing all bits set to 1 or to 0
	private final static int ALL_ONES_LITERAL = 0xFFFFFFFF;
	private final static int ALL_ZEROES_LITERAL = 0x80000000;

	// not valid index
	private final static int NOT_VALID_INDEX = -1;

	/**
	 * Creates an empty bit-string
	 */
	public WAHSet() {
		words = null;
		size = 0;
		maxSetBitInLastWord = 0;
		maxSetBit = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WAHSet clone() {
		WAHSet cloned = null;
		try {
			cloned = (WAHSet) super.clone();
			if (!isEmpty())
				cloned.words = Arrays.copyOf(words, words.length);
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		return cloned;
	}

	/**
	 * Checks if a word is a literal
	 * 
	 * @param word
	 * @return
	 */
	private boolean isLiteral(int word) {
		return (word & 0x80000000) == 0x80000000;
	}

	/**
	 * Checks if a word is a sequence of 1's
	 * 
	 * @param word
	 * @return
	 */
	private boolean isOneSequence(int word) {
		return (word & 0xC0000000) == 0x40000000;
	}

	/**
	 * Checks if a word is a sequence of 0's
	 * 
	 * @param word
	 * @return
	 */
	private boolean isZeroSequence(int word) {
		return (word & 0xC0000000) == 0x00000000;
	}

	/**
	 * Checks if a word is a sequence of 0's or 1's
	 * 
	 * @param word
	 * @return
	 */
	private boolean isSequence(int word) {
		return (word & 0x80000000) == 0x00000000;
	}

	/**
	 * Gets the number of blocks of 1's or 0's stored in a sequence word
	 * 
	 * @param word
	 */
	private int sequenceCount(int word) {
		return word & 0x3FFFFFFF;
	}

	/**
	 * Gets the bits of the given literal word
	 * 
	 * @param word
	 */
	private int literalBits(int word) {
		return word & 0x7FFFFFFF;
	}

	/**
	 * Index of the last word in <code>words</code>
	 * 
	 * @return
	 */
	private int lastWordIndex() {
		return words.length - 1;
	}

	/**
	 * Generates the literal corresponding to the <code>bitIndex</code> bit
	 * 
	 * @param bitIndex
	 * @return
	 */
	private int literal(int bitIndex) {
		return 0x80000000 | (1 << bitIndex);
	}

	/**
	 * Generates a word representing a sequence of <code>number</code> blocks
	 * of 1's
	 * 
	 * @param number
	 * @return
	 */
	private int oneSequence(int number) {
		return 0x40000000 | number;
	}

	/**
	 * Generates a word representing a sequence of <code>number</code> blocks
	 * of 0's
	 * 
	 * @param number
	 * @return
	 */
	private int zeroSequence(int number) {
		return number;
	}

	/**
	 * Sets the i-th bit
	 * 
	 * @param newBit
	 */
	private void append(int newBit) {
		if (newBit < 0 || newBit < maxSetBit)
			throw new IndexOutOfBoundsException();

		// new bit to set within the last word
		maxSetBitInLastWord += newBit - maxSetBit;

		// check if we are within the current literal
		if ((isEmpty() || isLiteral(words[lastWordIndex()]))
				&& maxSetBitInLastWord < MAXIMUM_LITERAL_LENGHT) {
			// the bit can be set within the current literal
			if (!isEmpty()) {
				words[lastWordIndex()] |= literal(maxSetBitInLastWord);
			} else {
				// first append
				resizeWords(1);
				words[lastWordIndex()] = literal(maxSetBitInLastWord);
			}

			// check if the new set bit "fills" the current literals of 1's
			if (words[lastWordIndex()] == ALL_ONES_LITERAL) {
				if (lastWordIndex() > 0
						&& isOneSequence(words[lastWordIndex() - 1])) {
					// increase the 1's sequence of the previous word
					words[lastWordIndex() - 1]++;
					resizeWords(-1);
				} else {
					// convert the current word to a sequence of 1's
					words[lastWordIndex()] = oneSequence(1);
				}
			}
		} else {
			// the bit must be set in a new block
			int zeroBlocks = (maxSetBitInLastWord / MAXIMUM_LITERAL_LENGHT) - 1;
			if (isEmpty())
				zeroBlocks++;
			if (zeroBlocks > 0) {
				resizeWords(2);
				words[lastWordIndex() - 1] = zeroSequence(zeroBlocks);
			} else {
				resizeWords(1);
			}
			maxSetBitInLastWord %= MAXIMUM_LITERAL_LENGHT;
			words[lastWordIndex()] = literal(maxSetBitInLastWord);
		}

		// update the last set bit
		maxSetBit = newBit;
		size++;
	}

	/**
	 * Changes the length of {@link #words}
	 * 
	 * @param wordsToAdd
	 *            number of words to add/remove.
	 *            <p>
	 *            If <code>wordsToAdd</code> is greater than zero, we add empty words.
	 *            If <code>wordsToAdd</code> is lesser than zero, we remove words.
	 */
	private void resizeWords(int wordsToAdd) {
		// nothing to change
		if (wordsToAdd == 0)
			return;

		// calculate the new length
		int newLenght = (words == null ? 0 : words.length) + wordsToAdd;
		if (newLenght < 1)
			throw new RuntimeException("Too many words removed");

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
	 * Possible operations
	 */
	private enum Operation {
		/** bitwise <code>and</code> */
		AND, 
		
		/** bitwise <code>or</code> */
		OR, 

		/** bitwise <code>xor</code> */
		XOR, 

		/** bitwise <code>and not</code> */
		ANDNOT,
	}

	/**
	 * Performs the given operation on the two words
	 * 
	 * @param word1
	 * @param word2
	 * @return
	 */
	private int combineLiterals(int word1, int word2, Operation op) {
		// convert the sequences to literal
		final int literal1 = isLiteral(word1) ? word1
				: (isZeroSequence(word1) ? ALL_ZEROES_LITERAL
						: ALL_ONES_LITERAL);
		final int literal2 = isLiteral(word2) ? word2
				: (isZeroSequence(word2) ? ALL_ZEROES_LITERAL
						: ALL_ONES_LITERAL);

		// perform the operation
		switch (op) {
		case AND:
			return ALL_ZEROES_LITERAL | (literal1 & literal2);
		case OR:
			return ALL_ZEROES_LITERAL | (literal1 | literal2);
		case XOR:
			return ALL_ZEROES_LITERAL | (literal1 ^ literal2);
		case ANDNOT:
			return ALL_ZEROES_LITERAL | (literal1 & (~literal2));
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Performs the given operation over the bit-sets
	 * 
	 * @param other
	 * @param opt
	 * @return
	 */
	private WAHSet getOperation(WAHSet other, Operation opt) {
		// empty arguments
		if (this.isEmpty() || other.isEmpty()) {
			switch (opt) {
			case AND:
				return new WAHSet();
			case OR:
			case XOR:
				if (!this.isEmpty() && other.isEmpty())
					return this.clone();
				if (this.isEmpty() && !other.isEmpty())
					return other.clone();
				return new WAHSet();
			case ANDNOT:
				if (!this.isEmpty() && other.isEmpty())
					return this.clone();
				return new WAHSet();
			default:
				throw new UnsupportedOperationException();
			}
		}

		// final intersection set
		WAHSet res = new WAHSet();

		// create a sufficient number of words to contain all possible results
		res.words = new int[this.words.length + other.words.length];

		// scan "this" and "other"
		int thisWordIndex = 0;
		int otherWordIndex = 0;
		int resWordIndex = 0;
		int thisWord = this.words[thisWordIndex];
		int otherWord = other.words[otherWordIndex];
		while (thisWordIndex < this.words.length
				&& otherWordIndex < other.words.length) {
			// perform the operation
			res.words[resWordIndex] = combineLiterals(thisWord, otherWord, opt);
			resWordIndex = compact(resWordIndex, res.words) + 1;

			// if both are sequence, the result is definitely a sequence and we
			// can avoid to loop
			if (isSequence(thisWord) && isSequence(otherWord)) {
				int duplicates = Math.min(sequenceCount(thisWord),
						sequenceCount(otherWord)) - 1;
				if (duplicates > 0) {
					res.words[resWordIndex - 1] += duplicates;
					thisWord -= duplicates;
					otherWord -= duplicates;
				}
			}

			// next word pair
			if (isLiteral(thisWord)
					|| (isSequence(thisWord) && sequenceCount(thisWord) == 1)) {
				if (++thisWordIndex < this.words.length)
					thisWord = this.words[thisWordIndex];
			} else {
				thisWord--;
			}
			if (isLiteral(otherWord)
					|| (isSequence(otherWord) && sequenceCount(otherWord) == 1)) {
				if (++otherWordIndex < other.words.length)
					otherWord = other.words[otherWordIndex];
			} else {
				otherWord--;
			}
		}

		/*
		 * if one bit-set is greater than the other one, we add the remaining
		 * bits depending on the given operation. NOTE: thisWord and otherWord
		 * can contain decreased counters, thus the first word must be taken
		 * from these variables
		 */
		int remainingThisWords = this.words.length - thisWordIndex;
		int remainingOtherWords = other.words.length - otherWordIndex;
		if (remainingThisWords > 0
				&& (opt == Operation.OR || opt == Operation.XOR || opt == Operation.ANDNOT)) {
			// if the previous word and the current word are sequences, then
			// merge them
			if ((isOneSequence(res.words[resWordIndex - 1]) && isOneSequence(thisWord))
					|| (isZeroSequence(res.words[resWordIndex - 1]) && isZeroSequence(thisWord))) {
				res.words[resWordIndex - 1] += sequenceCount(thisWord);
				resWordIndex--;
			} else {
				res.words[resWordIndex] = thisWord;
			}
			System.arraycopy(this.words, thisWordIndex + 1, res.words,
					resWordIndex + 1, remainingThisWords - 1);
			resWordIndex += remainingThisWords;
		}
		if (remainingOtherWords > 0
				&& (opt == Operation.OR || opt == Operation.XOR)) {
			// if the previous word and the current word are sequences, then
			// merge them
			if ((isOneSequence(res.words[resWordIndex - 1]) && isOneSequence(otherWord))
					|| (isZeroSequence(res.words[resWordIndex - 1]) && isZeroSequence(otherWord))) {
				res.words[resWordIndex - 1] += sequenceCount(otherWord);
				resWordIndex--;
			} else {
				res.words[resWordIndex] = otherWord;
			}
			System.arraycopy(other.words, otherWordIndex + 1, res.words,
					resWordIndex + 1, remainingOtherWords - 1);
			resWordIndex += remainingOtherWords;
		}

		// remove trailing zeroes
		if (isZeroSequence(res.words[resWordIndex - 1]))
			resWordIndex--;

		// empty result
		if (resWordIndex < 0)
			return new WAHSet();

		// compact the memory
		int[] newResWord = new int[resWordIndex];
		System.arraycopy(res.words, 0, newResWord, 0, resWordIndex);
		res.words = newResWord;

		// update size and max element
		res.updateSizeAndMaxElement();

		// return the set
		return res;
	}

	/**
	 * Computes the intersection size.
	 * <p>
	 * This is faster than calling {@link #getIntersection(WAHSet)}
	 * and then {@link #size()}
	 * 
	 * @param other
	 * @return intersection size
	 */
	public int intersectionSize(WAHSet other) {
		// empty arguments
		if (this.isEmpty() || other.isEmpty())
			return 0;

		// resulting size
		int res = 0;

		// scan "this" and "other"
		int thisWordIndex = 0;
		int otherWordIndex = 0;
		int thisWord = this.words[thisWordIndex];
		int otherWord = other.words[otherWordIndex];
		while (thisWordIndex < this.words.length
				&& otherWordIndex < other.words.length) {
			// perform the operation
			int curRes = Integer.bitCount(combineLiterals(thisWord, otherWord,
					Operation.AND)) - 1;
			res += curRes;

			// if both are sequence, the result is definitely a sequence and we
			// can avoid to loop
			if (isSequence(thisWord) && isSequence(otherWord)) {
				int duplicates = Math.min(sequenceCount(thisWord),
						sequenceCount(otherWord)) - 1;
				if (duplicates > 0) {
					res += curRes * duplicates;
					thisWord -= duplicates;
					otherWord -= duplicates;
				}
			}

			// next word pair
			if (isLiteral(thisWord)
					|| (isSequence(thisWord) && sequenceCount(thisWord) == 1)) {
				if (++thisWordIndex < this.words.length)
					thisWord = this.words[thisWordIndex];
			} else {
				thisWord--;
			}
			if (isLiteral(otherWord)
					|| (isSequence(otherWord) && sequenceCount(otherWord) == 1)) {
				if (++otherWordIndex < other.words.length)
					otherWord = other.words[otherWordIndex];
			} else {
				otherWord--;
			}
		}

		// return the intersection size
		return res;
	}

	/**
	 * Computes the union size.
	 * <p>
	 * This is faster than calling {@link #getUnion(WAHSet)}
	 * and then {@link #size()}
	 * 
	 * @param other
	 * @return the union size
	 */
	public int unionSize(WAHSet other) {
		return this.size + other.size - intersectionSize(other);
	}

	/**
	 * Computes the symmetric difference size.
	 * <p>
	 * This is faster than calling {@link #getSymmetricDifference(WAHSet)}
	 * and then {@link #size()}
	 * 
	 * @param other
	 * @return symmetric difference size
	 */
	public int symmetricDifferenceSize(WAHSet other) {
		return this.size + other.size - 2 * intersectionSize(other);
	}

	/**
	 * Computes the difference size.
	 * <p>
	 * This is faster than calling {@link #getDifference(WAHSet)}
	 * and then {@link #size()}
	 * 
	 * @param other
	 * @return difference set size
	 */
	public int differenceSize(WAHSet other) {
		return this.size - intersectionSize(other);
	}

	/**
	 * Computes the complement size.
	 * <p>
	 * This is faster than calling {@link #getComplement()}
	 * and then {@link #size()}
	 * 
	 * @return complement set size
	 */
	public int complementSize() {
		return this.maxSetBit - this.size + 1;
	}

	/**
	 * Updates <code>maxSetBit</code> and <code>size</code> according to the
	 * <code>words</code> content
	 */
	private void updateSizeAndMaxElement() {
		maxSetBit = 0;
		size = 0;

		// empty element
		if (words == null) {
			maxSetBitInLastWord = 0;
			return;
		}

		// calculate the maximal element and the resulting size
		for (int w : words) {
			if (isLiteral(w)) {
				maxSetBit += MAXIMUM_LITERAL_LENGHT;
				size += Integer.bitCount(w) - 1;
			} else {
				maxSetBit += MAXIMUM_LITERAL_LENGHT * sequenceCount(w);
				if (isOneSequence(w))
					size += MAXIMUM_LITERAL_LENGHT * sequenceCount(w);
			}
		}
		if (size > 0 && isLiteral(words[lastWordIndex()])) {
			maxSetBit -= Integer
					.numberOfLeadingZeros(literalBits(words[lastWordIndex()]));
			maxSetBitInLastWord = MAXIMUM_LITERAL_LENGHT
					- Integer
							.numberOfLeadingZeros(literalBits(words[lastWordIndex()]));
		} else {
			maxSetBitInLastWord = NOT_VALID_INDEX;
		}
	}

	/**
	 * Checks if the <i>literal</i> <code>currWords[currWordIndex]</code> can
	 * be transformed to a sequence and merged with the sequences of 0's and 1's
	 * of <code>currWords[currWordIndex - 1]</code>
	 * 
	 * @param currWordIndex
	 * @param currWords
	 * @return <code>currWordIndex - 1</code> if the word can be completely
	 *         merged, <code>currWordIndex</code> otherwise.
	 */
	private int compact(int currWordIndex, int[] currWords) {
		// transform the literal into a sequence of 0's
		if (currWords[currWordIndex] == ALL_ZEROES_LITERAL) {
			if (currWordIndex == 0 || isLiteral(currWords[currWordIndex - 1])
					|| isOneSequence(currWords[currWordIndex - 1])) {
				currWords[currWordIndex] = zeroSequence(1);
				return currWordIndex;
			}
			if (isZeroSequence(currWords[currWordIndex - 1])) {
				currWords[currWordIndex - 1]++;
				return currWordIndex - 1;
			}
			return currWordIndex;
		}

		// transform the literal into a sequence of 1's
		if (currWords[currWordIndex] == ALL_ONES_LITERAL) {
			if (currWordIndex == 0 || isLiteral(currWords[currWordIndex - 1])
					|| isZeroSequence(currWords[currWordIndex - 1])) {
				currWords[currWordIndex] = oneSequence(1);
				return currWordIndex;
			}
			if (isOneSequence(currWords[currWordIndex - 1])) {
				currWords[currWordIndex - 1]++;
				return currWordIndex - 1;
			}
			return currWordIndex;
		}

		// unmodified literal
		return currWordIndex;
	}

	/**
	 * Generates the intersection set (bitwise and)
	 * 
	 * @param other
	 * @return intersection set
	 */
	public WAHSet getIntersection(WAHSet other) {
		return getOperation(other, Operation.AND);
	}

	/**
	 * Generates the union set (bitwise or)
	 * 
	 * @param other
	 * @return union set
	 */
	public WAHSet getUnion(WAHSet other) {
		return getOperation(other, Operation.OR);
	}

	/**
	 * Generates the difference set (bitwise and-not)
	 * 
	 * @param other
	 * @return difference set
	 */
	public WAHSet getDifference(WAHSet other) {
		return getOperation(other, Operation.ANDNOT);
	}

	/**
	 * Generates the symmetric difference set (bitwise xor)
	 * 
	 * @param other
	 * @return symmetric difference set
	 */
	public WAHSet getSymmetricDifference(WAHSet other) {
		return getOperation(other, Operation.XOR);
	}

	/**
	 * Generates the complement set (bitwise not)
	 * 
	 * @return complement set
	 */
	public WAHSet getComplement() {
		WAHSet cloned = clone();
		cloned.complement();
		return cloned;
	}

	/**
	 * Complements the set representation, from 0 to <code>maxSetBit</code>
	 * (bitwise not)
	 */
	public void complement() {
		// complement each word
		for (int i = 0; i < words.length; i++) {
			if (isLiteral(words[i])) {
				// negate the bits and set the most significant bit to 1
				words[i] = ALL_ZEROES_LITERAL | ~words[i];
			} else {
				// flip the bit before the most significant one to switch the
				// sequence type
				words[i] ^= 0x40000000;
			}
		}

		// do not complement after the last element (maxSetBit)
		if (isLiteral(words[lastWordIndex()])) {
			words[lastWordIndex()] &= ALL_ZEROES_LITERAL
					| (0xFFFFFFFF >>> (MAXIMUM_LITERAL_LENGHT - maxSetBitInLastWord));
			compact(lastWordIndex(), words);
		}

		// remove trailing zeroes
		if (isZeroSequence(words[lastWordIndex()]))
			resizeWords(-1);

		// update size and max element
		updateSizeAndMaxElement();
	}

	/**
	 * Iterator for set bits
	 */
	private class SetBitIterator implements Iterator<Integer> {
		private int currentWordIndex;
		private int maxSetBitInWordBeforeLast;
		private int nextBitToCheck;

		/**
		 * 
		 */
		public SetBitIterator() {
			if (isEmpty()) {
				// empty bit-set
				currentWordIndex = NOT_VALID_INDEX;
			} else {
				// prepare for the first bit
				currentWordIndex = 0;
				maxSetBitInWordBeforeLast = 0;
				nextBitToCheck = 0;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			// there are no words
			if (currentWordIndex == NOT_VALID_INDEX)
				return false;

			// there are other words
			if (currentWordIndex < lastWordIndex())
				return true;

			// check if we already reached the last bit of the last word
			if (isLiteral(words[currentWordIndex])) 
				// it is a literal
				return Integer.numberOfTrailingZeros(words[currentWordIndex]
						& (0xFFFFFFFF << nextBitToCheck)) < MAXIMUM_LITERAL_LENGHT;

			// it is a sequence of 1's
			return nextBitToCheck < MAXIMUM_LITERAL_LENGHT
					* sequenceCount(words[currentWordIndex]);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer next() {
			if (currentWordIndex == NOT_VALID_INDEX)
				throw new NoSuchElementException();

			// loop until we find some set bit
			int currentSetBitInLastWord;
			do {
				currentSetBitInLastWord = getNextBitInCurrentWordOrAdvanceWord();
			} while (currentSetBitInLastWord == NOT_VALID_INDEX
					&& currentWordIndex < words.length);

			if (currentWordIndex > lastWordIndex())
				throw new NoSuchElementException();

			return maxSetBitInWordBeforeLast + currentSetBitInLastWord;
		}

		/**
		 * Tries to get the relative position of the next bit in the current word.
		 * If there are no other set bits, returns <code>NOT_VALID_INDEX</code>
		 * and advance the <code>currentWord</code> and
		 * <code>maxSetBitInLastWord</code>.
		 * 
		 * @return
		 */
		private int getNextBitInCurrentWordOrAdvanceWord() {
			// The current word is a literal
			if (isLiteral(words[currentWordIndex])) {
				nextBitToCheck = Integer
						.numberOfTrailingZeros(words[currentWordIndex]
								& (0xFFFFFFFF << nextBitToCheck));
				if (nextBitToCheck < MAXIMUM_LITERAL_LENGHT) 
					// return the current bit and then set the next search
					// within the literal
					return nextBitToCheck++;
				
				// end of word
				maxSetBitInWordBeforeLast += MAXIMUM_LITERAL_LENGHT;
				currentWordIndex++;
				nextBitToCheck = 0;
				return NOT_VALID_INDEX;
			}

			// The current word is a sequence of 1's
			if (isOneSequence(words[currentWordIndex])) {
				int onesInSequence = MAXIMUM_LITERAL_LENGHT
						* sequenceCount(words[currentWordIndex]);
				if (nextBitToCheck < onesInSequence) 
					// return the current bit and set the next search within the
					// sequence
					return nextBitToCheck++;
				// end of word
				maxSetBitInWordBeforeLast += onesInSequence;
				currentWordIndex++;
				nextBitToCheck = 0;
				return NOT_VALID_INDEX;
			}

			// The current word is a sequence of 0's
			maxSetBitInWordBeforeLast += MAXIMUM_LITERAL_LENGHT
					* sequenceCount(words[currentWordIndex]);
			currentWordIndex++;
			nextBitToCheck = 0;
			return NOT_VALID_INDEX;
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
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Integer> iterator() {
		return new SetBitIterator();
	}

	/**
	 * Print debug info 
	 * 
	 * @return debug info
	 */
	public String debugInfo() {
		StringBuilder res = new StringBuilder();
		res.append(toString());
		res.append("\n");
		if (isEmpty())
			return "null\n";
		for (int i = 0; i < words.length; i++) {
			res.append(i);
			res.append(") ");
			if (isLiteral(words[i])) {
				res.append("literal: ");
				res.append(Integer.toBinaryString(words[i]).replaceFirst("1", "*"));
			} else if (isOneSequence(words[i])) {
				res.append("1 blocks: ");
				res.append(sequenceCount(words[i]));
				res.append(" (");
				res.append(MAXIMUM_LITERAL_LENGHT * sequenceCount(words[i]));
				res.append(" bits)");
			} else {
				res.append("0 blocks: ");
				res.append(sequenceCount(words[i]));
				res.append(" (");
				res.append(MAXIMUM_LITERAL_LENGHT * sequenceCount(words[i]));
				res.append(" bits)");
			}
			res.append("\n");
		}
		res.append("maxSetBitInLastWord: ");
		res.append(maxSetBitInLastWord);
		res.append("\n");
		res.append("maxSetBit: ");
		res.append(maxSetBit);
		res.append("\n");
		res.append("size: ");
		res.append(size);
		res.append("\n");

		DecimalFormat f = new DecimalFormat();
		f.setMaximumFractionDigits(2);
		res.append("size : ");
		res.append(f.format(100D * words.length / Math.ceil(maxSetBit / 32D)));
		res.append("%\n");
		return res.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		words = null;
		maxSetBitInLastWord = 0;
		maxSetBit = 0;
		size = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super Integer> comparator() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer first() {
		Iterator<Integer> itr = iterator();
		if (itr.hasNext())
			return itr.next();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SortedSet<Integer> headSet(Integer toElement) {
		// TODO
		throw new UnsupportedOperationException("To complete!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer last() {
		return maxSetBit == -1 ? null : maxSetBit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
		// TODO
		throw new UnsupportedOperationException("To complete!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SortedSet<Integer> tailSet(Integer fromElement) {
		// TODO
		throw new UnsupportedOperationException("To complete!");
	}

	/**
	 * Converts a given collection to a CompressedBitSet_old instance
	 * 
	 * @param c
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static WAHSet asWAH_BitSet(Collection<?> c) {
		if (c == null)
			throw new NullPointerException();

		// useless to convert...
		if (c instanceof WAHSet)
			return (WAHSet) c;

		// try to convert the collection
		WAHSet res = new WAHSet();
		if (!c.isEmpty()) {
			// sorted element by ascending integers (in order to use the
			// append() method)
			SortedSet<Integer> elements;
			if (c instanceof SortedSet) {
				// if elements are already ordered according to the natural
				// order of Integer,
				// simply use them
				elements = (SortedSet<Integer>) c;
				if (elements.comparator() != null) {
					// not natural ordering...
					elements = new TreeSet<Integer>((Collection<Integer>) c);
				}
			} else {
				// sort elements in ascending order
				elements = new TreeSet<Integer>((Collection<Integer>) c);
			}

			// append elements
			for (Integer i : elements)
				res.append(i);
		}
		return res;
	}

	/**
	 * Converts a given element to a CompressedBitSet_old instance
	 * 
	 * @param e
	 * @return
	 */
	private WAHSet asWAH_BitSet(Object e) {
		WAHSet res = new WAHSet();
		res.append((Integer) e);
		return res;
	}

	/**
	 * Replaces the current instance with another instance
	 * 
	 * @param other
	 */
	private void becomeAliasOf(WAHSet other) {
		if (this == other)
			return;
		this.words = other.words;
		this.size = other.size;
		this.maxSetBitInLastWord = other.maxSetBitInLastWord;
		this.maxSetBit = other.maxSetBit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(Integer e) {
		// the element can be simply appended
		if (isEmpty() || e > maxSetBit) {
			append(e);
			return true;
		}

		// compute the "or" with the given element
		int sizeBefore = size;
		becomeAliasOf(getUnion(asWAH_BitSet(e)));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		int sizeBefore = size;
		becomeAliasOf(getUnion(asWAH_BitSet(c)));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		return intersectionSize(asWAH_BitSet(o)) == 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null || c.isEmpty())
			return true;
		if (c.size() > size)
			return false;
		return intersectionSize(asWAH_BitSet(c)) == c.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		int sizeBefore = size;
		becomeAliasOf(getDifference(asWAH_BitSet(o)));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		int sizeBefore = size;
		becomeAliasOf(getDifference(asWAH_BitSet(c)));
		return size != sizeBefore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		int sizeBefore = size;
		becomeAliasOf(getIntersection(asWAH_BitSet(c)));
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
	public int hashCode() {
		return Arrays.hashCode(words);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final WAHSet other = (WAHSet) obj;
		if (size != other.size)
			return false;
		if (maxSetBit != other.maxSetBit)
			return false;
		if (maxSetBitInLastWord != other.maxSetBitInLastWord)
			return false;
		if (!Arrays.equals(words, other.words))
			return false;
		return true;
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
/*	public static void main(String[] args) {
		WAHSet bitSet = new WAHSet();
		TreeSet<Integer> elements = new TreeSet<Integer>();

		if (false) {
			System.out.println(bitSet);
			System.out.println(bitSet.debugInfo());

			// elements to put
			elements.add(1000);
			elements.add(1001);
			elements.add(1023);
			elements.add(1024);
			elements.add(1025);
			elements.add(2000);
			elements.add(2046);
			for (int i = 0; i < 62; i++) {
				elements.add(2047 + i);
			}
			elements.add(2158);

			// put elements
			for (Integer i : elements) {
				System.out.println("Adding " + i);
				bitSet.append(i);
				System.out.println(bitSet.debugInfo());
			}
		}

		if (false) {
			Random rnd = new Random();

			bitSet.clear();
			elements.clear();
			for (int i = 0; i < 10000; i++)
				elements.add(rnd.nextInt(1000000000 + 1));
			for (Integer i : elements)
				bitSet.append(i);
			System.out.println("Check: "
					+ ((bitSet.size() == elements.size()) && bitSet
							.containsAll(elements)));
			System.out.println(bitSet.debugInfo());

			bitSet.clear();
			elements.clear();
			for (int i = 0; i < 10000; i++)
				elements.add(rnd.nextInt(10000 + 1));
			for (Integer i : elements)
				bitSet.append(i);
			System.out.println("Check: "
					+ ((bitSet.size() == elements.size()) && bitSet
							.containsAll(elements)));
			System.out.println(bitSet.debugInfo());

			bitSet.clear();
			elements.clear();
			for (int i = 0; i < 5000; i++)
				elements.add(rnd.nextInt(50 + 1));
			for (int i = 0; i < 5000; i++)
				elements.add(rnd.nextInt(100000 + 1));
			for (Integer i : elements)
				bitSet.append(i);
			System.out.println("Check: "
					+ ((bitSet.size() == elements.size()) && bitSet
							.containsAll(elements)));
			System.out.println(bitSet.debugInfo());
		}

		if (false) {
			WAHSet bitSet1 = new WAHSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			elements1.add(1);
			elements1.add(2);
			elements1.add(3);
			elements1.add(100);
			elements1.add(1000);
			for (Integer i : elements1)
				bitSet1.append(i);
			System.out.println("Check: "
					+ ((bitSet1.size() == elements1.size()) && bitSet1
							.containsAll(elements1)));
			System.out.println(bitSet1.debugInfo());

			WAHSet bitSet2 = new WAHSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			elements2.add(100);
			elements2.add(101);
			for (Integer i : elements2)
				bitSet2.append(i);
			System.out.println("Check: "
					+ ((bitSet2.size() == elements2.size()) && bitSet2
							.containsAll(elements2)));
			System.out.println(bitSet2.debugInfo());

			WAHSet bitSet3 = bitSet1.getIntersection(bitSet2);
			TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
			elements3.retainAll(elements2);
			System.out.println("Check: "
					+ ((bitSet3.size() == elements3.size()) && bitSet3
							.containsAll(elements3)));
			System.out.println(bitSet3.debugInfo());
		}

		if (true) {
			Random rnd = new Random();

			WAHSet bitSet1 = new WAHSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			for (int i = 0; i < 30; i++)
				elements1.add(rnd.nextInt(1000 + 1));
			for (int i = 0; i < 1000; i++)
				elements1.add(rnd.nextInt(200 + 1));
			bitSet1.addAll(elements1);
			System.out.println("Check: "
					+ (bitSet1.containsAll(elements1) && elements1
							.containsAll(bitSet1)));
			System.out.println(elements1);
			System.out.println(bitSet1.debugInfo());

			WAHSet bitSet2 = new WAHSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			for (int i = 0; i < 30; i++)
				elements2.add(rnd.nextInt(1000 + 1));
			for (int i = 0; i < 1000; i++)
				elements2.add(150 + rnd.nextInt(300 - 150 + 1));
			bitSet2.addAll(elements2);
			System.out.println("Check: "
					+ (bitSet2.containsAll(elements2) && elements2
							.containsAll(bitSet2)));
			System.out.println(elements2);
			System.out.println(bitSet2.debugInfo());

			if (true) {
				WAHSet bitSet3 = bitSet1.getIntersection(bitSet2);
				TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
				System.out.println("Check: "
						+ (bitSet3.containsAll(elements3) && elements3
								.containsAll(bitSet3)));
				System.out.println("Intersection size: " + bitSet3.size());
				System.out.println("Intersection size: "
						+ bitSet1.intersectionSize(bitSet2));
				System.out.println(elements3);
				System.out.println(bitSet3.debugInfo());

				elements3.retainAll(elements2);
				System.out.println("Check: "
						+ (bitSet3.containsAll(elements3) && elements3
								.containsAll(bitSet3)));
				System.out.println(elements3);
				System.out.println(bitSet3.debugInfo());
			}
		}

		if (false) {
			WAHSet bitSet1 = new WAHSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			elements1.add(1);
			elements1.add(2);
			elements1.add(30000);
			for (Integer i : elements1)
				bitSet1.append(i);
			System.out.println("Check: "
					+ ((bitSet1.size() == elements1.size()) && bitSet1
							.containsAll(elements1)));
			System.out.println(bitSet1.debugInfo());

			WAHSet bitSet2 = new WAHSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			elements2.add(100);
			elements2.add(101);
			for (int i = 0; i < 62; i++)
				elements2.add(341 + i);
			for (Integer i : elements2)
				bitSet2.append(i);
			System.out.println("Check: "
					+ ((bitSet2.size() == elements2.size()) && bitSet2
							.containsAll(elements2)));
			System.out.println(bitSet2.debugInfo());

			WAHSet bitSet3 = bitSet1.getUnion(bitSet2);
			TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
			elements3.addAll(elements2);
			System.out.println("Check: "
					+ ((bitSet3.size() == elements3.size()) && bitSet3
							.containsAll(elements3)));
			System.out.println(bitSet3.debugInfo());
		}

		if (false) {
			WAHSet bitSet1 = new WAHSet();
			bitSet1.append(1);
			bitSet1.append(2);
			bitSet1.append(30000);
			System.out.println(bitSet1.debugInfo());

			bitSet1 = bitSet1.getComplement();
			System.out.println(bitSet1.debugInfo());
		}

		if (false) {
			boolean debugInfo = true;

			WAHSet bitSet1 = new WAHSet();
			bitSet1.add(1);
			bitSet1.add(100);
			bitSet1.add(2);
			bitSet1.add(3);
			bitSet1.add(2);
			bitSet1.add(100);
			System.out.println("A: " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1.debugInfo());

			WAHSet bitSet2 = new WAHSet();
			bitSet2.add(1);
			bitSet2.add(1000000);
			bitSet2.add(2);
			bitSet2.add(30000);
			bitSet2.add(1000000);
			System.out.println("B: " + bitSet2);
			if (debugInfo)
				System.out.println(bitSet2.debugInfo());

			System.out.println("A.getSymmetricDifference(B): "
					+ bitSet1.getSymmetricDifference(bitSet2));
			if (debugInfo)
				System.out.println(bitSet1.getSymmetricDifference(bitSet2).debugInfo());

			System.out.println("A.getComplement(): " + bitSet1.getComplement());
			if (debugInfo)
				System.out.println(bitSet1.getComplement().debugInfo());

			bitSet1.removeAll(bitSet2);
			System.out.println("A.removeAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1.debugInfo());

			bitSet1.addAll(bitSet2);
			System.out.println("A.addAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1.debugInfo());

			bitSet1.retainAll(bitSet2);
			System.out.println("A.retainAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1.debugInfo());

			bitSet1.remove(1);
			System.out.println("B.remove(3): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1.debugInfo());
		}
	}*/
}

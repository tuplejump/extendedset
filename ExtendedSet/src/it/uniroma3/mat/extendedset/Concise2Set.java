package it.uniroma3.mat.extendedset;

import it.uniroma3.mat.extendedset.test.RandomNumbers;
import it.uniroma3.mat.extendedset.utilities.MersenneTwister;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedSet;

/**
 * @author colantonio
 *
 */
public class Concise2Set extends IntSet implements java.io.Serializable {
	/** generated serial ID */
	private static final long serialVersionUID = -6035357097384971493L;

	/**
	 * Compressed bitmap
	 */
	private int[] words;

	/**
	 * Cached cardinality of the bit-set. Defined for efficient {@link #size()}
	 * calls. When -1, the cache is invalid.
	 */ 
	private transient int size;

	/**
	 * Index of the last word in {@link #words}
	 */ 
	private transient int lastBlockIndex;

	/**
	 * User for <i>fail-fast</i> iterator. It counts the number of operations
	 * that <i>do</i> modify {@link #words}
	 */
	protected transient volatile int modCount = 0;

	
	/**
	 * Resets to an empty set
	 * 
	 * @see #ConciseSet()
	 * {@link #clear()}
	 */
	private void reset() {
		modCount++;
		words = null;
		size = 0;
		lastBlockIndex = -2;
	}

	/**
	 * Assures that the length of {@link #words} is sufficient to contain
	 * the given index.
	 */
	private void ensureCapacity(int blockIndex) {
		int capacity = words == null ? 0 : words.length;
		if (capacity > blockIndex + 2) 
			return;
		capacity = Math.max(capacity << 1, blockIndex + 2);

		if (words == null) {
			// nothing to copy
			words = new int[capacity];
			return;
		}
		words = Arrays.copyOf(words, capacity);
	}

	/**
	 * Removes unused allocated words at the end of {@link #words} only when they
	 * are more than twice of the needed space
	 */
	private void compact() {
		if (lastBlockIndex == -2) {
			reset();
			return;
		}
		if (words != null && ((lastBlockIndex + 2) << 1) < words.length)
			words = Arrays.copyOf(words, lastBlockIndex + 2);
	}

	/**
	 * Replace this set with the content of another set
	 * 
	 * @param other
	 *            the other set
	 */
	private void replace(Concise2Set other) {
		modCount++;
		words = other.words;
		lastBlockIndex = other.lastBlockIndex;
		size = other.size;
	}

	/**
	 * Identifies the index of the block where the key should be put
	 * 
	 * @param key
	 * @return
	 */
	private int binarySearch(int key) {
		return binarySearch(0, lastBlockIndex, key);
	}
	
	/**
	 * Extract the block containing the element
	 * 
	 * @param element
	 * @return
	 */
	private static int extractBlock(int element) {
		return element & 0xFFFFFFE0;
	}
	
	/**
	 * Identifies the index of the block where the key should be put
	 * 
	 * @param low
	 * @param high
	 * @param key
	 * @return
	 */
	private int binarySearch(int low, int high, int key) {
		key = extractBlock(key);
		while (low <= high) {
			// mid block, rounded to a multiple of two (clear the LSB)
			int mid = ((low + high) >>> 1) & 0xFFFFFFFE;
			int midVal = words[mid];

			// key found
			if (midVal == key)				
				return mid; 

			if (midVal < key)
				low = mid + 2;
			else 
				high = mid - 2;
		}
		
		// key not found
		return -(low + 1); 
	}

	/**
	 * Empty set
	 */
	public Concise2Set() {
		reset();
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
	public Concise2Set clone() {
		if (isEmpty())
			return empty();

		// NOTE: do not use super.clone() since it is 10 times slower!
		Concise2Set res = empty();
		res.lastBlockIndex = lastBlockIndex;
		res.modCount = 0;
		res.size = size;
		res.words = Arrays.copyOf(words, lastBlockIndex + 2);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set empty() {
		return new Concise2Set();
	}

	/**
	 * Convert a given collection to a {@link ConciseSet} instance
	 */
	private Concise2Set convert(IntSet c) {
		if (c instanceof Concise2Set)
			return (Concise2Set) c;
		if (c == null)
			return empty();

		Concise2Set res = empty();
		ExtendedIntIterator itr = c.intIterator();
		while (itr.hasNext()) 
			res.add(itr.next());
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set convert(int... a) {
		Concise2Set res = empty();
		if (a != null) {
			a = Arrays.copyOf(a, a.length);
			Arrays.sort(a);
			for (int i : a)
				res.add(i);
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set convert(Collection<Integer> c) {
		Concise2Set res = empty();
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
	public boolean add(int i) {
		modCount++;

		// identify which block the number should be put in
		int blockIndex = binarySearch(i);
		
		// non-existing block
		if (blockIndex < 0) {
			// block where the number should be put in
			blockIndex = -blockIndex - 1;
			
			// add the space for one more block
			ensureCapacity(lastBlockIndex + 2);
			
			// shift all the blocks
			if (blockIndex <= lastBlockIndex) 
				System.arraycopy(words, blockIndex, words, blockIndex + 2, lastBlockIndex - blockIndex + 2);
			lastBlockIndex += 2;
			words[blockIndex] = extractBlock(i);
			words[blockIndex + 1] = 1 << i;
			if (size >= 0)
				size++;
			return true;
		}

		// existing block
		int before = words[blockIndex + 1];
		words[blockIndex + 1] |= 1 << i;
		if (before != words[blockIndex + 1]) {
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
		modCount++;

		// identify which block contains the number
		int blockIndex = binarySearch(i);
		if (blockIndex < 0)
			return false;
		
		// remove the element
		int before = words[blockIndex + 1];
		words[blockIndex + 1] &= ~(1 << i);
		if (words[blockIndex + 1] == 0) {
			if (lastBlockIndex > 0) {
				if (blockIndex < lastBlockIndex)
					System.arraycopy(words, blockIndex + 2, words, blockIndex, lastBlockIndex - blockIndex);
			} else {
				words = null;
			}
			lastBlockIndex -= 2;
			if (size >= 0)
				size--;
			return true;
		}
		if (before != words[blockIndex + 1]) {
			if (size >= 0)
				size--;
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(int e) {
		modCount++;

		// identify which block contains the number
		int blockIndex = binarySearch(e);
		
		// add the element
		if (blockIndex < 0) {
			// block where the number should be put in
			blockIndex = -blockIndex - 1;
			
			// add the space for one more block
			ensureCapacity(lastBlockIndex + 2);
			
			// shift all the blocks
			if (blockIndex <= lastBlockIndex) 
				System.arraycopy(words, blockIndex, words, blockIndex + 2, lastBlockIndex - blockIndex + 2);
			lastBlockIndex += 2;
			words[blockIndex] = extractBlock(e);
			words[blockIndex + 1] = 1 << e;
			if (size >= 0)
				size++;
			return;
		}
		
		// flip the element
		int mask = 1 << e;
		words[blockIndex + 1] ^= mask;
		
		// remove the block
		if (words[blockIndex + 1] == 0) {
			if (lastBlockIndex > 0) {
				if (blockIndex < lastBlockIndex)
					System.arraycopy(words, blockIndex + 2, words, blockIndex, lastBlockIndex - blockIndex);
			} else {
				words = null;
			}
			lastBlockIndex -= 2;
			if (size >= 0)
				size--;
			return;
		}
		
		// update the size
		if (size >= 0) {
			if ((words[blockIndex + 1] & mask) == 0) 
				size--;
			else
				size++;
		}
	}


	private interface IntListIterator {
		boolean hasNext();
		boolean hasPrevious();
		void skipAllBefore(int i);
		void skipAllAfter(int i);
		int next();
		int previous();
	}
	
	/**
	 * Class for iterating over the bits of a word
	 */
	private class IntList {
		final int listSize;
		final int[] intBuffer;

		/**
		 * 
		 * @param index
		 *            index of {@link #words}
		 * @param intBuffer
		 *            buffer created as <code>new int[32]</code> (once for each
		 *            new iterator) (or <code>new int[x]</code> where
		 *            <code>x</code> is the maximum number of set bits)
		 */
		IntList(int index, int[] intBuffer) {
			assert index >= 0;
			assert index <= lastBlockIndex;
			assert index % 2 == 0;
			assert words[index + 1] != 0;
			assert intBuffer.length >= Integer.bitCount(words[index + 1]);
			
			this.intBuffer = intBuffer;
			int len = 0;
			int offset = words[index];
			int bits = words[index + 1];
			for (int i = 0; i < 32; i++) 
				if ((bits & (1 << i)) != 0)
					intBuffer[len++] = offset + i;
			listSize = len;
		}

		private IntListIterator iterator() {return iterator(0);}
		private IntListIterator iteratorFromLast() {return iterator(listSize);}

		private IntListIterator iterator(final int first) {
			return new IntListIterator() {
				int current = first;
				@Override public boolean hasNext() {return current < listSize;}
				@Override public int next() {assert hasNext(); return intBuffer[current++];}
				@Override public boolean hasPrevious() {return current > 0;}
				@Override public int previous() {assert hasPrevious(); return intBuffer[--current];}
				@Override public void skipAllAfter(int i) {
					while (hasPrevious() && intBuffer[current - 1] > i) 
						current--;
				}
				@Override public void skipAllBefore(int i) {
					while (hasNext() && intBuffer[current] < i) 
						current++;
				}
			};
		}

		@Override public String toString() {
	        IntListIterator itr = iterator();
	    	if (!itr.hasNext())
	    	    return "[]";

			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (;;) {
				int e = itr.next();
				sb.append(e);
				if (!itr.hasNext())
					return sb.append(']').toString();
				sb.append(", ");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIntIterator intIterator() {
		if (isEmpty()) {
			return new ExtendedIntIterator() {
				@Override public void skipAllBefore(int element) {/*empty*/}
				@Override public boolean hasNext() {return false;}
				@Override public int next() {throw new NoSuchElementException();}
				@Override public void remove() {throw new UnsupportedOperationException();}
			};
		}

		return new ExtendedIntIterator() {
			final int initialModCount = modCount;
			int blockIndex = 0;
			int[] intBuffer = new int[32];
			IntListIterator bitItr = new IntList(0, intBuffer).iterator();
			
			@Override
			public void skipAllBefore(int element) {
				if (initialModCount != modCount)
					throw new ConcurrentModificationException();

				int block = extractBlock(element);
				int b = words[blockIndex];
				
				// element already visited
				if (b > block)
					return;
				
				// skip to the next block to visit
				if (b < block) {
					blockIndex = binarySearch(blockIndex, lastBlockIndex, element);
					if (blockIndex < 0) 
						blockIndex = -blockIndex - 1;
					if (blockIndex <= lastBlockIndex)
						bitItr = new IntList(blockIndex, intBuffer).iterator();
				}
				
				// skip elements in the current block
				bitItr.skipAllBefore(element);
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public int next() {
				if (initialModCount != modCount)
					throw new ConcurrentModificationException();
				if (!bitItr.hasNext()) {
					blockIndex += 2;
					if (blockIndex > lastBlockIndex)
						throw new NoSuchElementException();
					bitItr = new IntList(blockIndex, intBuffer).iterator();
				}
				return bitItr.next();
			}
			
			@Override
			public boolean hasNext() {
				return blockIndex < lastBlockIndex || bitItr.hasNext();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIntIterator descendingIntIterator() {
		if (isEmpty()) {
			return new ExtendedIntIterator() {
				@Override public void skipAllBefore(int element) {/*empty*/}
				@Override public boolean hasNext() {return false;}
				@Override public int next() {throw new NoSuchElementException();}
				@Override public void remove() {throw new UnsupportedOperationException();}
			};
		}

		return new ExtendedIntIterator() {
			final int initialModCount = modCount;
			int blockIndex = lastBlockIndex;
			int[] intBuffer = new int[32];
			IntListIterator bitItr = new IntList(lastBlockIndex, intBuffer).iteratorFromLast();
			
			@Override
			public void skipAllBefore(int element) {
				if (initialModCount != modCount)
					throw new ConcurrentModificationException();

				int block = extractBlock(element);
				int b = words[blockIndex];
				
				// element already visited
				if (b < block)
					return;
				
				// skip to the next block to visit
				if (b > block) {
					blockIndex = binarySearch(0, blockIndex, element);
					if (blockIndex < 0) 
						blockIndex = -blockIndex - 3;
					if (blockIndex >= 0)
						bitItr = new IntList(blockIndex, intBuffer).iteratorFromLast();
				}
				
				// skip elements in the current block
				bitItr.skipAllAfter(element);
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public int next() {
				if (initialModCount != modCount)
					throw new ConcurrentModificationException();
				if (!bitItr.hasPrevious()) {
					blockIndex -= 2;
					if (blockIndex < 0)
						throw new NoSuchElementException();
					bitItr = new IntList(blockIndex, intBuffer).iteratorFromLast();
				}
				return bitItr.previous();
			}
			
			@Override
			public boolean hasNext() {
				return blockIndex > 0 || bitItr.hasPrevious();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		assert size >= -1;
		assert (lastBlockIndex >= 0 && words != null) || (lastBlockIndex == -2 && words == null);
		
		if (size < 0) {
			size = 0;
			for (int i = 0; i <= lastBlockIndex; i += 2) 
				size += Integer.bitCount(words[i + 1]);
			assert (size > 0 && words != null) || (size == 0 && words == null);
		}
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        int h = 1;
        for (int i = 0; i <= lastBlockIndex + 1; i++) 
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
		if (!(obj instanceof Concise2Set))
			return false;
		
		final Concise2Set other = (Concise2Set) obj;
		if (size() != other.size())
			return false;
		if (isEmpty())
			return true;
		
        for (int i = 0; i <= lastBlockIndex + 1; i++)
            if (words[i] != other.words[i])
                return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(int i) {
		int block = binarySearch(i);
		return (block >= 0) && ((words[block + 1] & (1 << i)) != 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(IntSet other) {
		Concise2Set res = union(other);
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
		Concise2Set res = difference(other);
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
		Concise2Set res = intersection(other);
		if (equals(res))
			return false;
		replace(res);
		return true;
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
	public Concise2Set complemented() {
		Concise2Set res = empty();
		if (!isEmpty()) {
			// allocate the maximum space that can be used
			res.size = complementSize();
			res.words = new int[(res.size + 1) << 1];
			
			// iterate over blocks
			int lastBlock = 0;
			for (int i = 0; i <= lastBlockIndex; i += 2) {
				// fill in the "gaps"
				while (lastBlock < words[i]) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = lastBlock;
					res.words[b + 1] = 0xFFFFFFFF;
					lastBlock += 32;
				}
				
				// complement current block
				int b = res.lastBlockIndex += 2;
				res.words[b] = lastBlock;
				if (i < lastBlockIndex) {
					res.words[b + 1] = ~words[i + 1];
				} else {
					// last block
					int zeros = 0xFFFFFFFF >>> Integer.numberOfLeadingZeros(words[i + 1]);
					res.words[b + 1] = ~words[i + 1] & zeros;
				}
				lastBlock += 32;
				
				// remove empty block
				if (res.words[b + 1] == 0)
					res.lastBlockIndex -= 2;
			}
			
			// remove unused space
			res.compact();
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(IntSet o) {
		if (o == null || o.isEmpty() || o == this)
			return true;
		if (isEmpty())
			return false;

		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		int thisBlock;
		int otherBlock;
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				if (thisBlock > otherBlock) 
					return false;
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					thisIndex += 2;
					if (thisIndex > lastBlockIndex)
						return false;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the intersection
			int r = words[thisIndex + 1] & other.words[otherIndex + 1];
			if (r != other.words[otherIndex + 1]) 
				return false;
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		return otherIndex > other.lastBlockIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(IntSet o) {
		if (o == null || o.isEmpty() || o == this)
			return true;
		if (isEmpty())
			return false;

		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		int res = 0;
		int thisBlock;
		int otherBlock;
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex)
						return false;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					thisIndex += 2;
					if (thisIndex > lastBlockIndex)
						return false;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the intersection
			res += Integer.bitCount(words[thisIndex + 1] & other.words[otherIndex + 1]);
			if (res != 0)
				return true;
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(IntSet o, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		if ((size >= 0 && size < minElements) || o == null || o.isEmpty() || isEmpty())
			return false;
		if (this == o)
			return size() >= minElements;

		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		int res = 0;
		int thisBlock;
		int otherBlock;
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex)
						return false;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					thisIndex += 2;
					if (thisIndex > lastBlockIndex)
						return false;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the intersection
			res += Integer.bitCount(words[thisIndex + 1] & other.words[otherIndex + 1]);
			if (res >= minElements)
				return true;
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(int from, int to) {
		modCount++;

		if (from > to)
			throw new IndexOutOfBoundsException("from: " + from + " > to: " + to);
		if (from == to) {
			remove(from);
			return;
		}

		// identify the range to clear
		int startBlockIndex = binarySearch(from);
		boolean startMissing = startBlockIndex < 0;
		if (startMissing) 
			startBlockIndex = -startBlockIndex - 1;

		int endBlockIndex = binarySearch(startBlockIndex, lastBlockIndex, to);
		boolean endMissing = endBlockIndex < 0;
		if (endMissing) 
			endBlockIndex = -endBlockIndex - 1;

		// nothing to clean
		if (startMissing && endMissing && startBlockIndex == endBlockIndex)
			return;

		// clear bits of the first and last words
		boolean modified = false;
		int firstWordMask = 0xFFFFFFFF << from;
		int lastWordMask = 0xFFFFFFFF >>> -(to + 1);
		if (startBlockIndex == endBlockIndex && !startMissing) {
			// Case 1: One word
			assert !endMissing;
			int before = words[startBlockIndex + 1];
			words[startBlockIndex + 1] &= ~(firstWordMask & lastWordMask);
			modified = words[startBlockIndex + 1] != before;
		} else {
			// Case 2: Two words
			if (!startMissing) {
				int before = words[startBlockIndex + 1];
				words[startBlockIndex + 1] &= ~firstWordMask;
				modified = words[startBlockIndex + 1] != before;
			}
			if (!endMissing) {
				int before = words[endBlockIndex + 1];
				words[endBlockIndex + 1] &= ~lastWordMask;
				modified = modified || words[endBlockIndex + 1] != before;
			}
		}
		
		// clear all the blocks between the first and the last one
		// startBlockIndex = first block to overwrite
		// endMissing = first block to keep
		if (!startMissing && words[startBlockIndex + 1] != 0)
			startBlockIndex += 2; // the starting block should not be removed
		if (!endMissing && words[endBlockIndex + 1] == 0)
			endBlockIndex += 2;
		int delta = endBlockIndex - startBlockIndex;
		if (delta >= 0) {
			// useless to move data, just prune the tail
			if (endBlockIndex > lastBlockIndex) {
				lastBlockIndex = startBlockIndex - 2;
				if (lastBlockIndex < 0) {
					reset();
					return;
				}
			} else {
				System.arraycopy(words, endBlockIndex, words, startBlockIndex, lastBlockIndex - endBlockIndex + 2);
				lastBlockIndex -= delta;
			}
			modified = true;
		}
			
		if (modified) 
			size = -1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(int from, int to) {
		modCount++;

		if (from > to)
			throw new IndexOutOfBoundsException("from: " + from + " > to: " + to);
		if (from == to) {
			add(from);
			return;
		}

		// Increase capacity, if necessary
		int startBlockIndex = binarySearch(from);
		boolean startMissing = startBlockIndex < 0;
		if (startMissing) 
			startBlockIndex = -startBlockIndex - 1;

		int endBlockIndex = binarySearch(startBlockIndex, lastBlockIndex, to);
		boolean endMissing = endBlockIndex < 0;
		if (endMissing) 
			endBlockIndex = -endBlockIndex - 1;

		int deltaIndex = 0;
		if (endMissing || (startMissing && (startBlockIndex == endBlockIndex + 2)))
			deltaIndex = 2;
		
		int blockIndexGap = ((to >>> 5) - (from >>> 5)) << 1; 
		deltaIndex += blockIndexGap - (endBlockIndex - startBlockIndex);
		if (deltaIndex > 0) {
			ensureCapacity(lastBlockIndex + deltaIndex);
			System.arraycopy(words, endBlockIndex, words, endBlockIndex + deltaIndex, lastBlockIndex - endBlockIndex + 2);
			lastBlockIndex += deltaIndex;
			endBlockIndex = startBlockIndex + blockIndexGap;

			if (startMissing) 
				words[startBlockIndex + 1] = 0;
			if (endMissing && (startBlockIndex != endBlockIndex)) 
				words[endBlockIndex + 1] = 0;
			for (int i = startBlockIndex, b = extractBlock(from); i <= endBlockIndex; i += 2, b += 32)
				words[i] = b;
		}
		
		// set bits
		boolean modified = false;
		int firstWordMask = 0xFFFFFFFF << from;
		int lastWordMask = 0xFFFFFFFF >>> -(to + 1);
		if (startBlockIndex == endBlockIndex) {
			// Case 1: One word
			int before = words[startBlockIndex + 1];
			words[startBlockIndex + 1] |= (firstWordMask & lastWordMask);
			modified = words[startBlockIndex + 1] != before;
		} else {
			// Case 2: Multiple words
			// Handle first word
			int before = words[startBlockIndex + 1];
			words[startBlockIndex + 1] |= firstWordMask;
			modified = words[startBlockIndex + 1] != before;

			// Handle intermediate words, if any
			for (int i = startBlockIndex + 2; i < endBlockIndex; i += 2) {
				modified = modified || words[i + 1] != 0xFFFFFFFF;
				words[i + 1] = 0xFFFFFFFF;
			}

			// Handle last word
			before = words[endBlockIndex + 1];
			words[endBlockIndex + 1] |= lastWordMask;
			modified = modified || words[endBlockIndex + 1] != before;
		}
		if (modified)
			size = -1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int get(int i) {
		int prev = 0;
		for (int j = 0; j <= lastBlockIndex; j += 2) {
			int curr = Integer.bitCount(words[j + 1]);
			if (i < prev + curr) {
				IntListIterator itr = new IntList(j, new int[curr]).iterator();
				for (int k = i - prev; k > 0; k--) 
					itr.next();
				return itr.next();
			}
			prev += curr;	
		}
		throw new NoSuchElementException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(int e) {
		int prev = 0;
		for (int j = 0; j <= lastBlockIndex; j += 2) {
			int w = words[j];
			if (e < w)
				throw new NoSuchElementException("element not existing (non-existing block): " + e);
			if (e > w + 31) {
				prev += Integer.bitCount(words[j + 1]);
				continue;
			}

			// block found!
			IntListIterator itr = new IntList(j, new int[32]).iterator();
			int curr = 0;
			while (itr.hasNext()) {
				if (e == itr.next())
					return prev + curr;
				curr++;
			}
			throw new NoSuchElementException("element not existing (existing block): " + e);
		}
		throw new NoSuchElementException("element greater than all elements: " + e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set intersection(IntSet o) {
		if (isEmpty() || o == null || o.isEmpty())
			return empty();
		if (this == o)
			return clone();
		
		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		Concise2Set res = empty();
		res.words = new int[Math.max(lastBlockIndex, other.lastBlockIndex) + 2];
		res.size = -1;
		int thisBlock;
		int otherBlock;
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex) {
						res.compact();
						return res;
					}
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					thisIndex += 2;
					if (thisIndex > lastBlockIndex){
						res.compact();
						return res;
					}
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the intersection
			int r = words[thisIndex + 1] & other.words[otherIndex + 1];
			if (r != 0) {
				int b = res.lastBlockIndex += 2;
				res.words[b] = thisBlock;
				res.words[b + 1] = r;
			}
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		res.compact();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set difference(IntSet o) {
		if (isEmpty() || this == o)
			return empty();
		if (o == null || o.isEmpty()) 
			return clone();
		
		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		Concise2Set res = empty();
		res.words = new int[lastBlockIndex + 2];
		res.size = -1;
		int thisBlock;
		int otherBlock;
		mainLoop:
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex) 
						break mainLoop;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = thisBlock;
					res.words[b + 1] = words[thisIndex + 1];
					thisIndex += 2;
					if (thisIndex > lastBlockIndex) 
						break mainLoop;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the difference
			int r = words[thisIndex + 1] & ~other.words[otherIndex + 1];
			if (r != 0) {
				int b = res.lastBlockIndex += 2;
				res.words[b] = thisBlock;
				res.words[b + 1] = r;
			}
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		while(thisIndex <= lastBlockIndex) {
			int b = res.lastBlockIndex += 2;
			res.words[b] = words[thisIndex];
			res.words[b + 1] = words[thisIndex + 1];
			thisIndex += 2;
		}
		res.compact();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set union(IntSet o) {
		if (this == o || o == null || o.isEmpty())
			return clone();
		if (isEmpty()) {
			Concise2Set res = convert(o);
			if (res == o)
				return res.clone();
			return res;
		}
		
		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		Concise2Set res = empty();
		res.words = new int[lastBlockIndex + other.lastBlockIndex + 4];
		res.size = -1;
		int thisBlock;
		int otherBlock;
		mainLoop:
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = other.words[otherIndex];
					res.words[b + 1] = other.words[otherIndex + 1];
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex) 
						break mainLoop;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = words[thisIndex];
					res.words[b + 1] = words[thisIndex + 1];
					thisIndex += 2;
					if (thisIndex > lastBlockIndex) 
						break mainLoop;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the union
			int r = words[thisIndex + 1] | other.words[otherIndex + 1];
			int b = res.lastBlockIndex += 2;
			res.words[b] = thisBlock;
			res.words[b + 1] = r;
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		while(thisIndex <= lastBlockIndex) {
			int b = res.lastBlockIndex += 2;
			res.words[b] = words[thisIndex];
			res.words[b + 1] = words[thisIndex + 1];
			thisIndex += 2;
		}
		while(otherIndex <= other.lastBlockIndex) {
			int b = res.lastBlockIndex += 2;
			res.words[b] = other.words[otherIndex];
			res.words[b + 1] = other.words[otherIndex + 1];
			otherIndex += 2;
		}
		res.compact();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concise2Set symmetricDifference(IntSet o) {
		if (this == o || o == null || o.isEmpty())
			return clone();
		if (isEmpty()) {
			Concise2Set res = convert(o);
			if (res == o)
				return res.clone();
			return res;
		}
		
		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		Concise2Set res = empty();
		res.words = new int[lastBlockIndex + other.lastBlockIndex + 4];
		res.size = -1;
		int thisBlock;
		int otherBlock;
		mainLoop:
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = other.words[otherIndex];
					res.words[b + 1] = other.words[otherIndex + 1];
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex) 
						break mainLoop;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					int b = res.lastBlockIndex += 2;
					res.words[b] = words[thisIndex];
					res.words[b + 1] = words[thisIndex + 1];
					thisIndex += 2;
					if (thisIndex > lastBlockIndex) 
						break mainLoop;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the symmetric difference
			int r = words[thisIndex + 1] ^ other.words[otherIndex + 1];
			if (r != 0) {
				int b = res.lastBlockIndex += 2;
				res.words[b] = thisBlock;
				res.words[b + 1] = r;
			}
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		while(thisIndex <= lastBlockIndex) {
			int b = res.lastBlockIndex += 2;
			res.words[b] = words[thisIndex];
			res.words[b + 1] = words[thisIndex + 1];
			thisIndex += 2;
		}
		while(otherIndex <= other.lastBlockIndex) {
			int b = res.lastBlockIndex += 2;
			res.words[b] = other.words[otherIndex];
			res.words[b + 1] = other.words[otherIndex + 1];
			otherIndex += 2;
		}
		res.compact();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(IntSet o) {
		if (isEmpty() || o == null || o.isEmpty())
			return 0;
		if (this == o)
			return size();
		
		final Concise2Set other = convert(o);
		
		int thisIndex = 0;
		int otherIndex = 0;
		int res = 0;
		int thisBlock;
		int otherBlock;
		while (thisIndex <= lastBlockIndex && otherIndex <= other.lastBlockIndex) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			// find two similar blocks
			while (thisBlock != otherBlock) {
				while (thisBlock > otherBlock) {
					otherIndex += 2;
					if (otherIndex > other.lastBlockIndex)
						return res;
					otherBlock = other.words[otherIndex];
				}
				if (thisBlock == otherBlock)
					break;
				while (thisBlock < otherBlock) {
					thisIndex += 2;
					if (thisIndex > lastBlockIndex)
						return res;
					thisBlock = words[thisIndex];
				}
			}
			
			// perform the intersection
			res += Integer.bitCount(words[thisIndex + 1] & other.words[otherIndex + 1]);
			
			// next block
			thisIndex += 2;
			otherIndex += 2;
		}
		return res;
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
	public int last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return words[lastBlockIndex] + 31 - Integer.numberOfLeadingZeros(words[lastBlockIndex + 1]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int first() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return words[0] + Integer.numberOfTrailingZeros(words[1]);
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
		
		final Concise2Set other = convert(o);

		int thisIndex = lastBlockIndex;
		int otherIndex = other.lastBlockIndex;
		int thisBlock;
		int otherBlock;
		while (thisIndex >= 0 && otherIndex >= 0) {
			thisBlock = words[thisIndex];
			otherBlock = other.words[otherIndex];

			if (thisBlock < otherBlock)
				return -1;
			if (thisBlock > otherBlock)
				return 1;
			long w1 = words[thisIndex + 1] & 0x00000000FFFFFFFFL;
			long w2 = other.words[otherIndex + 1] & 0x00000000FFFFFFFFL;
			if (w1 < w2)
				return -1;
			if (w1 > w2)
				return 1;
			
			thisIndex -= 2;
			otherIndex -= 2;
		}
		if (thisIndex > 0)
			return 1;
		if (otherIndex > 0)
			return -1;
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (lastBlockIndex + 2) / Math.ceil((1 + last()) / 32D);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return (double) (lastBlockIndex + 2) / size();
	}
	
	/**
	 * Save the state of the {@link Concise2Set}instance to a stream 
	 */
    private void writeObject(ObjectOutputStream s) throws IOException {
    	if (words != null && lastBlockIndex + 2 < words.length)
    		words = Arrays.copyOf(words, lastBlockIndex + 2);
    	s.defaultWriteObject();
    }

	/**
	 * Reconstruct the {@link Concise2Set} instance from a stream 
	 */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		if (words == null) {
			lastBlockIndex = -2;
			size = 0;
		} else {
			lastBlockIndex = words.length - 2;
			size = -1;
		}
    }

	/*
	 * DEBUGGING METHODS
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
		int n = words[lastBlockIndex] + 31 - Integer.numberOfLeadingZeros(words[lastBlockIndex + 1]);
		int len1 = lastBlockIndex < 2 ? 1 : (int) Math.ceil(Math.log10(lastBlockIndex + 1));
		int len2 = n < 2 ? 1 : (int) Math.ceil(Math.log10(n + 1));
		int[] intBuffer = new int[32];
		for (int i = 0; i <= lastBlockIndex; i += 2) 
			f.format("words[%" + len1 + "d] = %" + len2 + "d + %s %s\n", i, words[i], toBinaryString(words[i + 1]), new IntList(i, intBuffer));
		
		// object attributes
		f.format("size: %s\n", (size == -1 ? "invalid" : Integer.toString(size)));
		f.format("words.length: %d\n", words.length);
		f.format("lastBlockIndex: %d\n", lastBlockIndex);

		// compression
		f.format("bitmap compression: %.2f%%\n", 100D * bitmapCompressionRatio());
		f.format("collection compression: %.2f%%\n", 100D * collectionCompressionRatio());

		return s.toString();
	}
	
	public static void main(String[] args) {
		int maxCardinality = 100;
		Random r = new MersenneTwister(31);
		RandomNumbers rn = new RandomNumbers.Uniform(r.nextInt(maxCardinality), r.nextDouble() * 0.999, r.nextInt(maxCardinality / 10));

		for (int i = 0; i < 1000000; i++) {
			System.out.println("Test " + i);
			Collection<Integer> ints = rn.generate();
			Concise2Set x = new Concise2Set().convert(ints);
			FastSet y = new FastSet().convert(ints);
			
			if (!x.toString().equals(y.toString())) {
				System.out.println("Errore!");
				System.out.println(x);
				System.out.println(y);
				System.out.println(x.debugInfo());
			}
			
			int from = r.nextInt(maxCardinality);
			int to = r.nextInt(maxCardinality);
			if (from > to) {
				int s = from;
				from = to;
				to = s;
			}
			Concise2Set c = x.clone();
			x.fill(from, to);
			y.fill(from, to);
			if (!x.toString().equals(y.toString())) {
				System.out.println("from: " + from + " to: " + to);
				System.out.println(x);
				System.out.println(y);
				System.out.println(c.debugInfo());
				System.out.println(x.debugInfo());
			}
		}
	}
}

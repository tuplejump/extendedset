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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An {@link AbstractExtendedSet} implementation based on {@link BitSet}
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @see ExtendedSet
 * @see AbstractExtendedSet
 * @see ConciseSet
 * @see IndexedSet
 */
public class FastSet extends AbstractExtendedSet<Integer> {
	// plain bitmap representation
	private final BitSet bits;
	
	// set size (only for fast size() call)
	private int size;
	
	/**
	 * Empty integer set
	 */
	public FastSet() {
		bits = new BitSet();
		size = 0;
	}
	
	/**
	 * Creates a bit-string from an existing collection
	 * 
	 * @param c
	 */
	public FastSet(Collection<? extends Integer> c) {
		this();
		addAll(c);
	}

	/**
	 * Cloning constructor
	 * 
	 * @param bits
	 *            bits of the other {@link FastSet} instance. The object will be duplicated.
	 * @param size
	 *            size of the other {@link FastSet} instance
	 */
	private FastSet(BitSet bits, int size) {
		this.bits = (BitSet) bits.clone();
		this.size = size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet clone() {
		return new FastSet(bits, size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return bits.hashCode();
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

		FastSet other = convert((Collection<?>) obj);
		return size == other.size && bits.equals(other.bits);
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
	public int size() {
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(Integer i) {
		if (i < 0)
			throw new IndexOutOfBoundsException();
		if (bits.get(i)) 
			return false;

		bits.set(i);
		size++;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		Statistics.increaseUnionCount();
		if (c == null || c.isEmpty())
			return false;

		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.or(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
		
		return super.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		bits.clear();
		size = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null || isEmpty())
			return false;
		return bits.get((Integer) o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null || c.isEmpty())
			return true;
		if (isEmpty())
			return false;
		if (c.size() > size)
			return false;

		if (c instanceof FastSet) 
			return this.intersectionSize((FastSet) c) == size;
		
		return super.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Integer> other) {
		if (other == null || other.isEmpty() || isEmpty())
			return false;
		
		if (other instanceof FastSet) 
			return this.bits.intersects(((FastSet) other).bits);

		return intersectionSize(other) > 0;
	}

	/**
	 * Iterates over bits
	 */
	private class BitIterator implements ExtendedIterator<Integer> {
		// current bit
		private Integer curr;
		
		// next bit to poll
		private Integer next;

		/**
		 * Constructor
		 */
		public BitIterator() {
			curr = -1;
			next = bits.nextSetBit(0);
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
		public Integer next() {
			if (!hasNext())
				throw new NoSuchElementException();
			curr = next;
			next = bits.nextSetBit(curr + 1);
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(Integer element) {
			if (element <= next)
				return;
			next = bits.nextSetBit(element);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !bits.get(curr))
				throw new IllegalStateException();
			bits.clear(curr);
		}
	}

	/**
	 * Iterates over bits
	 */
	private class ReverseBitIterator implements ExtendedIterator<Integer> {
		// current bit
		private Integer curr;
		
		// next bit to poll
		private Integer next;

		/**
		 * Constructor
		 */
		public ReverseBitIterator() {
			curr = -1;
			next = bits.length() - 1;
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
		public Integer next() {
			if (!hasNext())
				throw new NoSuchElementException();

			// NOTE: it searches for the next set bit. There is no faster way to
			// get it, since there is no method similar to
			// bits.nextSetBit(fromIndex) for scanning bits from MSB to LSB.
			curr = next--;
			while (hasNext() && !bits.get(next)) 
				next--;
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void skipAllBefore(Integer element) {
			if (element >= next)
				return;
			
			// NOTE: it searches for the next set bit. There is no faster way to
			// get it, since there is no method similar to
			// bits.nextSetBit(fromIndex) for scanning bits from MSB to LSB.
			next = element;
			while (hasNext() && !bits.get(next)) 
				next--;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !bits.get(curr))
				throw new IllegalStateException();
			bits.clear(curr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> iterator() {
		return new BitIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> descendingIterator() {
		return new ReverseBitIterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		if (o == null || isEmpty())
			return false;
		
		int i = (Integer) o;
		if (this.bits.get(i)) {
			this.bits.clear(i);
			size--;
			return true;
		} 

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		Statistics.increaseDifferenceCount();
		if (c == null || c.isEmpty() || isEmpty())
			return false;
		
		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.andNot(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
			
		return super.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		Statistics.increaseIntersectionCount();
		if (isEmpty())
			return false;
		if (c == null || c.isEmpty()) {
			clear();
			return true;
		}
		
		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.and(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
		
		return super.retainAll(c);
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
		if (isEmpty())
			throw new NoSuchElementException();
		return bits.nextSetBit(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer last() {
		if (isEmpty())
			throw new NoSuchElementException();
		return bits.length() - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		if (isEmpty())
			return;
		final BitSet old = (BitSet) bits.clone();
		final int lastBit = last();
		bits.clear();
		bits.set(0, lastBit);
		bits.xor(old);
		bits.clear(lastBit);
		size = lastBit - size + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		if (isEmpty())
			return 0;
		return bits.length() - size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet complemented() {
		final FastSet res = this.clone();
		res.complement();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet difference(Collection<? extends Integer> other) {
		Statistics.increaseDifferenceCount();
		if (other == null)
			return clone();

		FastSet cloned = clone();
		if (other instanceof FastSet) {
			cloned.bits.andNot(((FastSet) other).bits);
			cloned.size = cloned.bits.cardinality();
		} else {
			cloned.removeAll(other);
		}
		return cloned;
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
	public FastSet symmetricDifference(Collection<? extends Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other == null)
			return clone();
		
		FastSet cloned = clone();
		if (other instanceof FastSet) {
			cloned.bits.xor(((FastSet) other).bits);
			cloned.size = cloned.bits.cardinality();
		} else {
			cloned.addAll(other);
			cloned.removeAll(this.intersection(other));
		}
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet intersection(Collection<? extends Integer> other) {
		Statistics.increaseIntersectionCount();
		if (other == null)
			return new FastSet();
		
		FastSet cloned = this.clone();
		if (other instanceof FastSet) {
			cloned.bits.and(((FastSet) other).bits);
			cloned.size = cloned.bits.cardinality();
		} else {
			cloned.retainAll(other);
		}
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet union(Collection<? extends Integer> other) {
		Statistics.increaseUnionCount();
		if (other == null)
			return clone();
		
		FastSet cloned = this.clone();
		if (other instanceof FastSet) {
			cloned.bits.or(((FastSet) other).bits);
			cloned.size = cloned.bits.cardinality();
		} else {
			cloned.addAll(other);
		}
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
		// NOTE: bits.size() returns the actual number of allocated words.
		// Instead, we are interested in the logical allocation.
		int s = bits.length();
		if ((s & 0x0000001F) == 0)
			return s / (32D * size);
		return ((s | 0x0000001F) + 1) / (32D * size);
	}

	/**
	 * Similar to {@link #convert(Collection)}, but static
	 * 
	 * @param c
	 *            collection to convert
	 * @return the generated {@link FastSet} instance
	 */
	@SuppressWarnings("unchecked")
	public static FastSet asFastSet(Collection<?> c) {
		if (c == null)
			return new FastSet();
		
		// useless to convert...
		if (c instanceof FastSet)
			return (FastSet) c;
		if (c instanceof AbstractExtendedSet.UnmodifiableExtendedSet) {
			ExtendedSet<?> x = ((AbstractExtendedSet.UnmodifiableExtendedSet) c).container();
			if (x instanceof FastSet)
				return (FastSet) x;
		}
		if (c instanceof AbstractExtendedSet.ExtendedSubSet) {
			ExtendedSet<?> x = ((AbstractExtendedSet.ExtendedSubSet) c).container();
			if (x instanceof FastSet)
				return (FastSet) ((AbstractExtendedSet.ExtendedSubSet) c).convert(c);
		}

		// try to convert the collection
		FastSet res = new FastSet();
		res.addAll((Collection<? extends Integer>) c);
		return res;
	}

	/**
	 * Similar to {@link #convert(Object...)}, but static
	 * 
	 * @param e
	 *            array to convert
	 * @return the generated {@link FastSet} instance
	 */
	public static FastSet asFastSet(Object... e) {
		if (e == null)
			return new FastSet();
		if (e.length == 1) {
			FastSet res = new FastSet();
			res.add((Integer) e[0]);
			return res;
		} 
		
		return asFastSet(Arrays.asList(e));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet convert(Collection<?> c) {
		return asFastSet(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet convert(Object... e) {
		return asFastSet(e);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends FastSet> powerSet() {
		return (List<? extends FastSet>) super.powerSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends FastSet> powerSet(int min, int max) {
		return (List<? extends FastSet>) super.powerSet(min, max);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(Integer from, Integer to) {
		bits.set(from, to + 1);
		size = bits.cardinality();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(Integer from, Integer to) {
		bits.clear(from, to + 1);
		size = bits.cardinality();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(Integer e) {
		bits.flip(e);
		if (bits.get(e))
			size++;
		else
			size--;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		return String.format("elements = %s\nsize = %d\nbitmap compression: %.2f%%\ncollection compression: %.2f%%\n", 
				bits.toString(), size, 100D * bitmapCompressionRatio(), 100D * collectionCompressionRatio());
	}
}

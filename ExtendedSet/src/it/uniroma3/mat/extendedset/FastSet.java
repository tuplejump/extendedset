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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An {@link ExtendedSet} implementation based on {@link BitSet}
 * 
 * @author Alessandro Colantonio
 * @version $Id: FastSet.java 17 2010-01-20 00:52:01Z cocciasik $
 * 
 * @see ExtendedSet
 * @see ConciseSet
 * @see IndexedSet
 */
public class FastSet extends ExtendedSet<Integer> {
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
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FastSet other = (FastSet) obj;
		if (size != other.size)
			return false;
		if (bits == null) {
			if (other.bits != null)
				return false;
		} else if (!bits.equals(other.bits))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return bits.isEmpty();
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
	private class BitIterator implements Iterator<Integer> {
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
	public Iterator<Integer> iterator() {
		return new BitIterator();
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
		if (c == null || c.isEmpty() || isEmpty())
			return false;
		
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
	public FastSet getComplement() {
		final FastSet res = this.clone();
		res.complement();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getDifference(Collection<? extends Integer> other) {
		Statistics.increaseDifferenceCount();
		FastSet cloned = this.clone();
		if (other instanceof FastSet)
			cloned.bits.andNot(((FastSet) other).bits);
		else
			cloned.removeAll(other);
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet emptySet() {
		return new FastSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getSymmetricDifference(Collection<? extends Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		FastSet cloned = this.clone();
		if (other instanceof FastSet) {
			cloned.bits.xor(((FastSet) other).bits);
		} else {
			cloned.addAll(other);
			cloned.removeAll(this.getIntersection(other));
		}
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getIntersection(Collection<? extends Integer> other) {
		Statistics.increaseIntersectionCount();
		FastSet cloned = this.clone();
		if (other instanceof FastSet)
			cloned.bits.and(((FastSet) other).bits);
		else
			cloned.retainAll(other);
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getUnion(Collection<? extends Integer> other) {
		Statistics.increaseUnionCount();
		FastSet cloned = this.clone();
		if (other instanceof FastSet)
			cloned.bits.or(((FastSet) other).bits);
		else
			cloned.addAll(other);
		cloned.size = cloned.bits.cardinality();
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
		return bits.size() / (32D * size);
	}

	/**
	 * Converts a given {@link Collection} instance to a {@link FastSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link FastSet}
	 *            instance
	 * @return the generated {@link FastSet} instance. <b>NOTE:</b> if
	 *         the parameter is an instance of {@link FastSet}, the
	 *         method returns this instance.
	 * @see #asFastSet(Object[])
	 */
	@SuppressWarnings("unchecked")
	public static FastSet asFastSet(Collection<?> c) {
		// useless to convert...
		if (c instanceof FastSet)
			return (FastSet) c;

		// try to convert the collection
		FastSet res = new FastSet();
		final Collection<? extends Integer> integerSet = (Collection<? extends Integer>) c;
		res.addAll(integerSet);
		return res;
	}

	/**
	 * Converts a given integer array to a {@link FastSet} instance
	 * 
	 * @param e
	 *            integers to put within the new instance of
	 *            {@link FastSet}
	 * @return new instance of {@link FastSet}
	 * @see #asFastSet(Collection)
	 */
	public static FastSet asFastSet(Object... e) {
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
	public String debugInfo() {
		return String.format("size = %d, elements = %s\nbitmap compression: %.2f%%\ncollection compression: %.2f%%\n", 
				size, bits.toString(), 100D * bitmapCompressionRatio(), 100D * collectionCompressionRatio());
	}
}
